package com.lucid.os.about;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AboutActivity extends Activity {
    private static final int BG = Color.rgb(13, 8, 24);
    private static final int PANEL = Color.rgb(31, 20, 49);
    private static final int PURPLE = Color.rgb(168, 85, 247);
    private static final int LILAC = Color.rgb(221, 190, 255);
    private static final int MUTED = Color.rgb(180, 158, 205);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(32));
        scroll.addView(page);

        TextView eyebrow = label("L U C I D   /   DEVICE IDENTITY", 11, LILAC, true);
        page.addView(eyebrow);
        TextView title = label("About this device", 31, Color.WHITE, true);
        LinearLayout.LayoutParams tp = params(-1, -2, 0, 8, 0, 18);
        page.addView(title, tp);

        ImageView hero = new ImageView(this);
        hero.setImageResource(getResources().getIdentifier("lucid_wallpaper", "drawable", getPackageName()));
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.setBackground(shape(PANEL, 28));
        hero.setClipToOutline(true);
        page.addView(hero, params(-1, dp(220), 0, 0, 0, 18));

        LinearLayout brand = card();
        TextView brandName = label("LUCID OS", 26, Color.WHITE, true);
        brand.addView(brandName);
        brand.addView(label("Violet, by design.", 15, LILAC, false), params(-1,-2,0,4,0,0));
        brand.addView(label("A calmer interface. A more personal device.", 13, MUTED, false), params(-1,-2,0,8,0,0));
        page.addView(brand, params(-1,-2,0,0,0,14));

        LinearLayout details = card();
        details.addView(label("DEVICE DETAILS", 11, LILAC, true), params(-1,-2,0,0,0,12));
        row(details, "Device", Build.MODEL);
        row(details, "Codename", Build.DEVICE);
        row(details, "Android version", Build.VERSION.RELEASE + "  (API " + Build.VERSION.SDK_INT + ")");
        row(details, "LucidOS build", prop("ro.lucid.version", prop("ro.build.display.id", "LucidOS")));
        row(details, "Build fingerprint", Build.FINGERPRINT);
        page.addView(details, params(-1,-2,0,0,0,14));

        LinearLayout identity = card();
        identity.addView(label("MADE TO FEEL LIKE YOURS", 11, LILAC, true));
        identity.addView(label("Purple is the primary palette. The Lucid wallpaper is part of the identity—not an afterthought.", 14, Color.WHITE, false), params(-1,-2,0,8,0,0));
        page.addView(identity, params(-1,-2,0,0,0,0));

        TextView footer = label("LUCID OS  •  LAVENDER EDITION", 10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, params(-1,-2,0,22,0,0));
        setContentView(scroll);
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(18), dp(18), dp(18), dp(18));
        v.setBackground(shape(PANEL, 24));
        return v;
    }
    private void row(LinearLayout parent, String name, String value) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.VERTICAL);
        line.setPadding(0, dp(8), 0, dp(8));
        TextView k = label(name, 12, MUTED, false);
        TextView v = label(value == null || value.isEmpty() ? "Unavailable" : value, 15, Color.WHITE, true);
        line.addView(k);
        line.addView(v, params(-1,-2,0,3,0,0));
        parent.addView(line);
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(64, 43, 86));
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(true);
        return t;
    }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(67, 43, 91));
        return d;
    }
    private LinearLayout.LayoutParams params(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h);
        p.setMargins(l, t, r, b); return p;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private String prop(String key, String fallback) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Object v = c.getMethod("get", String.class, String.class).invoke(null, key, fallback);
            return String.valueOf(v);
        } catch (Exception e) { return fallback; }
    }
}