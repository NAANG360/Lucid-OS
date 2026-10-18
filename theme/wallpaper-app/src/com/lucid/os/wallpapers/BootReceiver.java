package com.lucid.os.wallpapers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Applies only the default system palette once; never changes the user's wallpaper. */
public final class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "LucidBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Log.i(TAG, "Boot completed; initializing Lucid palette if not already chosen.");
        LucidPalette.applyDefaultIfNeeded(context);
    }
}
