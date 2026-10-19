package com.cosmos.setup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int LAVENDER = Color.rgb(198, 181, 255);
    private static final int CYAN = Color.rgb(211, 250, 251);
    private static final int PINK = Color.rgb(211, 91, 199);
    private static final int NIGHT = Color.rgb(39, 43, 46);
    private static final int BLUE = Color.rgb(0, 119, 255);
    private static final int WHITE = Color.WHITE;
    private static final int INK = Color.rgb(22, 22, 30);
    private final Handler handler = new Handler();
    private LinearLayout root;
    private int screen = 0;
    private Runnable activationFinish;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(LAVENDER);
        getWindow().setNavigationBarColor(LAVENDER);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        showWelcome();
    }

    @Override public void onBackPressed() {
        if (activationFinish != null) { handler.removeCallbacks(activationFinish); activationFinish = null; }
        if (screen == 1) showWelcome();
        else if (screen == 2) showWifi();
        else if (screen == 3) showAccount();
        else if (screen == 4) showAccount();
        else showWelcome();
    }

    private void shell(int bg, boolean lightBars) {
        screen = screenFor(bg);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(lightBars
            ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(24), dp(20), dp(24), dp(24));
        setContentView(root);
    }

    private int screenFor(int bg) { return bg == NIGHT ? 2 : bg == Color.BLACK ? 4 : 1; }

    private void showWelcome() {
        shell(LAVENDER, true);
        screen = 0;
        root.setPadding(dp(24), dp(28), dp(24), dp(26));
        TextView title = label("Let's set up your device", 25, INK, true);
        root.addView(title, margins(-1, -2, 0, 0, 0, 4));
        root.addView(label("Running LucidOS", 14, Color.rgb(70, 59, 101), false),
            margins(-1, -2, 0, 0, 0, 4));
        root.addView(new ArtView(this, ArtView.WELCOME), new LinearLayout.LayoutParams(-1, 0, 1f));
        Button start = pill("Let's start!", PINK, WHITE, v -> showWifi());
        root.addView(start, margins(-1, 54, 0, 0, 0, 10));
        root.addView(label("Your phone. Your pace.", 12, Color.rgb(92, 77, 130), false),
            centeredMargins(-1, -2, 0, 0, 0, 0));
    }

    private void showWifi() {
        shell(LAVENDER, true);
        screen = 1;
        root.setPadding(dp(0), dp(0), dp(0), dp(20));
        TextView title = label("Connect to Wi-Fi", 23, Color.BLACK, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, margins(-1, -2, 22, 22, 22, 8));
        root.addView(new ArtView(this, ArtView.WIFI), new LinearLayout.LayoutParams(-1, 0, 1f));
        Button wifi = pill("◉    Open Wi-Fi settings", PINK, WHITE, v -> openWifiSettings());
        root.addView(wifi, margins(-1, 54, 30, 0, 30, 12));
        Button skip = pill("Set up offline", Color.TRANSPARENT, Color.BLACK, v -> showAccount());
        skip.setBackground(roundRect(Color.TRANSPARENT, 28, Color.rgb(110, 91, 158), dp(1)));
        root.addView(skip, margins(-1, 50, 30, 0, 30, 10));
        root.addView(label("You can connect later in Settings.", 12,
            Color.rgb(83, 71, 117), false), centeredMargins(-1, -2, 0, 0, 0, 0));
    }

    private void openWifiSettings() {
        try {
            Intent panel = new Intent("android.settings.panel.action.WIFI");
            if (panel.resolveActivity(getPackageManager()) != null) startActivity(panel);
            else startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            Toast.makeText(this, "Connect to a network, then return here.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Open Settings → Network & internet → Wi-Fi.", Toast.LENGTH_LONG).show();
        }
    }

    private void showAccount() {
        shell(NIGHT, false);
        screen = 2;
        root.setPadding(dp(20), dp(0), dp(20), dp(18));
        ArtView art = new ArtView(this, ArtView.MOON);
        root.addView(art, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(4), dp(20), dp(4), dp(4));
        root.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        View handle = new View(this);
        handle.setBackground(roundRect(Color.WHITE, 2, Color.TRANSPARENT, 0));
        panel.addView(handle, centeredMargins(dp(28), dp(3), 0, 0, 0, 22));
        panel.addView(label("Login or sign up", 18, WHITE, true), centeredMargins(-1, -2, 0, 0, 0, 8));
        panel.addView(label("Please select your preferred method\nto continue setting up your account",
            12, Color.LTGRAY, false), centeredMargins(-1, -2, 0, 0, 0, 22));
        panel.addView(pill("Continue with Email", BLUE, WHITE, v -> comingSoon("Email sign-in")),
            margins(-1, 50, 0, 0, 0, 8));
        panel.addView(pill("Continue with Phone", NIGHT, WHITE, v -> showPhone()),
            margins(-1, 48, 0, 0, 0, 8));
        LinearLayout providers = new LinearLayout(this);
        providers.setOrientation(LinearLayout.HORIZONTAL);
        Button google = pill("G", NIGHT, WHITE, v -> comingSoon("Google sign-in"));
        Button apple = pill("●", NIGHT, WHITE, v -> comingSoon("Apple sign-in"));
        providers.addView(google, new LinearLayout.LayoutParams(0, dp(48), 1f));
        providers.addView(apple, margins(0, 48, 8, 0, 0, 0, 1f));
        panel.addView(providers, margins(-1, 48, 0, 0, 0, 24));
        panel.addView(label("Account services are coming later.\nYou can skip this step for now.",
            11, Color.LTGRAY, false), centeredMargins(-1, -2, 0, 0, 0, 18));
        panel.addView(pill("Skip account setup", NIGHT, WHITE, v -> showActivation()),
            margins(-1, 46, 0, 0, 0, 0));
    }

    private void showPhone() {
        shell(NIGHT, false);
        screen = 3;
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        TextView back = label("‹", 32, WHITE, false);
        back.setOnClickListener(v -> showAccount());
        root.addView(back, margins(-1, dp(48), 0, 0, 0, 28));
        root.addView(label("♧", 28, Color.LTGRAY, false), margins(-1, -2, 0, 0, 0, 8));
        root.addView(label("Your phone number", 20, WHITE, true), margins(-1, -2, 0, 0, 0, 8));
        root.addView(label("Phone verification will be available in a future update. No number is needed right now.",
            14, Color.LTGRAY, false), margins(-1, -2, 0, 0, 0, 24));
        TextView placeholder = label("Phone verification not enabled yet", 14, Color.LTGRAY, false);
        placeholder.setPadding(dp(14), dp(16), dp(14), dp(16));
        placeholder.setBackground(roundRect(Color.rgb(48, 53, 58), 10, BLUE, dp(1)));
        root.addView(placeholder, margins(-1, -2, 0, 0, 0, 0));
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        root.addView(pill("Continue", BLUE, WHITE, v -> showActivation()), margins(-1, 52, 0, 0, 0, 0));
    }

    private void showActivation() {
        shell(LAVENDER, true);
        screen = 4;
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(24), dp(28), dp(24));
        root.addView(new ArtView(this, ArtView.ACTIVATE), new LinearLayout.LayoutParams(-1, dp(240)));
        root.addView(label("Making it yours", 25, INK, true), centeredMargins(-1, -2, 0, 24, 0, 8));
        TextView status = label("Getting LucidOS ready…", 14, Color.rgb(80, 67, 117), false);
        root.addView(status, centeredMargins(-1, -2, 0, 0, 0, 0));
        activationFinish = () -> { activationFinish = null; finishSetup(); };
        handler.postDelayed(activationFinish, 1800);
    }

    private void finishSetup() {
        try {
            if (!Settings.Secure.putInt(getContentResolver(), "lucid_setup_complete", 1)) {
                throw new SecurityException("completion setting rejected");
            }
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        } catch (SecurityException e) {
            Toast.makeText(this, "Setup needs the privileged LucidOS install. Completion wasn't saved.", Toast.LENGTH_LONG).show();
            showAccount();
        }
    }

    private void comingSoon(String feature) {
        Toast.makeText(this, feature + " is planned for a later update. No data was collected.", Toast.LENGTH_LONG).show();
    }

    private Button pill(String text, int bg, int fg, View.OnClickListener action) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(fg);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundRect(bg, 28, Color.TRANSPARENT, 0));
        b.setOnClickListener(action);
        return b;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private GradientDrawable roundRect(int color, int radius, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeWidth > 0) d.setStroke(strokeWidth, stroke);
        return d;
    }

    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private LinearLayout.LayoutParams centeredMargins(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = margins(w, h, l, t, r, b);
        p.gravity = Gravity.CENTER_HORIZONTAL;
        return p;
    }

    private LinearLayout.LayoutParams margins(int w, int h, int l, int t, int r, int b, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class ArtView extends View {
        static final int WELCOME = 0, WIFI = 1, MOON = 2, ACTIVATE = 3;
        private final int kind;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final long start = System.currentTimeMillis();
        ArtView(Context c, int type) { super(c); kind = type; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight();
            if (kind == MOON) {
                c.drawColor(Color.rgb(158, 80, 233));
                p.setShader(new android.graphics.LinearGradient(0, 0, w, h,
                    Color.rgb(150, 99, 239), Color.rgb(225, 55, 173), android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, h, p); p.setShader(null);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(5)); p.setColor(0x55FFFFFF);
                for (int i=0;i<5;i++) c.drawCircle(w*(0.15f+i*.22f), h*.2f, w*(.22f+i*.035f), p);
                p.setStyle(Paint.Style.FILL);
                p.setShadowLayer(dp(12), 0, dp(8), 0x66531A82);
                p.setColor(Color.rgb(246, 207, 255));
                c.save(); c.rotate(-28, w*.5f, h*.6f);
                c.drawCircle(w*.5f, h*.58f, w*.22f, p);
                p.setShadowLayer(0,0,0,0); p.setColor(Color.rgb(190, 100, 224));
                c.drawCircle(w*.59f, h*.51f, w*.2f, p); c.restore();
                return;
            }
            if (kind == ACTIVATE) {
                c.drawColor(LAVENDER);
                float pulse = (System.currentTimeMillis()-start)%1000/1000f;
                for (int i=0;i<7;i++) {
                    float a = (float)(i*Math.PI*2/7);
                    float radius = w*.22f + (float)Math.sin(pulse*Math.PI*2+i)*dp(4);
                    p.setColor(i%2==0 ? Color.rgb(100,224,239) : PINK);
                    c.drawCircle(w*.5f+(float)Math.cos(a)*radius, h*.5f+(float)Math.sin(a)*radius,
                        dp(7 + (i%3)*2), p);
                }
                postInvalidateDelayed(80);
                return;
            }
            c.drawColor(LAVENDER);
            p.setShader(null); p.setColor(CYAN);
            path.reset();
            if (kind == WIFI) {
                path.moveTo(0,h*.12f); path.cubicTo(w*.18f,h*.05f,w*.12f,h*.3f,w*.27f,h*.37f);
                path.cubicTo(w*.38f,h*.47f,w*.2f,h*.55f,w*.05f,h*.56f); path.cubicTo(0,h*.58f,0,h*.6f,0,h*.6f);
            } else {
                path.moveTo(0,h*.58f); path.cubicTo(w*.2f,h*.48f,w*.24f,h*.64f,w*.34f,h*.72f);
                path.cubicTo(w*.48f,h*.8f,w*.33f,h*.92f,w*.2f,h*.92f); path.cubicTo(w*.08f,h*.94f,0,h*.85f,0,h*.82f);
            }
            path.close(); c.drawPath(path,p);
            p.setShader(new android.graphics.LinearGradient(w*.55f,h*.8f,w,h,
                Color.rgb(90,81,120), Color.rgb(13,34,49), android.graphics.Shader.TileMode.CLAMP));
            path.reset();
            path.moveTo(w,h*.47f); path.cubicTo(w*.72f,h*.49f,w*.79f,h*.62f,w*.61f,h*.72f);
            path.cubicTo(w*.43f,h*.83f,w*.66f,h*.9f,w*.78f,h*.95f); path.lineTo(w,h); path.close();
            c.drawPath(path,p); p.setShader(null);
        }
    }
}
