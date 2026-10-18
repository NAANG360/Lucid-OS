package com.lucid.os.wallpapers;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.InputStream;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        SharedPreferences preferences = context.getSharedPreferences("lucid_wallpaper", Context.MODE_PRIVATE);
        if (preferences.getBoolean("default_applied", false)) return;

        try (InputStream in = context.getAssets().open("lucid-wallpaper-dusk.jpg")) {
            WallpaperManager.getInstance(context).setStream(
                in, null, true,
                WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK
            );
            preferences.edit().putBoolean("default_applied", true).apply();
        } catch (Exception ignored) {
            // The picker remains available so the user can apply the wallpaper manually.
        }
    }
}
