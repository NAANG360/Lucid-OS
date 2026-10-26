package com.lucid.os.about;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class AboutActivity extends Activity {
    private static final int PAPER = Color.rgb(244, 240, 232);
    private static final int INK = Color.rgb(32, 32, 29);
    private static final int MUTED = Color.rgb(119, 115, 106);
    private static final int ACID = Color.rgb(199, 243, 107);
    private static final int PANEL = Color.rgb(37, 38, 34);
    private static final int WHITE = Color.rgb(245, 243, 237);
    private static final int RULE = Color.rgb(217, 212, 201);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);
        LinearLayout page = column();
        page.setPadding(dp(23), dp(16), dp(23), dp(30));
        scroll.addView(page);

        LinearLayout mast = new LinearLayout(this);
        mast.setGravity(Gravity.CENTER_VERTICAL);
        TextView wordmark = text("lucid", 25, INK, true);
        wordmark.setLetterSpacing(-0.055f);
        mast.addView(wordmark, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView label = text("OS  /  ABOUT", 9, MUTED, true);
        label.setLetterSpacing(0.09f);
        mast.addView(label);
        page.addView(mast);

        View rule = new View(this);
        rule.setBackgroundColor(RULE);
        page.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));

        addGap(page, 28);
        page.addView(text("DEVICE IDENTITY", 10, MUTED, true));
        TextView title = text("Yours, by\ndesign.", 39, INK, true);
        title.setLetterSpacing(-0.045f);
        title.setLineSpacing(dp(-1), 1f);
        page.addView(title, margins(-1, -2, 0, dp(8), 0, 0));
        page.addView(text("A considered Android experience for lavender. Clear, personal, and unmistakably Lucid.", 14, MUTED, false),
            margins(-1, -2, 0, dp(8), 0, dp(21)));

        int wallpaperId = getResources().getIdentifier("lucid_wallpaper", "drawable", getPackageName());
        ImageView hero = new ImageView(this);
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.setBackground(shape(PANEL, 12));
        hero.setClipToOutline(true);
        if (wallpaperId != 0) {
            hero.setImageResource(wallpaperId);
            hero.setContentDescription("Original Violet Dusk Lucid wallpaper from the project artwork");
        } else {
            hero.setContentDescription("Lucid original wallpaper unavailable");
        }
        page.addView(hero, margins(-1, dp(245), 0, 0, 0, dp(18)));

        LinearLayout identity = card(PANEL);
        identity.addView(text("LUCID OS  /  LAVENDER", 10, ACID, true));
        TextView statement = text("Built around the details that matter.", 25, WHITE, true);
        statement.setLetterSpacing(-0.035f);
        identity.addView(statement, margins(-1, -2, 0, dp(10), 0, 0));
        identity.addView(text("Original Lucid artwork. A restrained palette. Android fundamentals kept intact. No filler, no borrowed brand identity.", 14, WHITE, false),
            margins(-1, -2, 0, dp(7), 0, 0));
        page.addView(identity, margins(-1, -2, 0, 0, 0, dp(15)));

        LinearLayout details = card(Color.WHITE);
        details.addView(text("THIS DEVICE", 10, MUTED, true), margins(-1, -2, 0, 0, 0, dp(8)));
        row(details, "Model", Build.MODEL);
        row(details, "Device codename", Build.DEVICE);
        row(details, "Android version", Build.VERSION.RELEASE + "  /  API " + Build.VERSION.SDK_INT);
        row(details, "LucidOS version", prop("ro.lucid.version", prop("ro.build.display.id", "Not reported")));
        row(details, "Build fingerprint", Build.FINGERPRINT);
        page.addView(details, margins(-1, -2, 0, 0, 0, dp(15)));

        LinearLayout source = card(Color.WHITE);
        source.addView(text("THE ORIGINALS", 10, MUTED, true));
        source.addView(text("Lucid's wallpaper classics come from the project's Figma artwork—not generated wallpaper packs.", 14, INK, false),
            margins(-1, -2, 0, dp(7), 0, 0));
        TextView link = text("OPEN LUCIDOS FIGMA ↗", 10, INK, true);
        link.setLetterSpacing(0.06f);
        link.setPadding(0, dp(9), 0, dp(2));
        link.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS?node-id=553-124")));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open Figma.", Toast.LENGTH_SHORT).show();
            }
        });
        source.addView(link);
        page.addView(source);

        TextView footer = text("LUCID OS  /  MADE TO FEEL LIKE YOURS", 9, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, margins(-1, -2, 0, dp(22), 0, 0));
        setContentView(scroll);
    }

    private LinearLayout card(int color) {
        LinearLayout v = column();
        v.setPadding(dp(17), dp(17), dp(17), dp(17));
        v.setBackground(shape(color, 12));
        return v;
    }

    private void row(LinearLayout parent, String key, String value) {
        LinearLayout line = column();
        line.setPadding(0, dp(8), 0, dp(8));
        line.addView(text(key.toUpperCase(java.util.Locale.ROOT), 9, MUTED, true));
        String safe = value == null || value.trim().isEmpty() ? "Unavailable" : value;
        TextView val = text(safe, 14, INK, true);
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
        t.setIncludeFontPadding(true);
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
    private void addGap(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private String prop(String key, String fallback) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Object v = c.getMethod("get", String.class, String.class).invoke(null, key, fallback);
            return String.valueOf(v);
        } catch (Exception e) { return fallback; }
    }
}
