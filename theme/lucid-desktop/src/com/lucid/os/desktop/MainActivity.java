package com.lucid.os.desktop;

import com.lucid.motion.LucidMotion;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;

/** A quiet, native-feeling control surface for the Lucid on-device desktop. */
public final class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 10, 14);
    private final int surface = Color.rgb(22, 22, 30);
    private final int raised = Color.rgb(30, 29, 41);
    private final int line = Color.rgb(54, 50, 73);
    private final int fg = Color.rgb(245, 243, 250);
    private final int muted = Color.rgb(163, 159, 179);
    private int accent = Color.rgb(151, 130, 255);
    private LinearLayout root;
    private TextView status;
    private String lastLog = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadAppearance();
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(30));
        scroll.addView(root);
        setContentView(scroll);
        LucidMotion.apply(this);

        TextView wordmark = text("LUCID  /  DESKTOP", 12, accent, true);
        root.addView(wordmark);

        TextView heading = text("Your workspace,\nunfolded.", 32, fg, true);
        LinearLayout.LayoutParams hp = params(-1, -2, 0, 12);
        root.addView(heading, hp);

        TextView intro = text("Ubuntu on your phone, with a full desktop when you want one.", 15, muted, false);
        LinearLayout.LayoutParams ip = params(-1, -2, 0, 12);
        ip.bottomMargin = dp(22);
        root.addView(intro, ip);

        buildSessionPanel();
        section("YOUR DESKTOP");
        addAction("Start desktop", "Open your Ubuntu XFCE session", true, () -> runController("start"));
        addAction("Stop session", "Close the desktop cleanly", false, () -> runController("stop"));
        section("TOOLS");
        addAction("Check setup", "Verify root, Ubuntu files and X11", false, () -> runController("check"));
        addAction("Open display server", "Switch to Termux:X11", false, this::openX11);
        section("PREFERENCES");
        addAction("Desktop root access", "Manage the desktop's root gate", false, this::showRootGateMenu);
        addAction("View diagnostics", "See what happened if something went wrong", false, this::showLog);

        TextView foot = text("Needs the Lucid desktop controller, Ubuntu rootfs and Termux:X11.", 12, muted, false);
        LinearLayout.LayoutParams fp = params(-1, -2, 2, 20);
        root.addView(foot, fp);
    }

    private void buildSessionPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(17), dp(18), dp(17));
        panel.setBackground(rounded(raised, 20, line));
        root.addView(panel, params(-1, -2, 0, 0));

        TextView eyebrow = text("SESSION", 11, accent, true);
        panel.addView(eyebrow);

        status = text("Ready when you are", 18, fg, true);
        LinearLayout.LayoutParams sp = params(-1, -2, 0, 7);
        panel.addView(status, sp);

        TextView hint = text("The desktop runs locally on this device.", 13, muted, false);
        panel.addView(hint);
    }

    private void section(String label) {
        TextView t = text(label, 11, muted, true);
        t.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams p = params(-1, -2, 2, 19);
        root.addView(t, p);
    }

    private void addAction(String title, String subtitle, boolean primary, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(14), dp(14), dp(14));
        row.setBackground(rounded(primary ? accent : surface, 16, primary ? accent : line));
        LinearLayout.LayoutParams rp = params(-1, -2, 0, 8);
        root.addView(row, rp);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        TextView titleView = text(title, 15, primary ? Color.rgb(19, 16, 30) : fg, true);
        copy.addView(titleView);
        TextView subView = text(subtitle, 12, primary ? Color.rgb(54, 45, 83) : muted, false);
        LinearLayout.LayoutParams subp = params(-1, -2, 4, 0);
        copy.addView(subView, subp);

        TextView arrow = text("›", 27, primary ? Color.rgb(30, 23, 54) : accent, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(32)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());
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

    private LinearLayout.LayoutParams params(int w, int h, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        if (top > 0) p.topMargin = dp(top);
        if (bottom > 0) p.bottomMargin = dp(bottom);
        return p;
    }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private void loadAppearance() {
        Cursor c = null;
        try {
            c = getContentResolver().query(Uri.parse("content://com.lucid.os.settings.appearance/profile"),
                new String[]{"accent"}, null, null, null);
            if (c != null && c.moveToFirst()) accent = c.getInt(0);
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
    }

    private void showRootGateMenu() {
        String[] options = {"Allow desktop root", "Disable desktop root", "Check current setting"};
        new AlertDialog.Builder(this).setTitle("Desktop root access")
            .setItems(options, (dialog, which) -> {
                String command = which == 0 ? "enable" : which == 1 ? "disable" : "status";
                runShellAction("root", command,
                    "if [ -x /system/bin/lucid_root_toggle.sh ]; then /system/bin/lucid_root_toggle.sh " + command + "; else echo 'Desktop root toggle is not installed.'; fi");
            }).show();
    }

    private void runController(String action) {
        runShellAction(action, action,
            "if [ -x /system/bin/lucid_desktop.sh ]; then /system/bin/lucid_desktop.sh " + action +
            "; else echo 'Lucid desktop controller is missing from this ROM.'; exit 127; fi");
    }

    private void runShellAction(String label, String action, String command) {
        status.setText("Working…");
        new Thread(() -> {
            String output = execRoot(command);
            lastLog = output;
            runOnUiThread(() -> {
                String state = output.contains("LUCID_DESKTOP_STARTED") ? "Desktop session started" :
                    output.contains("LUCID_DESKTOP_STOPPED") ? "Desktop session stopped" :
                    output.contains("LUCID_DESKTOP_OK") ? "Setup looks good" :
                    output.contains("Lucid Desktop diagnostics:") ? "Diagnostics ready" :
                    output.toLowerCase().contains("error") || output.toLowerCase().contains("missing") ?
                        "Needs a little attention" : "Command finished";
                status.setText(state);
                new AlertDialog.Builder(this).setTitle(label.equals("root") ? "Desktop root access" : "Lucid Desktop")
                    .setMessage(output.isEmpty() ? "No response from the desktop controller." : output)
                    .setPositiveButton("Done", null).show();
            });
        }).start();
    }

    private String execRoot(String command) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(command + " 2>&1\nexit\n");
            os.flush();
            InputStream in = p.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int count;
            while ((count = in.read(buffer)) > 0) out.write(buffer, 0, count);
            p.waitFor();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "Couldn't reach the ROM controller: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (p != null) p.destroy();
        }
    }

    private void openX11() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.termux.x11");
        if (launch == null) {
            new AlertDialog.Builder(this).setTitle("Display server not installed")
                .setMessage("Install the Termux:X11 app and companion package, then come back here.")
                .setPositiveButton("Got it", null).show();
            return;
        }
        try { startActivity(launch); }
        catch (Exception e) { Toast.makeText(this, "Couldn't open the display server.", Toast.LENGTH_LONG).show(); }
    }

    private void showLog() {
        if (lastLog.isEmpty()) {
            lastLog = execRoot("cat /data/local/lucid_desktop/lucid-desktop.log 2>/dev/null || cat /data/local/tmp/lucid-desktop.log 2>/dev/null || echo 'No desktop log yet. Try Check setup first.'");
        }
        new AlertDialog.Builder(this).setTitle("Desktop diagnostics").setMessage(lastLog)
            .setPositiveButton("Copy", (d, w) -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Lucid Desktop log", lastLog));
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Close", null).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
