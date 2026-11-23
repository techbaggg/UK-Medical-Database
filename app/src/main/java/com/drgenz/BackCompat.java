package com.drgenz;

import android.app.Activity;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Back-button handling for Android 13+ (API 33). Apps that target Android 16 (API 36), which Google Play now
 * requires, no longer receive Activity.onBackPressed(); they must use OnBackInvokedCallback instead.
 * Kept in its own class so MainActivity never touches API 33 types on older phones.
 */
final class BackCompat {
    private BackCompat() { }

    static Object register(Activity a, Runnable onBack) {
        OnBackInvokedCallback cb = onBack::run;
        a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
        return cb;
    }

    static void unregister(Activity a, Object cb) {
        a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) cb);
    }
}
