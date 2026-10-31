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
    private static final String PREFS = "lucid_launcher";
    private final int bg = Color.rgb(12, 13, 19);
    private final int panel = Color.rgb(25, 25, 37);
    private final int panelHigh = Color.rgb(36, 37, 52);
    private final int fg = Color.rgb(246, 244, 252);
    private final int muted = Color.rgb(166, 164, 187);
    private int accent = Color.rgb(196, 151, 255);
    private LinearLayout root, grid;
    private EditText search;
    private TextClock clock;
    private View glance;
    private final List<AppEntry> apps = new ArrayList<>();
    private int columns = 4;
    private int iconSize = 48;
    private boolean showLabels = true;
    private boolean showGlance = true;
    private boolean receiverRegistered;

    static final class AppEntry {
        final String label, pkg;
        final DrawableIcon icon;
        AppEntry(String l, String p, DrawableIcon i) { label=l; pkg=p; icon=i; }
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
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        loadPreferences();
        loadAppearance();
        loadApps();
        buildHome();
    }

    private void loadPreferences() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        columns = p.getInt("columns", 4);
        iconSize = p.getInt("icon_size", 48);
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
        if (root != null) {
            applyAccent();
            if (search != null) render(search.getText().toString());
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
        else super.onBackPressed();
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
        Collections.sort(apps, (a,b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(14), dp(20), dp(16));
        root.setBackgroundColor(bg);
        root.setOnLongClickListener(v -> { showWorkspaceMenu(); return true; });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("lucid", 29, fg, true);
        brand.setLetterSpacing(-0.045f);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView wallpaper = actionChip("✧", "Wallpapers");
        header.addView(wallpaper, new LinearLayout.LayoutParams(dp(44), dp(44)));
        wallpaper.setOnClickListener(v -> launchLucid("com.lucid.os.wallpapers"));
        TextView settings = actionChip("⚙", "Customize");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(44), dp(44));
        sp.leftMargin = dp(8);
        header.addView(settings, sp);
        settings.setOnClickListener(v -> showCustomization());
        root.addView(header);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(17), dp(15), dp(17), dp(15));
        hero.setBackground(shape(panel, 23));
        clock = new TextClock(this);
        clock.setTextColor(fg);
        clock.setTextSize(36);
        clock.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        hero.addView(clock);
        TextView greeting = text("A little more room to think.", 13, muted, false);
        greeting.setPadding(0, dp(2), 0, dp(13));
        hero.addView(greeting);
        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setGravity(Gravity.CENTER_VERTICAL);
        shortcuts.addView(quickPill("Control Centre", () -> launchLucid("com.lucid.os.controlcentre")));
        shortcuts.addView(quickPill("Desktop", () -> launchLucid("com.lucid.os.desktop")));
        shortcuts.addView(quickPill("Settings", () -> openLucidSettings()));
        hero.addView(shortcuts);
        glance = hero;
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(10);
        hp.bottomMargin = dp(14);
        root.addView(hero, hp);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(fg);
        search.setHintTextColor(muted);
        search.setHint("⌕   Search apps");
        search.setTextSize(15);
        search.setPadding(dp(15), dp(10), dp(15), dp(10));
        search.setBackground(shape(panel, 17));
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(false);
        scroller.setClipToPadding(false);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        scroller.addView(grid);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, 0, 1f);
        gp.topMargin = dp(12);
        root.addView(scroller, gp);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { render(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        applyAccent();
        setContentView(root);
        LucidMotion.apply(this);
        render("");
        applyPreferencesToViews();
    }

    private void applyAccent() {
        if (root == null) return;
        // Keep the Lucid palette coherent; the shared appearance provider supplies the accent.
        root.setBackgroundColor(bg);
    }

    private void applyPreferencesToViews() {
        if (glance != null) glance.setVisibility(showGlance ? View.VISIBLE : View.GONE);
        render(search == null ? "" : search.getText().toString());
    }

    private TextView actionChip(String glyph, String description) {
        TextView v = text(glyph, 20, accent, true);
        v.setGravity(Gravity.CENTER);
        v.setContentDescription(description);
        v.setBackground(shape(panel, 22));
        return v;
    }

    private View quickPill(String label, Runnable action) {
        TextView v = text(label, 11, accent, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(11), dp(8), dp(11), dp(8));
        v.setBackground(shape(panelHigh, 15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.rightMargin = dp(7);
        v.setLayoutParams(lp);
        v.setOnClickListener(view -> action.run());
        return v;
    }

    private void render(String query) {
        if (grid == null) return;
        grid.removeAllViews();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        ArrayList<AppEntry> visible = new ArrayList<>();
        for (AppEntry a : apps) if (a.label.toLowerCase(Locale.ROOT).contains(q)) visible.add(a);
        for (int start=0; start<visible.size(); start+=columns) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.TOP);
            int end = Math.min(start+columns, visible.size());
            for (int j=start; j<end; j++) row.addView(appButton(visible.get(j)),
                new LinearLayout.LayoutParams(0, dp(showLabels ? 98 : iconSize+24), 1f));
            for (int j=end-start; j<columns; j++) row.addView(new View(this),
                new LinearLayout.LayoutParams(0, dp(showLabels ? 98 : iconSize+24), 1f));
            grid.addView(row);
        }
        if (visible.isEmpty()) {
            TextView empty = text(q.isEmpty() ? "No launchable apps found." : "No apps match “"+query+"”.", 14, muted, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            grid.addView(empty);
        }
    }

    private View appButton(final AppEntry a) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(2), dp(6), dp(2), dp(6));
        box.setOnLongClickListener(v -> { showAppMenu(a); return true; });
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try { icon.setImageDrawable(a.icon.get()); } catch (Exception ignored) {}
        box.addView(icon, new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
        if (showLabels) {
            TextView label = text(a.label, 11, fg, false);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(5);
            box.addView(label, lp);
        }
        box.setOnClickListener(v -> openApp(a));
        box.setOnCreateContextMenuListener((menu, v, info) -> {
            menu.setHeaderTitle(a.label);
            menu.add("Open").setOnMenuItemClickListener(item -> { openApp(a); return true; });
            menu.add("App info").setOnMenuItemClickListener(item -> { openAppInfo(a); return true; });
        });
        return box;
    }

    private void openApp(AppEntry a) {
        Intent i = getPackageManager().getLaunchIntentForPackage(a.pkg);
        if (i == null) { toast("This app has no launch activity."); return; }
        try { startActivity(i); } catch (Exception e) { toast("Couldn't open " + a.label); }
    }

    private void showAppMenu(AppEntry a) {
        String[] actions = {"Open", "App info", "Uninstall", "Share app"};
        new AlertDialog.Builder(this).setTitle(a.label).setItems(actions, (d, which) -> {
            if (which == 0) openApp(a);
            else if (which == 1) openAppInfo(a);
            else if (which == 2) {
                try { startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:"+a.pkg))); }
                catch (Exception e) { toast("Uninstall isn't available for this app."); }
            } else {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, "Check out " + a.label + " (package: " + a.pkg + ")");
                try { startActivity(Intent.createChooser(share, "Share app")); }
                catch (Exception e) { toast("Sharing isn't available."); }
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void openAppInfo(AppEntry a) {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+a.pkg))); }
        catch (Exception e) { toast("App info isn't available."); }
    }

    private void showWorkspaceMenu() {
        new AlertDialog.Builder(this).setTitle("Home screen")
            .setItems(new String[]{"Launcher customizations", "Wallpaper", "Lucid Settings", "Refresh apps"}, (d,w) -> {
                if (w==0) showCustomization();
                else if (w==1) launchLucid("com.lucid.os.wallpapers");
                else if (w==2) openLucidSettings();
                else { loadApps(); render(search.getText().toString()); }
            }).show();
    }

    private void showCustomization() {
        String[] choices = {"Home grid: "+columns+" columns", "Icon size: "+iconSize+" dp",
            (showLabels ? "Hide" : "Show")+" app labels", (showGlance ? "Hide" : "Show")+" clock & glance",
            "Reset launcher layout"};
        new AlertDialog.Builder(this).setTitle("Lucid Launcher").setItems(choices, (d,w) -> {
            if (w==0) {
                String[] values={"3 columns","4 columns","5 columns","6 columns"};
                new AlertDialog.Builder(this).setTitle("Home grid").setSingleChoiceItems(values, columns-3, (dialog,which) -> {
                    columns=which+3; savePreferences(); render(search.getText().toString()); dialog.dismiss();
                }).setNegativeButton("Cancel",null).show();
            } else if (w==1) {
                String[] values={"Small · 40 dp","Default · 48 dp","Large · 56 dp","Extra large · 64 dp"};
                int selected=iconSize==40?0:iconSize==56?2:iconSize==64?3:1;
                new AlertDialog.Builder(this).setTitle("Icon size").setSingleChoiceItems(values,selected,(dialog,which)->{
                    iconSize=new int[]{40,48,56,64}[which]; savePreferences(); render(search.getText().toString()); dialog.dismiss();
                }).setNegativeButton("Cancel",null).show();
            } else if (w==2) { showLabels=!showLabels; savePreferences(); render(search.getText().toString()); }
            else if (w==3) { showGlance=!showGlance; savePreferences(); applyPreferencesToViews(); }
            else {
                columns=4; iconSize=48; showLabels=true; showGlance=true;
                savePreferences(); applyPreferencesToViews();
            }
        }).setNegativeButton("Close",null).show();
    }

    private void savePreferences() {
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt("columns",columns)
            .putInt("icon_size",iconSize).putBoolean("labels",showLabels)
            .putBoolean("glance",showGlance).apply();
    }

    private void openLucidSettings() {
        Intent i = getPackageManager().getLaunchIntentForPackage("com.lucid.os.settings");
        if (i == null) {
            i = new Intent(Intent.ACTION_MAIN);
            i.setClassName("com.lucid.os.settings", "com.lucid.os.settings.MainActivity");
        }
        try { startActivity(i); }
        catch (Exception e) { toast("Lucid Settings isn't installed in this build."); }
    }

    private void launchLucid(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) { toast("This Lucid component isn't installed in this build."); return; }
        try { startActivity(i); } catch (Exception e) { toast("Couldn't open this Lucid component."); }
    }

    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }

    private TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL)); return t;
    }
    private GradientDrawable shape(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
}
