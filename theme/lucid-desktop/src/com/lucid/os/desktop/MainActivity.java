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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;

/** UI for the ROM-side desktop controller. Privileged operations remain in a reviewed ROM script. */
public final class MainActivity extends Activity {
    private final int bg = Color.rgb(12, 13, 19);
    private final int card = Color.rgb(25, 26, 38);
    private final int cardHigh = Color.rgb(36, 37, 52);
    private final int fg = Color.rgb(245, 244, 252);
    private final int muted = Color.rgb(167, 166, 187);
    private int accent = Color.rgb(139, 119, 255);
    private LinearLayout root;
    private TextView status;
    private String lastLog = "";

    @Override
    public void onCreate(Bundle state) {
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
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        root.setBackgroundColor(bg);
        scroll.addView(root);
        setContentView(scroll);
        LucidMotion.apply(this);

        TextView brand = new TextView(this);
        brand.setText("LUCID  /  DESKTOP");
        brand.setTextColor(accent);
        brand.setTextSize(11);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(brand);

        TextView title = new TextView(this);
        title.setText("Your phone. A desktop.");
        title.setTextColor(fg);
        title.setTextSize(30);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(8);
        root.addView(title, tp);

        TextView desc = new TextView(this);
        desc.setText("Start the Ubuntu XFCE environment on-device and render its desktop through the Termux:X11 display server. Lucid verifies prerequisites before launching.");
        desc.setTextColor(muted);
        desc.setTextSize(14);
        LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(-1, -2);
        dpParams.topMargin = dp(6);
        dpParams.bottomMargin = dp(18);
        root.addView(desc, dpParams);

        status = new TextView(this);
        status.setText("STATUS  •  Ready to check prerequisites");
        status.setTextColor(fg);
        status.setTextSize(14);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setPadding(dp(16), dp(16), dp(16), dp(16));
        status.setBackground(shape(cardHigh, 18));
        root.addView(status);

        addButton("Start Lucid Desktop", "Start the chroot XFCE desktop after the X11 display is listening", () -> runController("start"));
        addButton("Stop desktop", "Stop the active desktop session cleanly without rebooting", () -> runController("stop"));
        addButton("Check requirements", "Check root, Ubuntu rootfs, X11 socket, mounts and DNS", () -> runController("check"));
        addButton("Desktop root gate", "Enable or disable persist.lucid.desktop_root via /system/bin/lucid_root_toggle.sh", this::showRootGateMenu);
        addButton("Open X11 server", "Launch the installed Termux:X11 display server app", this::openX11);
        addButton("View diagnostics", "Read or copy the latest desktop diagnostic log", this::showLog);

        TextView note = new TextView(this);
        note.setText("Requires a ROM build that includes /system/bin/lucid_desktop.sh, /system/usr/lucid/ubuntu-base-arm64.tar.gz, and a compatible X11 server app.");
        note.setTextColor(muted);
        note.setTextSize(12);
        note.setPadding(dp(4), dp(18), dp(4), dp(4));
        root.addView(note);
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

    private GradientDrawable shape(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), Color.rgb(49, 48, 68));
        return d;
    }

    private void addButton(String title, String sub, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(shape(card, 16));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(10);
        root.addView(row, rp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(fg);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(t);

        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(muted);
        s.setTextSize(12);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4);
        row.addView(s, sp);
        row.setOnClickListener(v -> action.run());
    }

    private void showRootGateMenu() {
        String[] opts = {"Enable Desktop Root", "Disable Desktop Root", "Query Status"};
        new AlertDialog.Builder(this)
            .setTitle("Desktop Root Gate")
            .setItems(opts, (d, which) -> {
                final String cmd = which == 0 ? "enable" : (which == 1 ? "disable" : "status");
                status.setText("STATUS  •  Updating root gate (" + cmd + ")…");
                new Thread(() -> {
                    final String out = execRoot("if [ -x /system/bin/lucid_root_toggle.sh ]; then /system/bin/lucid_root_toggle.sh " + cmd + "; else echo 'Missing /system/bin/lucid_root_toggle.sh'; fi");
                    lastLog = out;
                    runOnUiThread(() -> {
                        status.setText("STATUS  •  Root gate: " + cmd);
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Root Gate · " + cmd)
                            .setMessage(out)
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }).start();
            })
            .show();
    }

    private void runController(final String action) {
        status.setText("STATUS  •  Running " + action + "…");
        new Thread(() -> {
            final String output = execRoot("if [ -x /system/bin/lucid_desktop.sh ]; then /system/bin/lucid_desktop.sh " + action + "; else echo 'Missing /system/bin/lucid_desktop.sh: this ROM build does not include the desktop controller.'; exit 127; fi");
            lastLog = output;
            runOnUiThread(() -> {
                status.setText(
                    output.contains("LUCID_DESKTOP_OK") || output.contains("Lucid Desktop diagnostics:") ? "STATUS  •  Checked" :
                    output.contains("LUCID_DESKTOP_STARTED") ? "STATUS  •  Running XFCE" :
                    output.contains("LUCID_DESKTOP_STOPPED") ? "STATUS  •  Stopped" :
                    "STATUS  •  Needs attention"
                );
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Lucid Desktop · " + action)
                    .setMessage(output.isEmpty() ? "No output returned. Check ROM integration and root permissions." : output)
                    .setPositiveButton("OK", null)
                    .show();
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
            byte[] b = new byte[2048];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            p.waitFor();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "Could not run the ROM controller as root: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\nEnsure this ROM has its configured root provider and desktop init integration.";
        } finally {
            if (p != null) p.destroy();
        }
    }

    private void openX11() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.termux.x11");
        if (i == null) {
            new AlertDialog.Builder(this)
                .setTitle("X11 server not found")
                .setMessage("Install a compatible Termux:X11 server and companion package first. A display server is not built into Android.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open X11 server.", Toast.LENGTH_LONG).show();
        }
    }

    private void showLog() {
        if (lastLog.isEmpty()) {
            lastLog = execRoot("cat /data/local/lucid_desktop/lucid-desktop.log 2>/dev/null || cat /data/local/tmp/lucid-desktop.log 2>/dev/null || echo 'No log found yet. Run Check requirements.'");
        }
        new AlertDialog.Builder(this)
            .setTitle("Desktop diagnostics")
            .setMessage(lastLog)
            .setPositiveButton("Copy", (d, w) -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Lucid Desktop log", lastLog));
                }
                Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
