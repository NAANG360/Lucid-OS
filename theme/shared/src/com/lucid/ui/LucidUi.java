package com.lucid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Shared Lucid design tokens and components.
 * Colour, radius and glass values are read from the cosmosOS Figma file (LucidOS page):
 * Control Center 63:2, Locked 40:243, Homescreen 330:111. Product Sans is not shipped with
 * Android, so type uses the platform medium/bold sans with tight tracking.
 */
public final class LucidUi {
    private LucidUi() {}

    // Figma: Control Center / Locked
    public static final int BLUE = 0xFF0A6AFF;         // active controls (#0A6AFF / #086AFE)
    public static final int BLUE_LIGHT = 0xFF1BA8FF;   // tile gradient end
    public static final int PERIWINKLE = 0xFF9091F4;   // canvas gradient start
    public static final int INDIGO = 0xFF6D6EC1;       // canvas gradient middle
    public static final int ROSE = 0xFFE5C1CB;         // canvas gradient end
    public static final int MIST = 0xFFEFF0F4;         // pale glass surface
    public static final int INK = 0xFF4F4F59;          // primary label
    public static final int INK_STRONG = 0xFF23232B;
    public static final int INK_SOFT = 0xFF777777;
    public static final int AMBER = 0xFFFFBA1E;
    public static final int CORAL = 0xFFFB5152;
    // Figma: Homescreen glass widgets are white at 25% and 50%
    public static final int GLASS_LOW = 0x40FFFFFF;
    public static final int GLASS_MID = 0x80FFFFFF;
    public static final int GLASS_EDGE = 0x55FFFFFF;
    public static final int ON_WALLPAPER = 0xFFFFFFFF;
    public static final int ON_WALLPAPER_SOFT = 0xE6FFFFFF;

