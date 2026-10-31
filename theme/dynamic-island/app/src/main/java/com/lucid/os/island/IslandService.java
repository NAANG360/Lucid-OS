package com.lucid.os.island;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class IslandService extends Service {
    public static final String ACTION_TOGGLE = "com.lucid.os.island.TOGGLE";
    public static final String ACTION_STOP = "com.lucid.os.island.STOP";
    private static final int BG = 0xff050509, ACCENT = 0xffb9a4ff, MUTED = 0xffaaa5bd;
    private WindowManager wm;
    private View island;
    private LinearLayout content;
    private TextView headline, detail, mediaAction;
    private boolean expanded;
    private final Handler handler = new Handler();
    private MediaController activeMedia;
    private String notificationTitle = "", notificationText = "";
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            refreshState();
            if (island != null) handler.postDelayed(this, 1200);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            if (island == null) show();
            else { expanded = !expanded; render(); }
        }
        return START_STICKY;
    }

    private void show() {
        if (!Settings.canDrawOverlays(this)) { Toast.makeText(this, "Allow Lucid overlay access first", Toast.LENGTH_SHORT).show(); stopSelf(); return; }
        expanded = false;
        content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setBackground(pill());
        content.setElevation(dp(10));
        headline = text("●", ACCENT, 12);
        detail = text("", Color.WHITE, 11);
        mediaAction = text("Ⅱ", ACCENT, 13);
        content.addView(headline);
        content.addView(detail, new LinearLayout.LayoutParams(-2, -2));
        content.addView(mediaAction);
        island = content;

        int notchHeight = dp(28), notchWidth = dp(112);
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                WindowInsets wi = wm.getCurrentWindowMetrics().getWindowInsets();
                android.graphics.Insets cut = wi.getInsetsIgnoringVisibility(WindowInsets.Type.displayCutout());
                if (cut.top > 0) notchHeight = Math.max(dp(28), Math.min(dp(48), cut.top + dp(5)));
            } catch (Exception ignored) {}
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            notchWidth, notchHeight, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = 0;
        try {
            wm.addView(island, lp);
            island.setOnClickListener(v -> { expanded = !expanded; render(); });
            island.setOnLongClickListener(v -> { stopSelf(); return true; });
            handler.post(refresh);
            render();
        } catch (Exception e) { island = null; stopSelf(); }
    }

    private void refreshState() {
        notificationTitle = latestTitle;
        notificationText = latestText;
        try {
            MediaSessionManager msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> sessions = msm.getActiveSessions(new ComponentName(this, IslandNotificationListener.class));
            activeMedia = sessions.isEmpty() ? null : sessions.get(0);
        } catch (SecurityException ignored) { activeMedia = null; }
        if (island != null) render();
    }

    private void render() {
        if (island == null) return;
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) island.getLayoutParams();
        content.removeAllViews();
        content.setOrientation(expanded ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(expanded ? 17 : 13), dp(expanded ? 11 : 3), dp(expanded ? 17 : 13), dp(expanded ? 11 : 3));
        content.setBackground(pill());

        if (!expanded) {
            TextView dot = text("●", ACCENT, 11);
            content.addView(dot);
            String shortText = activeMedia != null ? "  ♪" : (!notificationTitle.isEmpty() ? "  •" : "");
            content.addView(text(shortText, Color.WHITE, 11));
            lp.width = dp(112);
            lp.height = dp(30);
        } else {
            String title = activeMedia != null ? mediaTitle() : (!notificationTitle.isEmpty() ? notificationTitle : "Lucid is ready");
            String sub = activeMedia != null ? "Now playing" : (!notificationText.isEmpty() ? notificationText : "Tap the island to collapse · hold to dismiss");
            content.addView(text("L U C I D   ·   LIVE", ACCENT, 9));
            TextView h = text(title, Color.WHITE, 13); h.setPadding(0, dp(7), 0, dp(2)); content.addView(h);
            TextView d = text(sub, MUTED, 10); content.addView(d);
            if (activeMedia != null) {
                TextView control = text(isPlaying() ? "Ⅱ   PAUSE" : "▶   PLAY", ACCENT, 11);
                control.setPadding(0, dp(9), 0, 0);
                control.setOnClickListener(v -> togglePlayback());
                content.addView(control);
            }
            lp.width = dp(258);
            lp.height = dp(activeMedia != null ? 112 : 94);
        }
        try { wm.updateViewLayout(island, lp); } catch (Exception ignored) {}
    }

    private String mediaTitle() {
        try { MediaMetadata m = activeMedia.getMetadata(); String s = m == null ? null : m.getString(MediaMetadata.METADATA_KEY_TITLE); return s == null ? "Media playing" : s; }
        catch (Exception e) { return "Media playing"; }
    }
    private boolean isPlaying() { try { return activeMedia != null && activeMedia.getPlaybackState() != null && activeMedia.getPlaybackState().getState() == android.media.session.PlaybackState.STATE_PLAYING; } catch (Exception e) { return false; } }
    private void togglePlayback() { if (activeMedia == null) return; try { if (isPlaying()) activeMedia.getTransportControls().pause(); else activeMedia.getTransportControls().play(); } catch (Exception ignored) {} handler.postDelayed(refresh, 250); }

    public static void publishNotification(String title, String text) {
        latestTitle = title == null ? "" : title;
        latestText = text == null ? "" : text;
    }
    private static volatile String latestTitle = "", latestText = "";

    private GradientDrawable pill() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(BG);
        d.setCornerRadius(dp(40));
        d.setStroke(dp(1), 0xff24212f);
        return d;
    }
    private TextView text(String value, int color, int size) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size); t.setGravity(Gravity.CENTER); t.setMaxLines(1); t.setEllipsize(android.text.TextUtils.TruncateAt.END); return t;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (island != null) { try { wm.removeView(island); } catch (Exception ignored) {} island = null; }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
