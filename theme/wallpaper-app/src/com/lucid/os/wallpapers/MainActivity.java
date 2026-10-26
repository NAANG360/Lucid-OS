package com.lucid.os.wallpapers;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public final class MainActivity extends Activity {
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
        TextView brand = text("lucid", 25, INK, true);
        brand.setLetterSpacing(-0.055f);
        mast.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView tag = text("PERSONALIZATION  /  01", 9, MUTED, true);
        tag.setLetterSpacing(0.08f);
        mast.addView(tag);
        page.addView(mast);

        View rule = new View(this);
        rule.setBackgroundColor(RULE);
        page.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));

        addGap(page, 28);
        page.addView(text("THE LUCID ARCHIVE", 10, MUTED, true));
        TextView title = text("Wallpapers.\nThe originals.", 37, INK, true);
        title.setLetterSpacing(-0.045f);
        title.setLineSpacing(dp(-1), 1f);
        page.addView(title, margins(-1, -2, 0, dp(8), 0, 0));
        page.addView(text("Certified Lucid classics, taken from the project artwork. No generated filler.", 14, MUTED, false),
            margins(-1, -2, 0, dp(8), 0, dp(22)));

        page.addView(classicCard(
            "lucid-wallpaper-dusk.jpg", "CLASSIC  /  01", "Violet Dusk",
            "The signature Lucid landscape. A crescent moon above a cyan-to-violet horizon."));
        page.addView(classicCard(
            "lucid-wallpaper-alpine.jpg", "CLASSIC  /  02", "Blue Alpine",
            "A cool, dramatic mountain scene with the original warm sunset highlights."));

        LinearLayout note = column();
        note.setPadding(dp(17), dp(16), dp(17), dp(16));
        note.setBackground(shape(PANEL, 12));
        note.addView(text("ORIGINAL ARTWORK ONLY", 10, ACID, true));
        note.addView(text("These two source images are the original image-fill assets documented in the LucidOS Figma project. This app does not invent or synthesize wallpapers.", 13, WHITE, false),
            margins(-1, -2, 0, dp(8), 0, 0));
        page.addView(note, margins(-1, -2, 0, dp(2), 0, dp(18)));

        TextView source = text("VIEW THE LUCIDOS FIGMA PROJECT ↗", 10, INK, true);
        source.setLetterSpacing(0.06f);
        source.setPadding(0, dp(8), 0, dp(8));
        source.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.figma.com/design/UPUZotxcIfcIdElWklOHFN/cosmosOS?node-id=553-124")));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't open the Figma project.", Toast.LENGTH_SHORT).show();
            }
        });
        page.addView(source);
        page.addView(text("LUCID OS  /  MADE TO FEEL LIKE YOURS", 9, MUTED, true),
            margins(-1, -2, 0, dp(12), 0, 0));

        setContentView(scroll);
    }

    private View classicCard(String asset, String eyebrow, String title, String description) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(12), dp(12), dp(15));
        card.setBackground(shape(Color.WHITE, 12));
        card.setElevation(dp(2));

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(shape(Color.rgb(222, 219, 207), 8));
        preview.setClipToOutline(true);
        try (InputStream in = getAssets().open(asset)) {
            preview.setImageBitmap(BitmapFactory.decodeStream(in));
            preview.setContentDescription(title + " original Lucid wallpaper");
        } catch (Exception e) {
            preview.setContentDescription(title + " preview unavailable");
        }
        card.addView(preview, new LinearLayout.LayoutParams(-1, dp(310)));

        card.addView(text(eyebrow, 9, MUTED, true), margins(-1, -2, 0, dp(14), 0, 0));
        TextView heading = text(title, 25, INK, true);
        heading.setLetterSpacing(-0.03f);
        card.addView(heading, margins(-1, -2, 0, dp(4), 0, 0));
        card.addView(text(description, 13, MUTED, false), margins(-1, -2, 0, dp(5), 0, 0));

        Button apply = new Button(this);
        apply.setText("Set as wallpaper");
        styleButton(apply, ACID, INK);
        apply.setOnClickListener(v -> applyWallpaper(asset));
        card.addView(apply, margins(-1, dp(50), 0, dp(14), 0, 0));
        card.setLayoutParams(margins(-1, -2, 0, 0, 0, dp(17)));
        return card;
    }

    private void applyWallpaper(String asset) {
        try (InputStream in = getAssets().open(asset)) {
            WallpaperManager.getInstance(this).setStream(in, null, true,
                WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            Toast.makeText(this, "Lucid classic applied.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't apply wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void styleButton(Button b, int background, int foreground) {
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(foreground);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setMinHeight(dp(48));
        b.setBackground(shape(background, 7));
        if (android.os.Build.VERSION.SDK_INT >= 21) b.setStateListAnimator(null);
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
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
