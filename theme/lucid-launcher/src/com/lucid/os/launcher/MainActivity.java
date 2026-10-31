package com.lucid.os.launcher;

import com.lucid.motion.LucidMotion;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private final int bg = Color.rgb(12, 13, 19);
    private final int panel = Color.rgb(25, 25, 37);
    private final int panelHigh = Color.rgb(36, 37, 52);
    private final int fg = Color.rgb(246, 244, 252);
    private final int muted = Color.rgb(166, 164, 187);
    private int accent = Color.rgb(196, 151, 255);

    private LinearLayout root, grid, dock;
    private EditText search;
    private final List<AppEntry> apps = new ArrayList<>();
    private int columns = 4;
    private boolean receiverRegistered = false;

    static final class AppEntry {
        String label, pkg;
        DrawableIcon icon;
        AppEntry(String l, String p, DrawableIcon i) {
            label = l; pkg = p; icon = i;
        }
    }

    interface DrawableIcon {
        Drawable get();
    }

    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
            if (search != null) {
                render(search.getText().toString());
            }
        }
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        loadAppearance();
        loadApps();
        build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            registerReceiver(pkgReceiver, filter);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppearance();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(pkgReceiver);
            } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (search != null && search.getText() != null && search.getText().length() > 0) {
            search.setText("");
        }
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

    private void loadApps() {
        apps.clear();
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        for (ResolveInfo r : pm.queryIntentActivities(q, 0)) {
            final ResolveInfo info = r;
            String p = info.activityInfo.packageName;
            if (getPackageName().equals(p)) continue;
            apps.add(new AppEntry(info.loadLabel(pm).toString(), p, () -> info.loadIcon(pm)));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(14), dp(20), dp(12));
        root.setBackgroundColor(bg);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("lucid", 28, fg, true);
        brand.setLetterSpacing(-0.04f);
        head.addView(brand, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView wallBtn = text("✧", 18, accent, true);
        wallBtn.setGravity(Gravity.CENTER);
        wallBtn.setBackground(shape(panel, 24));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(dp(42), dp(42));
        wp.rightMargin = dp(8);
        head.addView(wallBtn, wp);
        wallBtn.setOnClickListener(v -> launch("com.lucid.os.wallpaper"));

        TextView settings = text("⚙", 20, accent, true);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(shape(panel, 24));
        head.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));
        settings.setOnClickListener(v -> showLauncherSettings());
        root.addView(head);

        LinearLayout glance = new LinearLayout(this);
        glance.setOrientation(LinearLayout.VERTICAL);
        glance.setPadding(dp(16), dp(14), dp(16), dp(14));
        glance.setBackground(shape(panel, 22));
        TextClock clock = new TextClock(this);
        clock.setTextColor(fg);
        clock.setTextSize(34);
        clock.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        glance.addView(clock);
        TextView greeting = text("A little more room to think.", 13, muted, false);
        greeting.setPadding(0, dp(2), 0, dp(10));
        glance.addView(greeting);

        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.addView(quickPill("Control Centre", () -> launch("com.lucid.os.controlcentre")));
        pills.addView(quickPill("Wallpapers", () -> launch("com.lucid.os.wallpaper")));
        pills.addView(quickPill("Desktop", () -> launch("com.lucid.os.desktop")));
        glance.addView(pills);

        LinearLayout.LayoutParams glanceParams = new LinearLayout.LayoutParams(-1, -2);
        glanceParams.topMargin = dp(8);
        glanceParams.bottomMargin = dp(12);
        root.addView(glance, glanceParams);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(fg);
        search.setHintTextColor(muted);
        search.setHint("⌕   Search apps");
        search.setTextSize(15);
        search.setPadding(dp(15), dp(10), dp(15), dp(10));
        search.setBackground(shape(panel, 17));
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(46)));

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(false);
        sc.addView(grid);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, 0, 1f);
        gp.topMargin = dp(12);
        root.addView(sc, gp);

        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(7), dp(8), dp(7), dp(8));
        dock.setBackground(shape(panel, 24));
        root.addView(dock, new LinearLayout.LayoutParams(-1, -2));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                render(s.toString());
            }
            public void afterTextChanged(Editable e) {}
        });

        setContentView(root);
        LucidMotion.apply(this);
        render("");
        renderDock();
    }

    private View quickPill(String labelText, final Runnable r) {
        TextView p = text(labelText, 11, accent, true);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(10), dp(7), dp(10), dp(7));
        p.setBackground(shape(panelHigh, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.rightMargin = dp(6);
        p.setLayoutParams(lp);
        p.setOnClickListener(v -> r.run());
        return p;
    }

    private void render(String query) {
        grid.removeAllViews();
        String q = query.toLowerCase(Locale.ROOT);
        ArrayList<AppEntry> visible = new ArrayList<>();
        for (AppEntry a : apps) {
            if (a.label.toLowerCase(Locale.ROOT).contains(q)) visible.add(a);
        }
        for (int start = 0; start < visible.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            for (int j = start; j < Math.min(start + columns, visible.size()); j++) {
                AppEntry a = visible.get(j);
                row.addView(appButton(a), new LinearLayout.LayoutParams(0, dp(94), 1f));
            }
            for (int j = Math.min(columns, visible.size() - start); j < columns; j++) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(94), 1f));
            }
            grid.addView(row);
        }
        if (visible.isEmpty()) {
            grid.addView(text("No apps found.", 14, muted, false));
        }
    }

    private View appButton(final AppEntry a) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(2), dp(5), dp(2), dp(5));
        ImageView icon = new ImageView(this);
        try {
            icon.setImageDrawable(a.icon.get());
        } catch (Exception ignored) {}
        box.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView label = text(a.label, 11, fg, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        box.addView(label, lp);
        box.setOnClickListener(v -> {
            Intent i = getPackageManager().getLaunchIntentForPackage(a.pkg);
            if (i != null) {
                try {
                    startActivity(i);
                } catch (Exception ignored) {}
            }
        });
        box.setOnLongClickListener(v -> {
            String[] opts = {"App info", "Uninstall (if allowed)", "Cancel"};
            new AlertDialog.Builder(this)
                .setTitle(a.label)
                .setItems(opts, (d, which) -> {
                    if (which == 0) {
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg));
                        startActivity(i);
                    } else if (which == 1) {
                        try {
                            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg)));
                        } catch (Exception ignored) {}
                    }
                })
                .show();
            return true;
        });
        return box;
    }

    private void renderDock() {
        dock.removeAllViews();
        String[] icons = {"☎", "✉", "◉", "◈", "⚙"};
        for (int i = 0; i < icons.length; i++) {
            final int ix = i;
            TextView t = text(icons[i], 20, accent, true);
            t.setGravity(Gravity.CENTER);
            dock.addView(t, new LinearLayout.LayoutParams(0, dp(45), 1f));
            t.setOnClickListener(v -> openDockAction(ix));
        }
    }

    private void openDockAction(int ix) {
        try {
            if (ix == 0) {
                startActivity(new Intent(Intent.ACTION_DIAL));
            } else if (ix == 1) {
                Intent sms = new Intent(Intent.ACTION_MAIN);
                sms.addCategory(Intent.CATEGORY_APP_MESSAGING);
                startActivity(sms);
            } else if (ix == 2) {
                startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
            } else if (ix == 3) {
                launch("com.lucid.os.controlcentre");
            } else {
                Intent in = getPackageManager().getLaunchIntentForPackage("com.lucid.os.settings");
                if (in != null) startActivity(in);
                else startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        } catch (Exception e) {
            Toast.makeText(this, "No default handler available on this build.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLauncherSettings() {
        String[] opts = {
            "Grid: 4 columns",
            "Grid: 5 columns",
            "Open Lucid Settings",
            "Lucid Wallpapers",
            "Lucid Control Centre",
            "Lucid Desktop",
            "Lucid Updater",
            "About Lucid OS",
            "Refresh app list"
        };
        new AlertDialog.Builder(this)
            .setTitle("Lucid Launcher")
            .setItems(opts, (d, w) -> {
                if (w == 0 || w == 1) {
                    columns = w == 0 ? 4 : 5;
                    render(search.getText().toString());
                } else if (w == 2) launch("com.lucid.os.settings");
                else if (w == 3) launch("com.lucid.os.wallpaper");
                else if (w == 4) launch("com.lucid.os.controlcentre");
                else if (w == 5) launch("com.lucid.os.desktop");
                else if (w == 6) launch("com.lucid.os.updater");
                else if (w == 7) launch("com.lucid.os.about");
                else {
                    loadApps();
                    render(search.getText().toString());
                }
            })
            .show();
    }

    private void launch(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            startActivity(i);
        } else {
            Toast.makeText(this, "Lucid component not installed yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable shape(int c, int r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(c);
        d.setCornerRadius(dp(r));
        return d;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
