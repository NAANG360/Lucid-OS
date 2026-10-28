package com.lucid.os.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.*;

public final class MainActivity extends Activity {
    private final int bg=Color.rgb(12,13,19),panel=Color.rgb(25,25,37),fg=Color.rgb(246,244,252),muted=Color.rgb(166,164,187); private int accent=Color.rgb(196,151,255);
    private LinearLayout root,grid,dock;private EditText search;private final List<AppEntry> apps=new ArrayList<>();private int columns=4;
    static final class AppEntry{String label,pkg;DrawableIcon icon;AppEntry(String l,String p,DrawableIcon i){label=l;pkg=p;icon=i;}}
    interface DrawableIcon{android.graphics.drawable.Drawable get();}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);loadAppearance();loadApps();build();}
    private void loadAppearance(){
        android.database.Cursor cursor=null;
        try{cursor=getContentResolver().query(android.net.Uri.parse("content://com.lucid.os.settings.appearance/profile"),new String[]{"accent"},null,null,null);if(cursor!=null&&cursor.moveToFirst())accent=cursor.getInt(0);}
        catch(Exception ignored){}finally{if(cursor!=null)cursor.close();}
    }
    private void loadApps(){apps.clear();Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);PackageManager pm=getPackageManager();for(ResolveInfo r:pm.queryIntentActivities(q,0)){String p=r.activityInfo.packageName;apps.add(new AppEntry(r.loadLabel(pm).toString(),p,()->r.loadIcon(pm)));}Collections.sort(apps,(a,b)->a.label.compareToIgnoreCase(b.label));}
    private void build(){
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(16),dp(20),dp(12));root.setBackgroundColor(bg);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("lucid",30,fg,true);brand.setLetterSpacing(-.04f);head.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView settings=text("⚙",23,accent,true);settings.setGravity(Gravity.CENTER);settings.setBackground(shape(panel,30));head.addView(settings,new LinearLayout.LayoutParams(dp(46),dp(46)));settings.setOnClickListener(v->showLauncherSettings());root.addView(head);
        TextView greeting=text("A little more room to think.",17,muted,false);greeting.setPadding(0,dp(5),0,dp(14));root.addView(greeting);
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(fg);search.setHintTextColor(muted);search.setHint("⌕   Search apps");search.setTextSize(15);search.setPadding(dp(15),dp(10),dp(15),dp(10));search.setBackground(shape(panel,17));root.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
        grid=new LinearLayout(this);grid.setOrientation(1);ScrollView sc=new ScrollView(this);sc.setFillViewport(false);sc.addView(grid);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,0,1);gp.topMargin=dp(12);root.addView(sc,gp);
        dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);dock.setPadding(dp(7),dp(8),dp(7),dp(8));dock.setBackground(shape(panel,24));root.addView(dock,new LinearLayout.LayoutParams(-1,-2));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){render(s.toString());}public void afterTextChanged(Editable e){}});
        setContentView(root);render("");renderDock();
    }
    private void render(String query){
        grid.removeAllViews();String q=query.toLowerCase(Locale.ROOT);ArrayList<AppEntry> visible=new ArrayList<>();for(AppEntry a:apps)if(a.label.toLowerCase(Locale.ROOT).contains(q))visible.add(a);
        for(int start=0;start<visible.size();start+=columns){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.TOP);for(int j=start;j<Math.min(start+columns,visible.size());j++){AppEntry a=visible.get(j);row.addView(appButton(a),new LinearLayout.LayoutParams(0,dp(94),1));}for(int j=Math.min(columns,visible.size()-start);j<columns;j++)row.addView(new View(this),new LinearLayout.LayoutParams(0,dp(94),1));grid.addView(row);}
        if(visible.isEmpty())grid.addView(text("No apps found.",14,muted,false));
    }
    private View appButton(AppEntry a){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setGravity(Gravity.CENTER);box.setPadding(dp(2),dp(5),dp(2),dp(5));
        ImageView icon=new ImageView(this);try{icon.setImageDrawable(a.icon.get());}catch(Exception ignored){}box.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
        TextView label=text(a.label,11,fg,false);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(4);box.addView(label,lp);
        box.setOnClickListener(v->{Intent i=getPackageManager().getLaunchIntentForPackage(a.pkg);if(i!=null)try{startActivity(i);}catch(Exception ignored){}});
        box.setOnLongClickListener(v->{String[] opts={"App info","Uninstall (if allowed)","Cancel"};new AlertDialog.Builder(this).setTitle(a.label).setItems(opts,(d,which)->{if(which==0){Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+a.pkg));startActivity(i);}else if(which==1){try{startActivity(new Intent(Intent.ACTION_DELETE,android.net.Uri.parse("package:"+a.pkg)));}catch(Exception ignored){}}}).show();return true;});return box;
    }
    private void renderDock(){
        dock.removeAllViews();String[] names={"Phone","Messages","Camera","Settings"};String[] pkgs={"com.android.dialer","com.android.messaging","com.android.camera","com.android.settings"};
        for(int i=0;i<names.length;i++){final String pkg=pkgs[i];TextView t=text(new String[]{"☎","✉","◉","⚙"}[i],20,accent,true);t.setGravity(Gravity.CENTER);dock.addView(t,new LinearLayout.LayoutParams(0,dp(45),1));t.setOnClickListener(v->{Intent in=getPackageManager().getLaunchIntentForPackage(pkg);if(in!=null)startActivity(in);else if(iSafe(pkg))startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));else Toast.makeText(this,"App not installed on this ROM.",Toast.LENGTH_SHORT).show();});}
    }
    private boolean iSafe(String p){return "com.android.settings".equals(p);}
    private void showLauncherSettings(){
        String[] opts={"Grid: 4 columns","Grid: 5 columns","Open Lucid Settings","Wallpapers","Updater","Refresh app list"};
        new AlertDialog.Builder(this).setTitle("Lucid Launcher").setItems(opts,(d,w)->{if(w==0||w==1){columns=w==0?4:5;render(search.getText().toString());}else if(w==2)launch("com.lucid.os.settings");else if(w==3)launch("com.lucid.os.wallpaper");else if(w==4)launch("com.lucid.os.updater");else{loadApps();render(search.getText().toString());}}).show();
    }
    private void launch(String pkg){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null)startActivity(i);else Toast.makeText(this,"Lucid component not installed yet.",Toast.LENGTH_SHORT).show();}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?1:0));return t;}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}