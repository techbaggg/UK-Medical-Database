package com.drgenz;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** First-run offline import of the included NHSBSA item 24 and item 25 release archives. */
public final class UkDmDInitialImportWorker extends Worker {
    public UkDmDInitialImportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        Context c = getApplicationContext();
        File dir = new File(c.getFilesDir(), "nhs_dmd_staging");
        if (!dir.exists() && !dir.mkdirs()) return Result.retry();
        File main = new File(dir, "bundled-item24.zip");
        File supplementary = new File(dir, "bundled-item25.zip");
        try {
            UkDmDSqlite db = new UkDmDSqlite(c);
            if (db.hasRecords()) {
                c.getSharedPreferences("uk_dmd_catalog", Context.MODE_PRIVATE).edit().putBoolean("bundled_data_prepared", true).apply();
                return Result.success();
            }
            copyAsset(c, "nhs_dmd_item24_11.1.0_20261109000001.zip", main);
            copyAsset(c, "nhs_dmd_item25_11.1.0_20261109000001.zip", supplementary);
            db.replaceFromArchives(main, supplementary);
            SharedPreferences p = c.getSharedPreferences("uk_dmd_catalog", Context.MODE_PRIVATE);
            p.edit()
                    .putString("item24_release_id", "nhsbsa_dmd_11.1.0_20261109000001.zip")
                    .putString("item24_release", "11.1.0")
                    .putString("item24_release_date", "2026-11-09")
                    .putString("item25_release_id", "nhsbsa_dmdbonus_11.1.0_20261109000001.zip")
                    .putString("item25_release", "11.1.0")
                    .putString("item25_release_date", "2026-11-09")
                    .putString("release", "11.1.0 + supplementary 11.1.0")
                    .putString("release_id", "nhsbsa_dmd_11.1.0_20261109000001.zip|nhsbsa_dmdbonus_11.1.0_20261109000001.zip")
                    .putString("release_date", "2026-11-09")
                    .putString("last_update_status", "Bundled NHS dm+d item 24 and item 25 imported")
                    .putString("updated_at", now())
                    .putBoolean("bundled_data_prepared", true).apply();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        } finally {
            //noinspection ResultOfMethodCallIgnored
            main.delete();
            //noinspection ResultOfMethodCallIgnored
            supplementary.delete();
        }
    }

    private void copyAsset(Context c, String asset, File target) throws Exception {
        try (InputStream in = c.getAssets().open(asset); FileOutputStream out = new FileOutputStream(target)) {
            byte[] b = new byte[32768]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            out.getFD().sync();
        }
    }
    private static String now() { return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()); }
}
