package com.lucid.os.about;

import com.lucid.motion.LucidMotion;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 13, 19);
    private static final int CARD = Color.rgb(24, 25, 35);
    private static final int CARD_HIGH = Color.rgb(34, 35, 48);
    private static final int FG = Color.rgb(246, 244, 252);
    private static final int MUTED = Color.rgb(166, 164, 188);
    private static final int RULE = Color.rgb(44, 45, 62);
    private int accent = Color.rgb(196, 151, 255);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        loadAppearance();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = column();
        page.setPadding(dp(20), dp(16), dp(20), dp(30));
        scroll.addView(page);

        LinearLayout mast = new LinearLayout(this);
        mast.setGravity(Gravity.CENTER_VERTICAL);
        TextView wordmark = text("lucid", 26, FG, true);
        wordmark.setLetterSpacing(-0.045f);
        mast.addView(wordmark, new LinearLayout.LayoutParams(0, dp(44), 1f));
        TextView badge = text("ABOUT DEVICE", 10, accent, true);
        badge.setLetterSpacing(0.10f);
        badge.setPadding(dp(11), dp(6), dp(11), dp(6));
        badge.setBackground(shape(CARD_HIGH, 20));
        mast.addView(badge);
        page.addView(mast);

        View rule = new View(this);
        rule.setBackgroundColor(RULE);
        page.addView(rule, margins(-1, dp(1), 0, dp(6), 0, dp(20)));

        page.addView(text("DEVICE IDENTITY", 10, accent, true));
        TextView title = text("Yours, by\ndesign.", 36, FG, true);
        title.setLetterSpacing(-0.035f);
        page.addView(title, margins(-1, -2, 0, dp(6), 0, dp(6)));
        page.addView(text("A considered Android experience for Redmi Note 7 / 7S (lavender). Dark surfaces, restrained violet accents, and original Lucid artwork.", 13, MUTED, false),
            margins(-1, -2, 0, 0, 0, dp(18)));

        ImageView hero = new ImageView(this);
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.setBackground(shape(CARD_HIGH, 18));
        hero.setClipToOutline(true);
        Bitmap heroBmp = loadHeroBitmap();
        if (heroBmp != null) {
            hero.setImageBitmap(heroBmp);
            hero.setContentDescription("Original Violet Dusk Lucid wallpaper");
        }
        page.addView(hero, margins(-1, dp(210), 0, 0, 0, dp(16)));

        LinearLayout identity = card(CARD);
        identity.addView(text("LUCID OS  •  LAVENDER", 10, accent, true));
        TextView statement = text("Built around the details that matter.", 22, FG, true);
        identity.addView(statement, margins(-1, -2, 0, dp(8), 0, dp(6)));
        identity.addView(text("Original Lucid artwork, unified appearance tokens across Launcher, Control Centre, Settings, and Desktop, with Android fundamentals intact.", 13, MUTED, false));
        page.addView(identity, margins(-1, -2, 0, 0, 0, dp(14)));

        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batLevel = bm == null ? -1 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String lucidVer = prop("ro.lucid.version", "0.4-dev (Build 4 Integrated)");

        LinearLayout specs = card(CARD);
        specs.addView(text("HARDWARE & SYSTEM SPECIFICATIONS", 10, accent, true), margins(-1, -2, 0, 0, 0, dp(8)));
        row(specs, "Lucid OS Version", lucidVer);
        row(specs, "Device Model", Build.MODEL + " (" + Build.DEVICE + ")");
        row(specs, "Platform / SoC", "Qualcomm Snapdragon 660 (SDM660)");
        row(specs, "Build Display ID", Build.DISPLAY);
        row(specs, "Kernel Version", System.getProperty("os.version", "4.19-lavender"));
        row(specs, "Battery Level", batLevel >= 0 ? batLevel + "%" : "Reported by hardware");
        row(specs, "Desktop Root Gate", "1".equals(prop("persist.lucid.desktop_root", "0")) ? "Enabled (persist.lucid.desktop_root=1)" : "Disabled (default safe mode)");
        row(specs, "Build Fingerprint", Build.FINGERPRINT);
        page.addView(specs, margins(-1, -2, 0, 0, 0, dp(14)));

        LinearLayout actions = card(CARD);
        actions.addView(text("SYSTEM SHORTCUTS", 10, accent, true), margins(-1, -2, 0, 0, 0, dp(8)));
        addActionButton(actions, "Open Lucid Settings", () -> launchPackage("com.lucid.os.settings", Settings.ACTION_SETTINGS));
        addActionButton(actions, "Open Lucid Updater", () -> launchPackage("com.lucid.os.updater", Settings.ACTION_DEVICE_INFO_SETTINGS));
        addActionButton(actions, "Stock Android Device Info", () -> {
            try { startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)); } catch (Exception ignored) {}
        });
        page.addView(actions);

        setContentView(scroll);
        LucidMotion.apply(this);
    }

    private void loadAppearance() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                Uri.parse("content://com.lucid.os.settings.appearance/profile"),
                new String[]{"accent"}, null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                accent = cursor.getInt(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private Bitmap loadHeroBitmap() {
        try (InputStream in = getAssets().open("lucid-wallpaper-dusk.jpg")) {
            return BitmapFactory.decodeStream(in);
        } catch (Exception ignored) {}
        File f = new File("/system/etc/lucid/wallpapers/lucid-wallpaper-dusk.jpg");
        if (f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                return BitmapFactory.decodeStream(in);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void addActionButton(LinearLayout parent, String labelText, Runnable r) {
        TextView btn = text(labelText + "  ↗", 13, FG, true);
        btn.setPadding(dp(14), dp(12), dp(14), dp(12));
        btn.setBackground(shape(CARD_HIGH, 14));
        btn.setOnClickListener(v -> r.run());
        parent.addView(btn, margins(-1, -2, 0, dp(6), 0, 0));
    }

    private void launchPackage(String pkg, String fallbackAction) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                startActivity(i);
                return;
            }
        } catch (Exception ignored) {}
        try {
            startActivity(new Intent(fallbackAction));
        } catch (Exception ignored) {}
    }

    private LinearLayout card(int color) {
        LinearLayout v = column();
        v.setPadding(dp(16), dp(16), dp(16), dp(16));
        v.setBackground(shape(color, 18));
        return v;
    }

    private void row(LinearLayout parent, String key, String value) {
        LinearLayout line = column();
        line.setPadding(0, dp(8), 0, dp(8));
        line.addView(text(key.toUpperCase(Locale.ROOT), 9, MUTED, true));
        String safe = value == null || value.trim().isEmpty() ? "Unavailable" : value;
        TextView val = text(safe, 14, FG, true);
        val.setTextIsSelectable(true);
        line.addView(val, margins(-1, -2, 0, dp(3), 0, 0));
        parent.addView(line);
        View divider = new View(this);
        divider.setBackgroundColor(RULE);
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private String prop(String key, String fallback) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Object v = c.getMethod("get", String.class, String.class).invoke(null, key, fallback);
            String s = String.valueOf(v);
            return s.isEmpty() ? fallback : s;
        } catch (Exception e) {
            return fallback;
        }
    }
}
