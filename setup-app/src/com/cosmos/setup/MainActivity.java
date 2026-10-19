package com.cosmos.setup;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(242, 240, 248);
    private static final int INK = Color.rgb(34, 34, 55);
    private static final int MUTED = Color.rgb(102, 101, 128);
    private static final int BLUE = Color.rgb(8, 102, 255);
    private String selectedPalette = "#0866FF";
    private String selectedWallpaper = null;
    private TextView stepLabel;
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        showWelcome();
    }

    private void base(String step) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(28));
        scroll.addView(root);
        TextView brand = text("L U C I D   /   ANDROID", 11, BLUE, true);
        root.addView(brand);
        stepLabel = text(step, 12, MUTED, true);
        LinearLayout.LayoutParams stepParams = params(-1, -2, 0, 0, 0, 12);
        root.addView(stepLabel, stepParams);
        setContentView(scroll);
    }

    private void showWelcome() {
        base("01   —   WELCOME");
        TextView title = text("A clearer kind\nof Android.", 34, INK, true);
        root.addView(title);
        TextView subtitle = text(
            "Your phone, with a little more room to feel like yours. Let's set up the Lucid details.",
            16, MUTED, false);
        root.addView(subtitle, params(-1, -2, 0, 10, 0, 22));

        ImageView hero = preview("lucid-wallpaper-dusk.jpg", 250);
        root.addView(hero);
        TextView caption = text("VIOLET DUSK  ·  THE SIGNATURE LOOK", 11, BLUE, true);
        root.addView(caption, params(-1, -2, 0, 14, 0, 8));

        TextView note = text("Nothing is locked in. Choose a look now, or skip straight to Android.", 14, MUTED, false);
        root.addView(note, params(-1, -2, 0, 0, 0, 24));
        root.addView(primary("Personalize my phone", v -> showAppearance()), params(-1, 54, 0, 0, 0, 10));
        root.addView(secondary("Skip for now", v -> finishSetup(false)), params(-1, 50, 0, 0, 0, 0));
    }

    private void showAppearance() {
        base("02   —   MAKE IT YOURS");
        root.addView(text("Set the mood.", 32, INK, true));
        root.addView(text("Pick a system colour and, if you like, a matching wallpaper. You can change both later.",
            15, MUTED, false), params(-1, -2, 0, 8, 0, 20));

        root.addView(text("SYSTEM COLOUR", 11, BLUE, true), params(-1, -2, 0, 0, 0, 10));
        root.addView(paletteChoice("Lucid Blue", "#0866FF", BLUE));
        root.addView(paletteChoice("Violet Dusk", "#7657FF", Color.rgb(118, 87, 255)));
        root.addView(paletteChoice("Cyan Horizon", "#00AFC8", Color.rgb(0, 175, 200)));

        root.addView(text("WALLPAPER", 11, BLUE, true), params(-1, -2, 0, 20, 0, 10));
        root.addView(wallpaperChoice("lucid-wallpaper-dusk.jpg", "Violet Dusk", "The signature Lucid landscape."));
        root.addView(wallpaperChoice("lucid-wallpaper-alpine.jpg", "Blue Alpine", "Cool mountain light, warm horizon."));

        root.addView(primary("Apply choices & continue", v -> finishSetup(true)), params(-1, 54, 0, 22, 0, 10));
        root.addView(secondary("Keep my current wallpaper", v -> { selectedWallpaper = null; finishSetup(true); }),
            params(-1, 50, 0, 0, 0, 0));
    }

    private View paletteChoice(String label, String seed, int swatch) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(cardBackground());
        row.setElevation(dp(1));
        LinearLayout.LayoutParams rp = params(-1, 58, 0, 0, 0, 8);
        row.setLayoutParams(rp);

        View dot = new View(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(swatch);
        dot.setBackground(circle);
        row.addView(dot, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView name = text(label, 15, INK, true);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        np.leftMargin = dp(12);
        row.addView(name, np);
        TextView selected = text(seed.equals(selectedPalette) ? "SELECTED" : "CHOOSE", 10,
            seed.equals(selectedPalette) ? BLUE : MUTED, true);
        row.addView(selected);
        row.setOnClickListener(v -> {
            selectedPalette = seed;
            showAppearance();
        });
        return row;
    }

    private View wallpaperChoice(String asset, String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(12), dp(10));
        card.setBackground(cardBackground());
        card.setElevation(dp(1));

        ImageView image = preview(asset, 112);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(78), dp(100));
        card.addView(image, ip);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.leftMargin = dp(12);
        card.addView(copy, cp);
        copy.addView(text(title, 16, INK, true));
        copy.addView(text(description, 12, MUTED, false), params(-1, -2, 0, 4, 0, 0));
        TextView state = text(asset.equals(selectedWallpaper) ? "SELECTED" : "TAP TO SELECT",
            10, asset.equals(selectedWallpaper) ? BLUE : MUTED, true);
        copy.addView(state, params(-1, -2, 0, 8, 0, 0));

        LinearLayout.LayoutParams cardParams = params(-1, -2, 0, 0, 0, 10);
        card.setLayoutParams(cardParams);
        card.setOnClickListener(v -> {
            selectedWallpaper = asset;
            showAppearance();
        });
        return card;
    }

    private ImageView preview(String asset, int heightDp) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setClipToOutline(true);
        image.setBackground(roundRect(Color.rgb(224, 222, 242), dp(22)));
        try (InputStream in = getAssets().open(asset)) {
            image.setImageBitmap(BitmapFactory.decodeStream(in));
        } catch (Exception ignored) {}
        image.setContentDescription(asset.contains("dusk") ? "Violet dusk landscape" : "Blue alpine mountain wallpaper");
        image.setLayoutParams(params(-1, dp(heightDp), 0, 0, 0, 0));
        return image;
    }

    private void finishSetup(boolean applyChoices) {
        try {
            JSONObject palette = new JSONObject();
            palette.put("android.theme.customization.system_palette", selectedPalette);
            palette.put("android.theme.customization.theme_style", "TONAL_SPOT");
            palette.put("android.theme.customization.color_source", "preset");
            palette.put("android.theme.customization.dynamic_color", "1");
            if (applyChoices) {
                if (!Settings.Secure.putString(getContentResolver(),
                    "theme_customization_overlay_packages", palette.toString())) {
                    throw new SecurityException("Android refused the system palette setting");
                }
                if (selectedWallpaper != null) {
                    try (InputStream in = getAssets().open(selectedWallpaper)) {
                        WallpaperManager.getInstance(this).setStream(in, null, true,
                            WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                    }
                }
            }
            if (!Settings.Secure.putInt(getContentResolver(), "lucid_setup_complete", 1)) {
                throw new SecurityException("Android refused to save setup completion");
            }
            Toast.makeText(this, "You're all set. Welcome to LucidOS.", Toast.LENGTH_LONG).show();
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        } catch (SecurityException e) {
            Toast.makeText(this, "Setup needs the privileged LucidOS install. Your choices were not saved.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Setup couldn't finish: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Button primary(String label, View.OnClickListener listener) {
        Button b = button(label, BLUE, Color.WHITE, listener);
        b.setElevation(dp(2));
        return b;
    }

    private Button secondary(String label, View.OnClickListener listener) {
        return button(label, Color.WHITE, INK, listener);
    }

    private Button button(String label, int bg, int fg, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(fg);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(roundRect(bg, dp(18)));
        b.setOnClickListener(listener);
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private GradientDrawable cardBackground() {
        return roundRect(Color.WHITE, dp(22));
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private LinearLayout.LayoutParams params(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
