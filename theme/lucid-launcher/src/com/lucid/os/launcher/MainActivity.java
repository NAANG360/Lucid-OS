package com.lucid.os.launcher;

import com.lucid.motion.LucidMotion;
import com.lucid.ui.LucidUi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

/**
 * Lucid Launcher. Wallpaper-first home: the user's wallpaper is the canvas, surfaces are the
 * white-25%/50% glass from the cosmosOS Homescreen frame, the dock carries the Lucid apps.
 */
public final class MainActivity extends Activity {
    private static final String PREFS = "lucid_launcher";
    private int accent = LucidUi.BLUE;
    private LinearLayout page, grid;
    private View hero;
    private EditText search;
    private final List<AppEntry> apps = new ArrayList<>();
    private final List<AppEntry> visible = new ArrayList<>();
    private int columns = 4;
    private int iconSize = 52;
    private boolean showLabels = true;
    private boolean showGlance = true;
    private boolean receiverRegistered;

    static final class AppEntry {
        final String label, pkg;
        final DrawableIcon icon;
        AppEntry(String l, String p, DrawableIcon i) { label = l; pkg = p; icon = i; }
    }
    interface DrawableIcon { Drawable get(); }

    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            loadApps();
            if (search != null) render(search.getText().toString());
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        loadPreferences();
        loadAppearance();
        loadApps();
        buildHome();
    }

    private void loadPreferences() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        columns = p.getInt("columns", 4);
        iconSize = p.getInt("icon_size", 52);
        showLabels = p.getBoolean("labels", true);
        showGlance = p.getBoolean("glance", true);
    }

    @Override protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_PACKAGE_ADDED);
            f.addAction(Intent.ACTION_PACKAGE_REMOVED);
            f.addAction(Intent.ACTION_PACKAGE_CHANGED);
            f.addDataScheme("package");
            registerReceiver(pkgReceiver, f);
            receiverRegistered = true;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        loadPreferences();
        loadAppearance();
        loadApps();
        if (page != null) {
            hero.setVisibility(showGlance ? View.VISIBLE : View.GONE);
            render(search.getText().toString());
        }
    }

    @Override protected void onStop() {
        if (receiverRegistered) {
            try { unregisterReceiver(pkgReceiver); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override @SuppressWarnings("deprecation") public void onBackPressed() {
        if (search != null && search.length() > 0) search.setText("");
        else if (search != null && search.hasFocus()) search.clearFocus();
        // A launcher never finishes itself on Back.
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already on Home: reset search and scroll back to the top.
        if (search != null) { search.setText(""); search.clearFocus(); }
    }

    private void loadAppearance() {
        Cursor c = null;
        try {
            c = getContentResolver().query(Uri.parse("content://com.lucid.os.settings.appearance/profile"),
                new String[]{"accent"}, null, null, null);
            if (c != null && c.moveToFirst()) accent = c.getInt(0);
        } catch (Exception ignored) {
        } finally { if (c != null) c.close(); }
    }

    private void loadApps() {
        apps.clear();
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        for (ResolveInfo r : pm.queryIntentActivities(q, 0)) {
            final ResolveInfo info = r;
            String pkg = info.activityInfo.packageName;
            if (getPackageName().equals(pkg)) continue;
            apps.add(new AppEntry(info.loadLabel(pm).toString(), pkg, () -> info.loadIcon(pm)));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void buildHome() {
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setOnLongClickListener(v -> { showWorkspaceMenu(); return true; });

        // Clock block + customise chip
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.TOP);
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        hero = block;
        TextClock clock = new TextClock(this);
        clock.setTextColor(LucidUi.ON_WALLPAPER);
        clock.setTextSize(58);
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        clock.setLetterSpacing(-0.045f);
        clock.setIncludeFontPadding(false);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        clock.setShadowLayer(dp(5), 0, dp(1), 0x55000000);
        block.addView(clock);
        TextClock date = new TextClock(this);
        date.setTextColor(LucidUi.ON_WALLPAPER_SOFT);
        date.setTextSize(15);
        date.setIncludeFontPadding(false);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setShadowLayer(dp(3), 0, dp(1), 0x66000000);
        date.setPadding(dp(3), dp(4), 0, 0);
        block.addView(date);
        header.addView(block, new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(roundButton(LucidUi.Glyph.TUNE, "Customise launcher", () -> showCustomization()),
            new LinearLayout.LayoutParams(dp(44), dp(44)));
        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // Search pill
        LinearLayout pill = new LinearLayout(this);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(16), 0, dp(16), 0);
        pill.setBackground(LucidUi.glass(this, LucidUi.GLASS_LOW, 26, true));
        ImageView mag = new ImageView(this);
        mag.setImageDrawable(new LucidUi.Glyph(LucidUi.Glyph.SEARCH, LucidUi.ON_WALLPAPER_SOFT));
        mag.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        pill.addView(mag, new LinearLayout.LayoutParams(dp(20), dp(20)));
        search = new EditText(this);
        search.setSingleLine(true);
        search.setBackground(null);
        search.setTextColor(LucidUi.ON_WALLPAPER);
        search.setHintTextColor(0xB3FFFFFF);
        search.setHint("Search apps");
        search.setTextSize(16);
        search.setPadding(dp(12), 0, 0, 0);
        search.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.setContentDescription("Search apps");
        search.setOnEditorActionListener((v, actionId, e) -> {
            if (!visible.isEmpty()) openApp(visible.get(0));
            return true;
        });
        pill.addView(search, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(52));
        pp.topMargin = dp(18);
        page.addView(pill, pp);

        // App grid
        ScrollView scroller = new ScrollView(this);
        scroller.setClipToPadding(false);
        scroller.setVerticalScrollBarEnabled(false);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(grid);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, 0, 1f);
        gp.topMargin = dp(14);
        page.addView(scroller, gp);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { render(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });

        // Dock
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(10), dp(10), dp(10), dp(10));
        dock.setBackground(LucidUi.glass(this, LucidUi.GLASS_LOW, 30, true));
        dock.addView(dockButton(LucidUi.Glyph.SETTINGS, "Lucid Settings", () -> openLucidSettings()));
        dock.addView(dockButton(LucidUi.Glyph.WALLPAPER, "Wallpapers", () -> launchLucid("com.lucid.os.wallpaper")));
        dock.addView(dockButton(LucidUi.Glyph.CONTROL, "Control Centre", () -> launchLucid("com.lucid.os.controlcentre")));
        dock.addView(dockButton(LucidUi.Glyph.UPDATE, "System Updater", () -> launchLucid("com.lucid.os.updater")));
        LinearLayout.LayoutParams dp0 = new LinearLayout.LayoutParams(-1, -2);
        dp0.topMargin = dp(10);
        page.addView(dock, dp0);

        setContentView(page);
        LucidUi.edgeToEdge(this, page, dp(20), dp(12), dp(20), dp(14));
        hero.setVisibility(showGlance ? View.VISIBLE : View.GONE);
        LucidMotion.apply(this);
        render("");
    }

    private View roundButton(int glyph, String description, final Runnable action) {
        ImageView v = new ImageView(this);
        v.setImageDrawable(new LucidUi.Glyph(glyph, LucidUi.ON_WALLPAPER));
        v.setPadding(dp(11), dp(11), dp(11), dp(11));
        v.setBackground(LucidUi.glass(this, LucidUi.GLASS_LOW, 22, true));
        v.setContentDescription(description);
        v.setClickable(true);
        v.setOnClickListener(x -> action.run());
        return v;
    }

    private View dockButton(int glyph, String description, final Runnable action) {
        ImageView v = new ImageView(this);
        v.setImageDrawable(new LucidUi.Glyph(glyph, LucidUi.ON_WALLPAPER));
        v.setPadding(dp(14), dp(14), dp(14), dp(14));
        v.setBackground(LucidUi.glass(this, 0x26FFFFFF, 18, false));
        v.setContentDescription(description);
        v.setClickable(true);
        v.setOnClickListener(x -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1f);
        lp.leftMargin = dp(5);
        lp.rightMargin = dp(5);
        v.setLayoutParams(lp);
        return v;
    }

    private void render(String query) {
        if (grid == null) return;
        grid.removeAllViews();
        visible.clear();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        for (AppEntry a : apps) if (a.label.toLowerCase(Locale.ROOT).contains(q)) visible.add(a);
        int rowH = dp(showLabels ? iconSize + 46 : iconSize + 24);
        for (int start = 0; start < visible.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            int end = Math.min(start + columns, visible.size());
            for (int j = start; j < end; j++) row.addView(appButton(visible.get(j)), new LinearLayout.LayoutParams(0, rowH, 1f));
            for (int j = end - start; j < columns; j++) row.addView(new View(this), new LinearLayout.LayoutParams(0, rowH, 1f));
            grid.addView(row);
        }
        if (visible.isEmpty()) {
            TextView empty = LucidUi.text(this, q.isEmpty() ? "No launchable apps found." : "No apps match \u201C" + query + "\u201D.",
                14, LucidUi.ON_WALLPAPER_SOFT, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(32), 0, dp(32));
            LucidUi.shadow(this, empty);
            grid.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private View appButton(final AppEntry a) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(2), dp(8), dp(2), dp(4));
        box.setClickable(true);
        box.setContentDescription(a.label);
        box.setOnLongClickListener(v -> { showAppMenu(a); return true; });
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try { icon.setImageDrawable(a.icon.get()); } catch (Exception ignored) {}
        box.addView(icon, new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
        if (showLabels) {
            TextView label = LucidUi.text(this, a.label, 11.5f, LucidUi.ON_WALLPAPER, false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LucidUi.shadow(this, label);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(6);
            box.addView(label, lp);
        }
        box.setOnClickListener(v -> openApp(a));
        return box;
    }

    private void openApp(AppEntry a) {
        Intent i = getPackageManager().getLaunchIntentForPackage(a.pkg);
        if (i == null) { toast("This app has no launch activity."); return; }
        try { startActivity(i); } catch (Exception e) { toast("Couldn\u2019t open " + a.label); }
    }

    private void showAppMenu(final AppEntry a) {
        LucidUi.sheet(this, a.label, new String[]{"Open", "App info", "Uninstall", "Share app"}, -1, which -> {
            if (which == 0) openApp(a);
            else if (which == 1) openAppInfo(a);
            else if (which == 2) {
                try { startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg))); }
                catch (Exception e) { toast("Uninstall isn\u2019t available for this app."); }
            } else {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "Check out " + a.label + " (package: " + a.pkg + ")");
                try { startActivity(Intent.createChooser(share, "Share app")); }
                catch (Exception e) { toast("Sharing isn\u2019t available."); }
            }
        });
    }

    private void openAppInfo(AppEntry a) {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.pkg))); }
        catch (Exception e) { toast("App info isn\u2019t available."); }
    }

    private void showWorkspaceMenu() {
        LucidUi.sheet(this, "Home screen", new String[]{"Launcher layout", "Wallpapers", "Lucid Settings",
            "Lucid Desktop", "About Lucid", "Refresh apps"}, -1, w -> {
            if (w == 0) showCustomization();
            else if (w == 1) launchLucid("com.lucid.os.wallpaper");
            else if (w == 2) openLucidSettings();
            else if (w == 3) launchLucid("com.lucid.os.desktop");
            else if (w == 4) launchLucid("com.lucid.os.about");
            else { loadApps(); render(search.getText().toString()); }
        });
    }

    private void showCustomization() {
        String[] choices = {"Home grid \u00B7 " + columns + " columns", "Icon size \u00B7 " + iconSize + " dp",
            (showLabels ? "Hide" : "Show") + " app labels", (showGlance ? "Hide" : "Show") + " clock",
            "Reset launcher layout"};
        LucidUi.sheet(this, "Launcher layout", choices, -1, w -> {
            if (w == 0) {
                LucidUi.sheet(this, "Home grid", new String[]{"3 columns", "4 columns", "5 columns", "6 columns"}, columns - 3, which -> {
                    columns = which + 3; savePreferences(); render(search.getText().toString());
                });
            } else if (w == 1) {
                int selected = iconSize == 44 ? 0 : iconSize == 60 ? 2 : iconSize == 68 ? 3 : 1;
                LucidUi.sheet(this, "Icon size", new String[]{"Small", "Default", "Large", "Extra large"}, selected, which -> {
                    iconSize = new int[]{44, 52, 60, 68}[which]; savePreferences(); render(search.getText().toString());
                });
            } else if (w == 2) { showLabels = !showLabels; savePreferences(); render(search.getText().toString()); }
            else if (w == 3) { showGlance = !showGlance; savePreferences(); hero.setVisibility(showGlance ? View.VISIBLE : View.GONE); }
            else {
                columns = 4; iconSize = 52; showLabels = true; showGlance = true;
                savePreferences(); hero.setVisibility(View.VISIBLE); render(search.getText().toString());
            }
        });
    }

    private void savePreferences() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("columns", columns)
            .putInt("icon_size", iconSize).putBoolean("labels", showLabels)
            .putBoolean("glance", showGlance).apply();
    }

    private void openLucidSettings() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.lucid.os.settings");
        if (i == null) {
            i = new Intent(Intent.ACTION_MAIN);
            i.setClassName("com.lucid.os.settings", "com.lucid.os.settings.MainActivity");
        }
        try { startActivity(i); }
        catch (Exception e) { toast("Lucid Settings isn\u2019t installed in this build."); }
    }

    private void launchLucid(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { toast("This Lucid component isn\u2019t installed in this build."); return; }
        try { startActivity(i); } catch (Exception e) { toast("Couldn\u2019t open this Lucid component."); }
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private int dp(int n) { return LucidUi.dp(this, n); }
}
