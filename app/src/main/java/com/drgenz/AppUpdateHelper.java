package com.drgenz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.util.Log;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Manages in-app update checks for UK Medical Database using a dual-tier strategy:
 * 1. Google Play In-App Updates API (official Play Core method for Google Play releases).
 * 2. Custom remote JSON version check fallback (for sideloaded builds, Amazon Appstore, or non-GMS devices).
 */
public final class AppUpdateHelper {

    public static final int REQ_CODE_APP_UPDATE = 9302;
    private static final String TAG = "DrGenZ-Update-UK";

    // Remote version config hosted in repository
    public static final String REMOTE_VERSION_URL =
            "https://raw.githubusercontent.com/techbaggg/UK-Medical-Database/main/version.json";

    public static final class VersionConfig {
        public final int latestVersionCode;
        public final int minRequiredVersionCode;
        public final String versionName;
        public final String updateMessage;
        public final String playStoreUrl;
        public final String websiteUrl;

        public VersionConfig(int latestVersionCode, int minRequiredVersionCode,
                             String versionName, String updateMessage,
                             String playStoreUrl, String websiteUrl) {
            this.latestVersionCode = latestVersionCode;
            this.minRequiredVersionCode = minRequiredVersionCode;
            this.versionName = (versionName == null) ? "" : versionName;
            this.updateMessage = (updateMessage == null || updateMessage.trim().isEmpty())
                    ? "A new update is available with updated UK medicine records and performance enhancements."
                    : updateMessage;
            this.playStoreUrl = (playStoreUrl == null || playStoreUrl.trim().isEmpty())
                    ? "https://play.google.com/store/apps/details?id=com.drgenz.ukmeddb"
                    : playStoreUrl;
            this.websiteUrl = (websiteUrl == null || websiteUrl.trim().isEmpty())
                    ? "https://drgenz.info"
                    : websiteUrl;
        }
    }

    private AppUpdateHelper() {}

    /**
     * Checks for updates. First attempts Google Play's In-App Updates API.
     * If unavailable, fails, or running on non-GMS devices, seamlessly falls back to remote JSON check.
     */
    public static void checkForAppUpdate(final Activity activity, final Runnable onCheckCompleted) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        try {
            final AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;

                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                                REQ_CODE_APP_UPDATE
                        );
                        return;
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, "Failed to start Google Play update flow", e);
                    }
                }

                // If no Play update is pending or allowed, run remote fallback check
                checkRemoteVersionFallback(activity, onCheckCompleted);
            }).addOnFailureListener(e -> {
                // Device does not have Google Play or build was not installed from Play Store
                Log.d(TAG, "Play in-app update not available on this device: " + e.getMessage());
                checkRemoteVersionFallback(activity, onCheckCompleted);
            });
        } catch (Throwable t) {
            Log.w(TAG, "Error initializing AppUpdateManager", t);
            checkRemoteVersionFallback(activity, onCheckCompleted);
        }
    }

    /**
     * Resumes an immediate update flow if one was interrupted in the background.
     * Call inside Activity onResume().
     */
    public static void onResume(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            final AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                                REQ_CODE_APP_UPDATE
                        );
                    } catch (IntentSender.SendIntentException ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    /**
     * Cross-store fallback: fetches version.json from GitHub/server asynchronously.
     */
    public static void checkRemoteVersionFallback(final Activity activity, final Runnable onCheckCompleted) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (onCheckCompleted != null) onCheckCompleted.run();
            return;
        }

        new Thread(() -> {
            VersionConfig config = fetchVersionConfig(REMOTE_VERSION_URL);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;

                if (config != null) {
                    int currentVersionCode = BuildConfig.VERSION_CODE;
                    if (shouldForceUpdate(currentVersionCode, config)) {
                        showForceUpdateDialog(activity, config);
                        return;
                    } else if (shouldSuggestUpdate(currentVersionCode, config)) {
                        showOptionalUpdateDialog(activity, config, onCheckCompleted);
                        return;
                    }
                }

                if (onCheckCompleted != null) {
                    onCheckCompleted.run();
                }
            });
        }).start();
    }

    public static boolean shouldForceUpdate(int currentCode, VersionConfig config) {
        return config != null && currentCode < config.minRequiredVersionCode;
    }

    public static boolean shouldSuggestUpdate(int currentCode, VersionConfig config) {
        return config != null && currentCode < config.latestVersionCode && currentCode >= config.minRequiredVersionCode;
    }

    public static VersionConfig parseVersionConfig(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return null;
        try {
            int latest = optInt(jsonStr, "latestVersionCode", 0);
            int minReq = optInt(jsonStr, "minRequiredVersionCode", 0);
            String vName = optString(jsonStr, "versionName", "");
            String msg = optString(jsonStr, "updateMessage", "");
            String playUrl = optString(jsonStr, "playStoreUrl", "");
            String webUrl = optString(jsonStr, "websiteUrl", "");
            if (latest <= 0 && minReq <= 0 && vName.isEmpty()) {
                return null;
            }
            return new VersionConfig(latest, minReq, vName, msg, playUrl, webUrl);
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse version JSON", e);
            return null;
        }
    }

    private static int optInt(String json, String key, int def) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return def;
    }

    private static String optString(String json, String key, String def) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return def;
    }

    private static VersionConfig fetchVersionConfig(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setUseCaches(false);
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                reader.close();
                return parseVersionConfig(sb.toString());
            }
        } catch (Throwable t) {
            Log.d(TAG, "Remote version check network error (offline or unreachable): " + t.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static void showForceUpdateDialog(final Activity activity, final VersionConfig config) {
        new AlertDialog.Builder(activity)
                .setTitle("Update Required")
                .setMessage(config.updateMessage + "\n\nPlease update to version " + config.versionName + " to continue using UK Medical Database.")
                .setPositiveButton("Update Now", (d, w) -> openStoreOrWeb(activity, config))
                .setNegativeButton("Exit", (d, w) -> activity.finish())
                .setCancelable(false)
                .show();
    }

    private static void showOptionalUpdateDialog(final Activity activity, final VersionConfig config, final Runnable onDismiss) {
        new AlertDialog.Builder(activity)
                .setTitle("New Version Available")
                .setMessage(config.updateMessage)
                .setPositiveButton("Update", (d, w) -> openStoreOrWeb(activity, config))
                .setNegativeButton("Later", (d, w) -> {
                    if (onDismiss != null) onDismiss.run();
                })
                .setCancelable(true)
                .setOnCancelListener(d -> {
                    if (onDismiss != null) onDismiss.run();
                })
                .show();
    }

    public static void openStoreOrWeb(Context context, VersionConfig config) {
        if (context == null || config == null) return;
        // Try Google Play store app intent first
        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName()));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(marketIntent);
            return;
        } catch (ActivityNotFoundException ignored) {}

        // Fallback to web Play Store link
        try {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(config.playStoreUrl));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
            return;
        } catch (ActivityNotFoundException ignored) {}

        // Fallback to websiteUrl
        try {
            Intent siteIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(config.websiteUrl));
            siteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(siteIntent);
        } catch (ActivityNotFoundException ignored) {}
    }
}
