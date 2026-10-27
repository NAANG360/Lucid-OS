package com.lucid.os.wallpapers;

import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(18, 18, 22);
    private static final int SURFACE = Color.rgb(30, 30, 36);
    private static final int SURFACE_HIGH = Color.rgb(43, 43, 51);
    private static final int FG = Color.rgb(245, 243, 250);
    private static final int SECONDARY = Color.rgb(190, 187, 202);
    private static final int ACCENT = Color.rgb(202, 151, 255);
    private static final int OUTLINE = Color.rgb(70, 68, 81);

    private final Wallpaper[] wallpapers = new Wallpaper[] {
        new Wallpaper("Glass Bubbles", "wallpaper-bubbles.png", "01  ·  AQUA"),
        new Wallpaper("Purple Crescent", "wallpaper-purple-crescent.png", "02  ·  LUNAR"),
        new Wallpaper("Mint Clock", "wallpaper-clock-home.png", "03  ·  HOME SCREEN"),
        new Wallpaper("Lucid Crescent", "wallpaper-lucid-crescent.png", "04  ·  LUCID"),
        new Wallpaper("DynaPunch", "wallpaper-dynapunch.png", "05  ·  CONCEPT"),
        new Wallpaper("Colour Study", "wallpaper-brushwork.png", "06  ·  ABSTRACT")
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(10), dp(20), dp(28));
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("lucid", 24, FG, true);
        brand.setLetterSpacing(-0.045f);
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView section = label("PERSONALIZATION", 10, SECONDARY, true);
        section.setLetterSpacing(.08f);
        top.addView(section);
        page.addView(top);

        View divider = new View(this);
        divider.setBackgroundColor(OUTLINE);
        page.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        TextView heading = label("Wallpapers", 30, FG, true);
        heading.setLetterSpacing(-.025f);
        page.addView(heading, params(-1, -2, 0, dp(22), 0, dp(3)));
        page.addView(label("Choose a look for your device", 14, SECONDARY, false),
            params(-1, -2, 0, 0, 0, dp(18)));

        int width = getResources().getDisplayMetrics().widthPixels;
        int usable = Math.round(width / getResources().getDisplayMetrics().density) - 40;
        int gap = dp(10);
        int cardWidth = (usable - gap) / 2;
        LinearLayout row = null;
        for (int i = 0; i < wallpapers.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                page.addView(row, params(-1, -2, 0, 0, 0, dp(10)));
            }
            row.addView(tile(wallpapers[i]), new LinearLayout.LayoutParams(cardWidth, dp(264)));
            if (i % 2 == 0) {
                View spacer = new View(this);
                row.addView(spacer, new LinearLayout.LayoutParams(gap, 1));
            }
        }

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(15), dp(13), dp(15), dp(13));
        footer.setBackground(round(SURFACE, 16));
        footer.addView(label("LUCID WALLPAPER ARCHIVE", 10, ACCENT, true));
        footer.addView(label("Artwork and concepts from your supplied collection.", 12, SECONDARY, false),
            params(-1, -2, 0, dp(5), 0, 0));
        TextView figma = label("Open Figma project  ↗", 13, FG, true);
        figma.setPadding(0, dp(10), 0, 0);
        figma.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS?node-id=553-124")));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open Figma.", Toast.LENGTH_SHORT).show();
            }
        });
        footer.addView(figma);
        page.addView(footer, params(-1, -2, 0, dp(6), 0, 0));
        setContentView(scroll);
    }

    private View tile(Wallpaper wallpaper) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(7), dp(7), dp(7), dp(9));
        card.setBackground(round(SURFACE, 15));
        card.setOnClickListener(v -> showPreview(wallpaper));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(round(SURFACE_HIGH, 10));
        image.setClipToOutline(true);
        Bitmap bitmap = readBitmap(wallpaper.asset);
        if (bitmap != null) image.setImageBitmap(bitmap);
        image.setContentDescription(wallpaper.title);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(190)));

        TextView title = label(wallpaper.title, 14, FG, true);
        title.setMaxLines(1);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(title, params(-1, -2, dp(3), dp(8), dp(3), dp(2)));
        card.addView(label(wallpaper.category, 9, SECONDARY, true),
            params(-1, -2, dp(3), 0, dp(3), 0));
        return card;
    }

    private void showPreview(Wallpaper wallpaper) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(16));
        root.setBackgroundColor(BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(wallpaper.title, 20, FG, true);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView close = label("✕", 22, SECONDARY, false);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        bar.addView(close, new LinearLayout.LayoutParams(dp(44), dp(52)));
        root.addView(bar);

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Bitmap bitmap = readBitmap(wallpaper.asset);
        if (bitmap != null) preview.setImageBitmap(bitmap);
        preview.setContentDescription(wallpaper.title + " full-screen preview");
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));

        Button apply = new Button(this);
        apply.setText("Set wallpaper");
        apply.setAllCaps(false);
        apply.setTextColor(BG);
        apply.setTextSize(15);
        apply.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        apply.setBackground(round(ACCENT, 28));
        apply.setOnClickListener(v -> {
            applyWallpaper(wallpaper);
            dialog.dismiss();
        });
        root.addView(apply, params(-1, dp(52), 0, dp(12), 0, 0));
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(BG));
            window.setLayout(-1, -1);
            window.setStatusBarColor(BG);
            window.setNavigationBarColor(BG);
        }
        dialog.show();
        if (window != null) window.setLayout(-1, -1);
    }

    private Bitmap readBitmap(String asset) {
        try (InputStream in = getAssets().open(asset)) {
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            return null;
        }
    }

    private void applyWallpaper(Wallpaper wallpaper) {
        try (InputStream in = getAssets().open(wallpaper.asset)) {
            WallpaperManager.getInstance(this).setStream(in, null, true,
                WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            Toast.makeText(this, wallpaper.title + " applied.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Wallpaper asset unavailable: " + wallpaper.title, Toast.LENGTH_LONG).show();
        }
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(true);
        return t;
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout.LayoutParams params(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private static final class Wallpaper {
        final String title, asset, category;
        Wallpaper(String title, String asset, String category) {
            this.title = title; this.asset = asset; this.category = category;
        }
    }
}