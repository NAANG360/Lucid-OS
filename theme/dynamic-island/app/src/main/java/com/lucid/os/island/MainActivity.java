package com.lucid.os.island;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(26), dp(38), dp(26), dp(26));
        root.setBackgroundColor(0xff09090f);
        TextView eyebrow = label("L U C I D   /   SYSTEM EXPERIENCE", 10, 0xffb9a4ff);
        root.addView(eyebrow);
        TextView title = label("Dynamic Island", 29, 0xffffffff);
        title.setTypeface(null, 1); title.setPadding(0, dp(12), 0, dp(6)); root.addView(title);
        TextView copy = label("A living extension of the camera cutout. Media and notifications appear inside the notch instead of sitting underneath it.", 14, 0xffaaa5bd);
        copy.setPadding(0, 0, 0, dp(24)); root.addView(copy);
        status = label("", 12, 0xffb9a4ff); root.addView(status);
        add(root, "Allow overlay", v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        add(root, "Enable notification access", v -> startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        add(root, "Start / expand island", v -> {
            if (!Settings.canDrawOverlays(this)) { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); return; }
            startService(new Intent(this, IslandService.class).setAction(IslandService.ACTION_TOGGLE));
        });
        add(root, "Stop island", v -> stopService(new Intent(this, IslandService.class)));
        TextView hint = label("Tap the notch to expand · tap again to collapse · long-press to dismiss. Media controls appear when a supported player is active. Notification access is optional.", 12, 0xff777286);
        hint.setPadding(0, dp(22), 0, 0); root.addView(hint);
        setContentView(root);
    }
    @Override protected void onResume() { super.onResume(); if (status != null) status.setText((Settings.canDrawOverlays(this) ? "● Overlay ready" : "○ Overlay permission needed") + "     " + (isNotificationAccessEnabled() ? "● Notifications ready" : "○ Notification access optional")); }
    private boolean isNotificationAccessEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }
    private void add(LinearLayout root, String text, android.view.View.OnClickListener click) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(0xffffffff); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff292334));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.topMargin = dp(10); root.addView(b, p); b.setOnClickListener(click);
    }
    private TextView label(String s, int size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
}
