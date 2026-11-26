package com.drgenz;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Offline NHS dm+d (Dictionary of Medicines and Devices) catalogue manager.
 * Queries local SQLite database populated from bundled NHSBSA releases.
 */
public final class UkDmDManager {
    private static final String PREFS = "uk_dmd_catalog";
    private static final String WORK_INITIAL = "uk_dmd_initial_import";

    private UkDmDManager() {}

    public static void schedule(Context context) {
        Context app = context.getApplicationContext();
        boolean prepared = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("bundled_data_prepared", false);
        if (!prepared) {
            OneTimeWorkRequest prepare = new OneTimeWorkRequest.Builder(UkDmDInitialImportWorker.class).build();
            WorkManager.getInstance(app).enqueueUniqueWork(WORK_INITIAL, ExistingWorkPolicy.KEEP, prepare);
        }
    }

    public static String status(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String release = p.getString("release", "Release 11.1.0");
        String date = p.getString("release_date", "2026-11-09");
        return "UK Medicines Catalogue: " + release + " (" + date + ") · bundled offline catalogue";
    }

    public static List<Map<String, String>> search(Context context, String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 3 || limit < 1) return new ArrayList<>();
        schedule(context);
        UkDmDSqlite sql = new UkDmDSqlite(context);
        try {
            if (sql.hasRecords()) return sql.search(q, limit);
        } catch (Throwable ignored) {}
        return new ArrayList<>();
    }

    public static String tableLabel(String table) {
        if ("amp_amps".equals(table) || "amp".equals(table)) return "Actual medicinal product (AMP)";
        if ("ampp_ampps".equals(table) || "ampp".equals(table)) return "Actual medicinal product pack (AMPP)";
        if ("vmp_vmps".equals(table) || "vmp".equals(table)) return "Virtual medicinal product (VMP)";
        if ("vmpp_vmpps".equals(table) || "vmpp".equals(table)) return "Virtual medicinal product pack (VMPP)";
        if ("ingredient".equals(table) || "ing".equals(table)) return "Ingredient";
        if ("vtm".equals(table)) return "Virtual therapeutic moiety (VTM)";
        if ("tf".equals(table)) return "Trade Family";
        if ("tfg".equals(table)) return "Trade Family Group";
        if ("vtm_ing".equals(table)) return "VTM ingredient relationship";
        if ("history".equals(table)) return "Historic clinical code";
        return "Clinical terminology";
    }
}
