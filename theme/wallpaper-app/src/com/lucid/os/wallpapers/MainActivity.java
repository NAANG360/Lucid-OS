package com.lucid.os.wallpapers;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(13, 10, 25);
    private static final int INK = Color.rgb(245, 240, 255);
    private static final int BLUE = Color.rgb(160, 96, 255);
    private static final int MUTED = Color.rgb(184, 164, 220);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(28));
        scroll.addView(root);

        TextView brand = text("L U C I D   /   PERSONALIZATION", 11, BLUE, true);
        root.addView(brand);

        TextView title = text("Wallpapers", 32, INK, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(10);
        root.addView(title, titleParams);

        TextView subtitle = text("Choose the atmosphere for your device.", 15, MUTED, false);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(4);
        subParams.bottomMargin = dp(22);
        root.addView(subtitle, subParams);

        root.addView(paletteCard());

        root.addView(wallpaperCard(
            "lucid-wallpaper-dusk.jpg",
            "01  /  DUSK",
            "Moonlit valley",
            "The signature LucidOS look: violet skies, deep purple shadows, and a soft lilac horizon."
        ));
        root.addView(wallpaperCard(
            "lucid-wallpaper-alpine.jpg",
            "02  /  ALPINE",
            "Violet mountains",
            "A purple-forward alpine scene with indigo shadows and lilac light."
        ));

        TextView footer = text("LUCID OS  •  LIGHT, WITHOUT THE NOISE", 10, MUTED, true);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(20);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, footerParams);

        setContentView(scroll);
    }

    private View paletteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundRect(Color.rgb(28, 20, 45), dp(24)));
        card.setElevation(dp(3));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(18);
        card.setLayoutParams(cardParams);

        TextView heading = text("System palette", 20, INK, true);
        card.addView(heading);
        TextView description = text(
            "Apply a Lucid color profile to Android surfaces, including Settings and system UI.",
            13, MUTED, false
        );
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(5);
        descParams.bottomMargin = dp(12);
        card.addView(description, descParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(paletteButton("Lucid Violet", "#0866FF", BLUE));
        row.addView(paletteButton("Violet", "#7657FF", Color.rgb(118, 87, 255)));
        row.addView(paletteButton("Cyan", "#00AFC8", Color.rgb(0, 175, 200)));
        card.addView(row);
        return card;
    }

    private Button paletteButton(String label, String sourceColor, int buttonColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(roundRect(buttonColor, dp(16)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
        params.leftMargin = dp(3);
        params.rightMargin = dp(3);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                applyPalette(label, sourceColor);
            }
        });
        button.setLayoutParams(params);
        return button;
    }

    private void applyPalette(String label, String sourceColor) {
        try {
            boolean changed = LucidPalette.apply(this, sourceColor);
            if (changed) {
                Toast.makeText(this, label + " palette applied. System colors may take a moment to update.",
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Android did not accept the palette change.", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Palette control needs the privileged LucidOS install.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't apply palette: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private View wallpaperCard(String asset, String eyebrow, String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(16));
        card.setBackground(roundRect(Color.WHITE, dp(26)));
        card.setElevation(dp(4));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(18);

        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(roundRect(Color.rgb(224, 222, 242), dp(18)));
        preview.setClipToOutline(true);
        try (InputStream in = getAssets().open(asset)) {
            preview.setImageBitmap(BitmapFactory.decodeStream(in));
        } catch (Exception ignored) {
            preview.setImageDrawable(null);
        }
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, dp(300));
        card.addView(preview, imageParams);

        TextView label = text(eyebrow, 10, BLUE, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(15);
        card.addView(label, labelParams);

        TextView heading = text(title, 21, INK, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(5);
        card.addView(heading, headingParams);

        TextView body = text(description, 13, MUTED, false);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.topMargin = dp(5);
        card.addView(body, bodyParams);

        Button button = new Button(this);
        button.setText("Set as wallpaper");
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(roundRect(BLUE, dp(18)));
        button.setPadding(dp(12), dp(7), dp(12), dp(7));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(52));
        buttonParams.topMargin = dp(14);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                applyWallpaper(asset);
            }
        });
        card.addView(button, buttonParams);

        card.setLayoutParams(cardParams);
        return card;
    }

    private void applyWallpaper(String asset) {
        try (InputStream in = getAssets().open(asset)) {
            WallpaperManager.getInstance(this).setStream(
                in, null, true,
                WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK
            );
            Toast.makeText(this, "Lucid wallpaper applied.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't apply wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
