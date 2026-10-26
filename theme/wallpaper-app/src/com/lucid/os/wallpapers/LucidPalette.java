package com.lucid.os.wallpapers;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

/** Applies a Lucid seed through Android's native dynamic-color settings. */
public final class LucidPalette {
    private static final String TAG = "LucidPalette";
    private static final String PREFS = "lucid_theme";
    private static final String INITIALIZED = "lucid_palette_initialized";
    private static final String THEME_SETTING = "theme_customization_overlay_packages";
    private static final String STYLE = "TONAL_SPOT";
    private static final String DEFAULT_BLUE = "#0866FF";

    private LucidPalette() {}

    public static boolean apply(Context context, String sourceColor) throws Exception {
        JSONObject palette = new JSONObject();
        palette.put("android.theme.customization.system_palette", sourceColor);
        palette.put("android.theme.customization.theme_style", STYLE);
        palette.put("android.theme.customization.color_source", "preset");
        palette.put("android.theme.customization.dynamic_color", "1");

        boolean written = Settings.Secure.putString(
            context.getContentResolver(), THEME_SETTING, palette.toString()
        );
        if (written) {
            preferences(context).edit().putBoolean(INITIALIZED, true).apply();
            Log.i(TAG, "Applied Lucid palette seed " + sourceColor);
        }
        return written;
    }

    public static void applyDefaultIfNeeded(Context context) {
        SharedPreferences preferences = preferences(context);
        if (preferences.getBoolean(INITIALIZED, false)) {
            return;
        }
        try {
            if (apply(context, DEFAULT_BLUE)) {
                Log.i(TAG, "Applied first-boot Lucid Blue palette; wallpaper remains user-controlled.");
            } else {
                Log.w(TAG, "Android did not accept the default Lucid Blue palette.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not apply first-boot Lucid palette.", e);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
