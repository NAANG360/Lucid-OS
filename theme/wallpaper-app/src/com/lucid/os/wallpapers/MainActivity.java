package com.lucid.os.wallpaper;

import com.lucid.motion.LucidMotion;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 13, 19);
    private static final int SURFACE = Color.rgb(24, 25, 35);
    private static final int SURFACE_HIGH = Color.rgb(39, 39, 51);
    private static final int FG = Color.rgb(246, 244, 252);
    private static final int SECONDARY = Color.rgb(166, 164, 188);
    private static final int OUTLINE = Color.rgb(54, 53, 70);
    private int accent = Color.rgb(196, 151, 255);

    private static final class WallpaperItem {
        final String title;
        final String asset;
        final String category;
        final String subtitle;
        WallpaperItem(String title, String asset, String category, String subtitle) {
            this.title = title;
            this.asset = asset;
            this.category = category;
            this.subtitle = subtitle;
        }
    }

    private final WallpaperItem[] wallpapers = new WallpaperItem[] {
        new WallpaperItem("Violet Dusk", "lucid-wallpaper-dusk.jpg", "01  ·  SIGNATURE", "Moonlit valley with violet skies and cyan horizon"),
        new WallpaperItem("Glass Bubbles", "wallpaper-bubbles.webp", "02  ·  AQUA", "The original translucent glass-sphere wallpaper"),
        new WallpaperItem("Purple Crescent", "wallpaper-purple-crescent.webp", "03  ·  LUNAR", "Soft lavender gradient and crescent silhouette"),
        new WallpaperItem("Lucid Crescent", "wallpaper-lucid-crescent.webp", "04  ·  LUCID", "Deep electric violet atmosphere crafted for AMOLED & LCD"),
        new WallpaperItem("Blue Alpine", "lucid-wallpaper-alpine.jpg", "05  ·  ALPINE", "Dramatic blue alpine peaks with warm dusk highlights"),
        new WallpaperItem("Colour Study", "wallpaper-brushwork.webp", "06  ·  ABSTRACT", "Original colour-study artwork")
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        loadAppearance();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        render();
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

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(14), dp(20), dp(30));
        scroll.addView(page);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("lucid", 26, FG, true);
        brand.setLetterSpacing(-0.04f);
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView badge = label("WALLPAPERS", 10, accent, true);
        badge.setLetterSpacing(0.10f);
        badge.setPadding(dp(11), dp(6), dp(11), dp(6));
        badge.setBackground(round(SURFACE_HIGH, 20));
        top.addView(badge);
        page.addView(top);

        View divider = new View(this);
        divider.setBackgroundColor(OUTLINE);
        page.addView(divider, params(-1, dp(1), 0, dp(6), 0, dp(18)));

        TextView heading = label("Atmosphere for your screen.", 28, FG, true);
        heading.setLetterSpacing(-0.02f);
        page.addView(heading, params(-1, -2, 0, 0, 0, dp(4)));
        page.addView(label("Supplied Lucid artwork and Figma classics. Tap any card to preview or apply to Home and Lock screens.", 13, SECONDARY, false),
            params(-1, -2, 0, 0, 0, dp(18)));

        page.addView(paletteCard(), params(-1, -2, 0, 0, 0, dp(18)));

        int widthPx = getResources().getDisplayMetrics().widthPixels;
        int horizontalPad = dp(40);
        int gap = dp(12);
        int cardWidth = Math.max(dp(130), (widthPx - horizontalPad - gap) / 2);

        LinearLayout row = null;
        for (int i = 0; i < wallpapers.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                page.addView(row, params(-1, -2, 0, 0, 0, dp(12)));
            }
            row.addView(tile(wallpapers[i]), new LinearLayout.LayoutParams(cardWidth, dp(268)));
            if (i % 2 == 0) {
                View spacer = new View(this);
                row.addView(spacer, new LinearLayout.LayoutParams(gap, 1));
            }
        }

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(16), dp(14), dp(16), dp(14));
        footer.setBackground(round(SURFACE, 18));
        footer.addView(label("LUCID WALLPAPER ARCHIVE", 10, accent, true));
        footer.addView(label("All six selected wallpapers are bundled natively in the APK and mirrored under /system/etc/lucid/wallpapers.", 12, SECONDARY, false),
            params(-1, -2, 0, dp(5), 0, 0));
        page.addView(footer, params(-1, -2, 0, dp(6), 0, 0));

        setContentView(scroll);
        LucidMotion.apply(this);
    }

    private View paletteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(15));
        card.setBackground(round(SURFACE, 20));

        card.addView(label("SYSTEM ACCENT PALETTE", 10, accent, true));
        card.addView(label("Syncs Monet tonal seed with Android system surfaces.", 12, SECONDARY, false),
            params(-1, -2, 0, dp(4), 0, dp(12)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(paletteButton("Violet Dusk", "#8B77FF", Color.rgb(139, 119, 255)));
        row.addView(paletteButton("Blue Pulse", "#0866FF", Color.rgb(8, 102, 255)));
        row.addView(paletteButton("Cyan Horizon", "#00AFC8", Color.rgb(0, 175, 200)));
        card.addView(row);
        return card;
    }

    private View paletteButton(final String name, final String hex, final int color) {
        TextView b = label(name, 12, FG, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), dp(10), dp(8), dp(10));
        b.setBackground(round(color, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> applySystemPalette(name, hex, color));
        return b;
    }

    private void applySystemPalette(String labelText, String hexColor, int rgb) {
        accent = rgb;
        try {
            JSONObject json = new JSONObject();
            json.put("android.theme.customization.system_palette", hexColor);
            json.put("android.theme.customization.accent_color", hexColor);
            json.put("android.theme.customization.color_source", "preset");
            json.put("android.theme.customization.theme_style", "TONAL_SPOT");
            json.put("_applied_timestamp", System.currentTimeMillis());
            Settings.Secure.putString(getContentResolver(), "theme_customization_overlay_packages", json.toString());
            Toast.makeText(this, labelText + " palette applied to system.", Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {
            Toast.makeText(this, labelText + " accent selected for Lucid surfaces.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Palette updated locally.", Toast.LENGTH_SHORT).show();
        }
        render();
    }

    private View tile(final WallpaperItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(10));
        card.setBackground(round(SURFACE, 18));
        card.setOnClickListener(v -> showPreview(item));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(round(SURFACE_HIGH, 13));
        image.setClipToOutline(true);
        Bitmap bmp = decodeSampledBitmap(item.asset, 540, 960);
        if (bmp != null) {
            image.setImageBitmap(bmp);
        }
        image.setContentDescription(item.title);
        card.addView(image, new LinearLayout.LayoutParams(-1, dp(192)));

        TextView title = label(item.title, 14, FG, true);
        title.setMaxLines(1);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title, params(-1, -2, dp(4), dp(9), dp(4), dp(2)));

        TextView cat = label(item.category, 9, accent, true);
        cat.setMaxLines(1);
        card.addView(cat, params(-1, -2, dp(4), 0, dp(4), 0));
        return card;
    }

    private void showPreview(final WallpaperItem item) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(20));
        root.setBackgroundColor(BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.addView(label(item.category, 10, accent, true));
        titleCol.addView(label(item.title, 20, FG, true));
        bar.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView close = label("✕", 20, SECONDARY, true);
        close.setGravity(Gravity.CENTER);
        close.setBackground(round(SURFACE_HIGH, 22));
        close.setOnClickListener(v -> dialog.dismiss());
        bar.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        root.addView(bar, params(-1, -2, 0, 0, 0, dp(10)));

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(round(SURFACE, 20));
        preview.setClipToOutline(true);
        Bitmap bmp = decodeSampledBitmap(item.asset, 1080, 2160);
        if (bmp != null) {
            preview.setImageBitmap(bmp);
        }
        preview.setContentDescription(item.title + " preview");
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1f));

        root.addView(label(item.subtitle, 12, SECONDARY, false), params(-1, -2, 0, dp(12), 0, dp(12)));

        Button applyBtn = new Button(this);
        applyBtn.setText("Apply wallpaper");
        applyBtn.setAllCaps(false);
        applyBtn.setTextColor(BG);
        applyBtn.setTextSize(15);
        applyBtn.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        applyBtn.setBackground(round(accent, 24));
        applyBtn.setOnClickListener(v -> {
            String[] targets = {"Home & Lock screens", "Home screen only", "Lock screen only"};
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Apply " + item.title)
                .setItems(targets, (d, which) -> {
                    int flags = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                    if (which == 1) flags = WallpaperManager.FLAG_SYSTEM;
                    else if (which == 2) flags = WallpaperManager.FLAG_LOCK;
                    applyWallpaper(item, flags);
                    dialog.dismiss();
                })
                .show();
        });
        root.addView(applyBtn, new LinearLayout.LayoutParams(-1, dp(52)));

        dialog.setContentView(root);
        LucidMotion.apply(this);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(BG));
            window.setLayout(-1, -1);
            window.setStatusBarColor(BG);
            window.setNavigationBarColor(BG);
        }
        dialog.show();
    }

    private InputStream openWallpaperStream(String assetName) throws Exception {
        try {
            return getAssets().open(assetName);
        } catch (Exception first) {
            File sysFile = new File("/system/etc/lucid/wallpapers", assetName);
            if (sysFile.isFile()) {
                return new FileInputStream(sysFile);
            }
            throw first;
        }
    }

    private Bitmap decodeSampledBitmap(String assetName, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream in = openWallpaperStream(assetName)) {
                BitmapFactory.decodeStream(in, null, opts);
            }
            opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in2 = openWallpaperStream(assetName)) {
                return BitmapFactory.decodeStream(in2, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        while ((width / inSampleSize) > 2560 || (height / inSampleSize) > 2560) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }

    private void applyWallpaper(WallpaperItem item, int flags) {
        try {
            Bitmap bmp = decodeSampledBitmap(item.asset, 1440, 2560);
            if (bmp == null) {
                throw new IllegalStateException("Could not decode " + item.asset);
            }
            WallpaperManager wm = WallpaperManager.getInstance(this);
            wm.setBitmap(bmp, null, true, flags);
            Toast.makeText(this, item.title + " applied.", Toast.LENGTH_SHORT).show();
            if (Intent.ACTION_SET_WALLPAPER.equals(getIntent() != null ? getIntent().getAction() : null)) {
                setResult(RESULT_OK);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not apply wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private LinearLayout.LayoutParams params(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