    public interface Pick { void on(int index); }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable glass(Context c, int fill, float radiusDp, boolean edge) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (edge) d.setStroke(Math.max(1, dp(c, 1f)), GLASS_EDGE);
        return d;
    }

    public static GradientDrawable canvas() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{PERIWINKLE, INDIGO, ROSE});
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    public static void shadow(Context c, TextView t) {
        t.setShadowLayer(dp(c, 3), 0, dp(c, 1), 0x66000000);
    }

    /** Transparent system bars, content drawn edge to edge, padding follows real insets. */
    public static void edgeToEdge(Activity a, final View root, final int baseL, final int baseT,
                                  final int baseR, final int baseB) {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            w.setNavigationBarContrastEnforced(false);
            w.setStatusBarContrastEnforced(false);
        }
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                v.setPadding(baseL + in.getSystemWindowInsetLeft(), baseT + in.getSystemWindowInsetTop(),
                    baseR + in.getSystemWindowInsetRight(), baseB + in.getSystemWindowInsetBottom());
                return in;
            }
        });
        root.requestApplyInsets();
    }

    /** Bottom sheet in the Lucid pale-glass style. selected = -1 for a plain action list. */
    public static Dialog sheet(Activity a, String title, String[] items, int selected, final Pick cb) {
        final Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a, 8), dp(a, 18), dp(a, 8), dp(a, 10));
        card.setBackground(glass(a, 0xF2EFF0F4, 28, false));
        TextView t = text(a, title, 17, INK_STRONG, true);
        t.setPadding(dp(a, 16), 0, dp(a, 16), dp(a, 10));
        card.addView(t);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(a, 16), 0, dp(a, 16), 0);
            row.setMinimumHeight(dp(a, 52));
            row.setClickable(true);
            row.setBackground(pressable(a));
            View dot = new View(a);
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(i == selected ? BLUE : Color.TRANSPARENT);
            dot.setBackground(dg);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(a, 8), dp(a, 8));
            dlp.rightMargin = dp(a, selected >= 0 ? 12 : 0);
            if (selected >= 0) row.addView(dot, dlp);
            TextView label = text(a, items[i], 16, i == selected ? BLUE : INK, i == selected);
            row.addView(label);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { d.dismiss(); if (cb != null) cb.on(idx); }
            });
            card.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        d.setContentView(card);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.y = dp(a, 12);
            lp.horizontalMargin = 0f;
            lp.dimAmount = 0.35f;
            w.setAttributes(lp);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setWindowAnimations(android.R.style.Animation_InputMethod);
            w.getDecorView().setPadding(dp(a, 12), 0, dp(a, 12), 0);
            if (Build.VERSION.SDK_INT >= 31) {
                try { w.setBackgroundBlurRadius(dp(a, 24)); } catch (Throwable ignored) {}
            }
        }
        d.show();
        return d;
    }

    public static Drawable pressable(Context c) {
        android.content.res.ColorStateList tint = android.content.res.ColorStateList.valueOf(0x220A6AFF);
        return new android.graphics.drawable.RippleDrawable(tint, null, glass(c, Color.WHITE, 16, false));
    }

    /** Hand-drawn glyphs so the UI never depends on emoji or symbol fonts being present. */
    public static final class Glyph extends Drawable {
        public static final int SEARCH = 0, SETTINGS = 1, WALLPAPER = 2, CONTROL = 3, UPDATE = 4,
            INFO = 5, TUNE = 6, DESKTOP = 7;
        private final int kind;
        private final int color;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF r = new RectF();

        public Glyph(int kind, int color) { this.kind = kind; this.color = color; }

        private float x0, y0, u;
        private float X(float v) { return x0 + v * u; }
        private float Y(float v) { return y0 + v * u; }

        private void line(Canvas c, float a, float b, float d, float e) { c.drawLine(X(a), Y(b), X(d), Y(e), p); }
        private void dot(Canvas c, float cx, float cy, float rad) {
            Paint.Style old = p.getStyle(); p.setStyle(Paint.Style.FILL);
            c.drawCircle(X(cx), Y(cy), rad * u, p); p.setStyle(old);
        }
        private void box(Canvas c, float l, float t, float rr, float b, float rad, boolean fill) {
            r.set(X(l), Y(t), X(rr), Y(b));
            Paint.Style old = p.getStyle();
            p.setStyle(fill ? Paint.Style.FILL : Paint.Style.STROKE);
            c.drawRoundRect(r, rad * u, rad * u, p); p.setStyle(old);
        }

        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            float s = Math.min(b.width(), b.height());
            u = s / 24f;
            x0 = b.left + (b.width() - s) / 2f;
            y0 = b.top + (b.height() - s) / 2f;
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1.5f, u * 1.9f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            switch (kind) {
                case SEARCH:
                    c.drawCircle(X(10.5f), Y(10.5f), 6.5f * u, p);
                    line(c, 15.5f, 15.5f, 20f, 20f);
                    break;
                case SETTINGS:
                    line(c, 4, 6, 20, 6); line(c, 4, 12, 20, 12); line(c, 4, 18, 20, 18);
                    dot(c, 9, 6, 2.6f); dot(c, 15, 12, 2.6f); dot(c, 8, 18, 2.6f);
                    break;
                case TUNE:
                    line(c, 4, 8, 20, 8); line(c, 4, 16, 20, 16);
                    dot(c, 8, 8, 2.7f); dot(c, 16, 16, 2.7f);
                    break;
                case WALLPAPER:
                    box(c, 3.5f, 4.5f, 20.5f, 19.5f, 3f, false);
                    dot(c, 9, 10, 1.8f);
                    path.reset();
                    path.moveTo(X(4.5f), Y(17f)); path.lineTo(X(10f), Y(12f)); path.lineTo(X(14f), Y(15.5f));
                    path.lineTo(X(17f), Y(13f)); path.lineTo(X(20f), Y(16.5f));
                    c.drawPath(path, p);
                    break;
                case CONTROL:
                    box(c, 4, 4, 10.5f, 10.5f, 2.4f, true); box(c, 13.5f, 4, 20, 10.5f, 2.4f, false);
                    box(c, 4, 13.5f, 10.5f, 20, 2.4f, false); box(c, 13.5f, 13.5f, 20, 20, 2.4f, true);
                    break;
                case UPDATE:
                    r.set(X(5), Y(5), X(19), Y(19));
                    c.drawArc(r, -40f, 280f, false, p);
                    path.reset();
                    path.moveTo(X(8.5f), Y(2.6f)); path.lineTo(X(8.5f), Y(9.2f)); path.lineTo(X(13f), Y(5.9f)); path.close();
                    p.setStyle(Paint.Style.FILL); c.drawPath(path, p); p.setStyle(Paint.Style.STROKE);
                    break;
                case INFO:
                    c.drawCircle(X(12), Y(12), 8f * u, p);
                    dot(c, 12, 8, 1.1f); line(c, 12, 11.2f, 12, 16.5f);
                    break;
                case DESKTOP:
                    box(c, 3.5f, 5, 20.5f, 16, 2.2f, false);
                    line(c, 12, 16, 12, 19.5f); line(c, 8.5f, 19.5f, 15.5f, 19.5f);
                    break;
                default: break;
            }
        }
        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
