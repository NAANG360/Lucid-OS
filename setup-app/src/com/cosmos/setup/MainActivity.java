package com.cosmos.setup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.text.InputType;
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
    private WifiManager wifiManager;
    private LinearLayout networksList;
    private EditText passwordInput;
    private String selectedSsid;
    private TextView wifiStatus;
    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { renderNetworks(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(LAVENDER);
        getWindow().setNavigationBarColor(LAVENDER);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        showWelcome();
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(scanReceiver); } catch (Exception ignored) {}
        if (activationFinish != null) handler.removeCallbacks(activationFinish);
        super.onDestroy();
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
        root.setPadding(dp(24), dp(14), dp(24), dp(18));
        root.addView(label("Connect to Wi-Fi", 26, INK, true), margins(-1, -2, 0, 0, 0, 4));
        root.addView(label("Choose a network to get your phone online.", 14, Color.rgb(70, 59, 101), false), margins(-1, -2, 0, 0, 0, 14));
        wifiStatus = label("Searching for nearby networks…", 13, Color.rgb(64, 54, 95), false);
        root.addView(wifiStatus, margins(-1, -2, 0, 0, 0, 8));
        ScrollView scroll = new ScrollView(this);
        networksList = new LinearLayout(this);
        networksList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(networksList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        passwordInput = new EditText(this);
        passwordInput.setSingleLine(true);
        passwordInput.setTextSize(16);
        passwordInput.setHint("Wi-Fi password (if required)");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setPadding(dp(14), dp(8), dp(14), dp(8));
        passwordInput.setBackground(roundRect(Color.WHITE, 12, Color.rgb(135, 120, 177), dp(1)));
        root.addView(passwordInput, margins(-1, 52, 0, 10, 0, 8));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = pill("Refresh", Color.WHITE, INK, v -> scanNetworks());
        refresh.setBackground(roundRect(Color.WHITE, 12, Color.rgb(135, 120, 177), dp(1)));
        Button connect = pill("Connect", PINK, WHITE, v -> connectSelectedNetwork());
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout.LayoutParams connectLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        connectLp.leftMargin = dp(10);
        actions.addView(connect, connectLp);
        root.addView(actions, margins(-1, 50, 0, 0, 0, 8));
        Button offline = pill("Set up offline", Color.TRANSPARENT, INK, v -> showAccount());
        offline.setBackground(roundRect(Color.TRANSPARENT, 12, Color.rgb(110, 91, 158), dp(1)));
        root.addView(offline, margins(-1, 48, 0, 0, 0, 4));
        root.addView(label("Your password stays on this device.", 12, Color.rgb(83, 71, 117), false), centeredMargins(-1, -2, 0, 8, 0, 0));
        try { registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); } catch (Exception ignored) {}
        renderNetworks();
        scanNetworks();
    }

    private void scanNetworks() {
        if (wifiManager == null) { wifiStatus.setText("Wi-Fi service is unavailable on this build."); return; }
        try {
            if (!wifiManager.isWifiEnabled() && !wifiManager.setWifiEnabled(true))
                wifiStatus.setText("Wi-Fi could not be enabled. Check privileged setup permissions.");
            boolean started = wifiManager.startScan();
            if (!started) renderNetworks(); else wifiStatus.setText("Scanning for networks…");
        } catch (SecurityException e) {
            wifiStatus.setText("Wi-Fi scan permission missing. Install as the privileged setup app.");
        } catch (Exception e) { wifiStatus.setText("Could not scan. Tap Refresh to retry."); }
    }

    private void renderNetworks() {
        if (networksList == null || wifiManager == null) return;
        networksList.removeAllViews();
        try {
            java.util.LinkedHashMap<String, ScanResult> unique = new java.util.LinkedHashMap<>();
            java.util.List<ScanResult> results = wifiManager.getScanResults();
            if (results != null) for (ScanResult result : results) {
                if (result.SSID != null && !result.SSID.trim().isEmpty()) {
                    ScanResult old = unique.get(result.SSID);
                    if (old == null || result.level > old.level) unique.put(result.SSID, result);
                }
            }
            for (ScanResult result : unique.values()) {
                final String ssid = result.SSID;
                String caps = result.capabilities == null ? "" : result.capabilities;
                String security = (caps.contains("WPA") || caps.contains("WEP") || caps.contains("SAE")) ? "Password required" : "Open network";
                Button item = pill((ssid.equals(selectedSsid) ? "✓  " : "") + ssid + "   ·   " + security,
                    ssid.equals(selectedSsid) ? Color.rgb(225, 215, 255) : Color.WHITE, INK, v -> { selectedSsid = ssid; renderNetworks(); });
                item.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                item.setPadding(dp(14), 0, dp(10), 0);
                item.setTextSize(14);
                item.setBackground(roundRect(ssid.equals(selectedSsid) ? Color.rgb(225, 215, 255) : Color.WHITE, 12, Color.rgb(195, 183, 225), dp(1)));
                networksList.addView(item, margins(-1, 52, 0, 0, 0, 7));
            }
            if (unique.isEmpty()) {
                networksList.addView(label("No networks found yet. Make sure Wi-Fi is on, then tap Refresh.", 14, INK, false), margins(-1, -2, 2, 10, 2, 10));
                wifiStatus.setText("No networks found.");
            } else wifiStatus.setText(unique.size() + " networks found · select one below");
        } catch (SecurityException e) { wifiStatus.setText("Wi-Fi scan results require privileged permissions."); }
    }

    @SuppressWarnings("deprecation")
    private void connectSelectedNetwork() {
        if (selectedSsid == null) { Toast.makeText(this, "Select a Wi-Fi network first.", Toast.LENGTH_SHORT).show(); return; }
        if (wifiManager == null) return;
        String password = passwordInput.getText().toString();
        try {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + selectedSsid + "\"";
            String caps = "";
            java.util.List<ScanResult> scans = wifiManager.getScanResults();
            if (scans != null) for (ScanResult scan : scans) if (selectedSsid.equals(scan.SSID)) { caps = scan.capabilities == null ? "" : scan.capabilities; break; }
            if (caps.contains("WEP")) {
                config.wepKeys[0] = "\"" + password + "\"";
                config.wepTxKeyIndex = 0;
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            } else if (caps.contains("WPA") || caps.contains("SAE")) {
                if (password.isEmpty()) { Toast.makeText(this, "Enter this network's password.", Toast.LENGTH_SHORT).show(); return; }
                config.preSharedKey = "\"" + password + "\"";
            } else config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            int id = wifiManager.addNetwork(config);
            if (id < 0) { wifiStatus.setText("Android rejected this network. Check platform signing and privileged permissions."); return; }
            boolean enabled = wifiManager.enableNetwork(id, true);
            wifiManager.reconnect();
            wifiStatus.setText(enabled ? "Connecting to " + selectedSsid + "…" : "Connection request rejected.");
            handler.postDelayed(() -> {
                try {
                    android.net.ConnectivityManager cm = (android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
                    android.net.NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
                    String connected = wifiManager.getConnectionInfo().getSSID();
                    if (info != null && info.isConnected() && connected != null && connected.replace("\"", "").equals(selectedSsid)) {
                        wifiStatus.setText("Connected to " + selectedSsid + ".");
                        showAccount();
                    } else wifiStatus.setText("Not connected yet. Check the password or retry.");
                } catch (Exception e) { wifiStatus.setText("Connection status unavailable. Check the password or retry."); }
            }, 5000);
        } catch (SecurityException e) { wifiStatus.setText("Wi-Fi connection requires the platform-signed privileged setup app."); }
        catch (Exception e) { wifiStatus.setText("Could not connect. Verify the password and retry."); }
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
        Button apple = pill("Apple", NIGHT, WHITE, v -> comingSoon("Apple sign-in"));
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
        root.addView(label("☎", 28, Color.LTGRAY, false), margins(-1, -2, 0, 0, 0, 8));
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
        b.setTextSize(15);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        b.setIncludeFontPadding(true);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine(true);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26000000), roundRect(bg, 12, Color.TRANSPARENT, 0), null));
        b.setElevation(dp(2));
        b.setStateListAnimator(null);
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

    private static int dpView(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

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
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dpView(getContext(), 5)); p.setColor(0x55FFFFFF);
                for (int i=0;i<5;i++) c.drawCircle(w*(0.15f+i*.22f), h*.2f, w*(.22f+i*.035f), p);
                p.setStyle(Paint.Style.FILL);
                p.setShadowLayer(dpView(getContext(), 12), 0, dpView(getContext(), 8), 0x66531A82);
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
                    float radius = w*.22f + (float)Math.sin(pulse*Math.PI*2+i)*dpView(getContext(), 4);
                    p.setColor(i%2==0 ? Color.rgb(100,224,239) : PINK);
                    c.drawCircle(w*.5f+(float)Math.cos(a)*radius, h*.5f+(float)Math.sin(a)*radius,
                        dpView(getContext(), 7 + (i%3)*2), p);
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
