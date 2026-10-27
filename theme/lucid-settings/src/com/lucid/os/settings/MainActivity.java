package com.lucid.os.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private final int BG=Color.rgb(12,13,19), CARD=Color.rgb(24,25,35), CARD2=Color.rgb(34,35,48), FG=Color.rgb(245,245,252), MUTED=Color.rgb(163,165,184);
    private int ACCENT=Color.rgb(139,119,255);
    private LinearLayout list;
    private EditText search;
    private final List<Item> items=new ArrayList<>();
    static class Item { String title,subtitle,group; int icon; String action; Item(String t,String s,String g,int i,String a){title=t;subtitle=s;group=g;icon=i;action=a;} }
    @Override public void onCreate(Bundle b){super.onCreate(b);ACCENT=getSharedPreferences("lucid_appearance",MODE_PRIVATE).getInt("accent",Color.rgb(139,119,255));getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0);populate();build();}
    private void populate(){
        items.add(new Item("Network & internet","Wi-Fi, mobile network, hotspot","CONNECTIVITY",0,Settings.ACTION_WIRELESS_SETTINGS));
        items.add(new Item("Connected devices","Bluetooth, pairing, connection preferences","CONNECTIVITY",1,Settings.ACTION_BLUETOOTH_SETTINGS));
        items.add(new Item("Apps","Default apps, recent apps, permissions","APPS",2,Settings.ACTION_APPLICATION_SETTINGS));
        items.add(new Item("Notifications","Notification history and conversations","PERSONAL",3,Settings.ACTION_APP_NOTIFICATION_SETTINGS));
        items.add(new Item("Display & colour","Brightness, dark theme, screen timeout","PERSONAL",4,Settings.ACTION_DISPLAY_SETTINGS));
        items.add(new Item("Sound & vibration","Volume, ringtones, vibration","PERSONAL",5,Settings.ACTION_SOUND_SETTINGS));
        items.add(new Item("Wallpaper & style","Wallpaper, colour palette and icons","LUCID",6,"lucid_wallpaper"));
        items.add(new Item("Lucid Control Centre","Quick controls and nearby-device discovery","LUCID",7,"lucid_control"));
        items.add(new Item("Themes & appearance","Lucid accents, shapes and interface styling","LUCID",8,"lucid_theme"));
        items.add(new Item("Security & privacy","Screen lock, permissions and privacy","SECURITY",9,Settings.ACTION_SECURITY_SETTINGS));
        items.add(new Item("Location","Location services and app access","SECURITY",10,Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        items.add(new Item("Battery","Battery status and power use","DEVICE",11,Settings.ACTION_BATTERY_SAVER_SETTINGS));
        items.add(new Item("Storage","Space used by apps and files","DEVICE",12,Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        items.add(new Item("System & updates","Languages, gestures, system update","DEVICE",13,Settings.ACTION_SETTINGS));
        items.add(new Item("Lucid Updater","Check and download the latest lavender ROM","LUCID",15,"lucid_updater"));
        items.add(new Item("About this device","Lucid OS, device, Android and build details","DEVICE",14,"lucid_about"));
        items.add(new Item("Lock screen","Clock, notifications and display settings","PERSONAL",16,Settings.ACTION_DISPLAY_SETTINGS));
    }
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(18),dp(20),dp(30));scroll.addView(page);
        TextView eyebrow=txt("L U C I D   O S",11,ACCENT,true);page.addView(eyebrow);
        TextView title=txt("Settings",34,FG,true);title.setPadding(0,dp(4),0,dp(3));page.addView(title);
        TextView sub=txt("Your device. Your rules.",14,MUTED,false);page.addView(sub);
        LinearLayout hero=card();hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(17),dp(16),dp(17),dp(16));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView orb=txt("L",24,BG,true);orb.setGravity(Gravity.CENTER);orb.setBackground(shape(ACCENT,100));LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(dp(48),dp(48));top.addView(orb,op);
        LinearLayout ident=new LinearLayout(this);ident.setOrientation(LinearLayout.VERTICAL);ident.setPadding(dp(12),0,0,0);
        ident.addView(txt("Lucid OS",18,FG,true));ident.addView(txt(android.os.Build.MODEL+"  •  "+android.os.Build.VERSION.RELEASE,12,MUTED,false));top.addView(ident,new LinearLayout.LayoutParams(0,-2,1));
        hero.addView(top);
        BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);int level=bm==null?-1:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        TextView status=txt("BATTERY  "+(level>=0?level+"%":"—")+"     •     "+android.os.Build.DEVICE.toUpperCase(Locale.ROOT),10,MUTED,true);status.setPadding(0,dp(14),0,0);hero.addView(status);page.addView(hero,margin(-1,-2,0,dp(22),0,dp(20)));
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(FG);search.setHintTextColor(MUTED);search.setHint("⌕   Search settings");search.setTextSize(15);search.setPadding(dp(15),dp(12),dp(15),dp(12));search.setBackground(shape(CARD2,18));page.addView(search,margin(-1,-2,0,0,0,dp(20)));
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);page.addView(list);
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render(s.toString());}public void afterTextChanged(android.text.Editable e){}});
        setContentView(scroll);render("");
    }
    private void render(String query){
        list.removeAllViews();String q=query.toLowerCase(Locale.ROOT);String last="";
        for(Item item:items){if(!(item.title+" "+item.subtitle+" "+item.group).toLowerCase(Locale.ROOT).contains(q))continue;
            if(!item.group.equals(last)){TextView h=txt(groupName(item.group),10,ACCENT,true);h.setLetterSpacing(.14f);h.setPadding(dp(3),dp(7),0,dp(8));list.addView(h);last=item.group;}
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(13),dp(12),dp(12),dp(12));row.setBackground(shape(CARD,17));
            TextView icon=txt(icon(item.icon),18,ACCENT,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(CARD2,13));row.addView(icon,new LinearLayout.LayoutParams(dp(43),dp(43)));
            LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(dp(12),0,dp(5),0);col.addView(txt(item.title,14,FG,true));TextView d=txt(item.subtitle,11,MUTED,false);d.setMaxLines(2);col.addView(d,margin(-1,-2,0,dp(3),0,0));row.addView(col,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(txt("›",25,MUTED,false));row.setOnClickListener(v->open(item));list.addView(row,margin(-1,-2,0,0,0,dp(7)));
        }
        if(list.getChildCount()==0)list.addView(txt("No settings match “"+query+"”.",14,MUTED,false));
    }
    private void open(Item i){
        if(i.action.equals("lucid_control")){launch("com.lucid.os.controlcentre");return;}
        if(i.action.equals("lucid_updater")){launch("com.lucid.os.updater");return;}
        if(i.action.equals("lucid_wallpaper")){launch("com.lucid.os.wallpaper");return;}
        if(i.action.equals("lucid_theme")){showAppearance();return;}
        if(i.action.equals("lucid_about")){showAbout();return;}
        try{startActivity(new Intent(i.action));}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}}
    }

    private void showAppearance(){
        String[] names={"Violet Dusk","Blue Pulse","Cyan Horizon","Bubbles","Rounded surfaces","Compact spacing","Reduce motion"};
        int[] colors={Color.rgb(139,119,255),Color.rgb(8,102,255),Color.rgb(0,175,200)};
        android.content.SharedPreferences prefs=getSharedPreferences("lucid_appearance",MODE_PRIVATE);
        boolean[] checked={prefs.getInt("accent",colors[0])==colors[0],prefs.getInt("accent",colors[0])==colors[1],prefs.getInt("accent",colors[0])==colors[2],prefs.getBoolean("bubbles",true),prefs.getBoolean("rounded",true),prefs.getBoolean("compact",false),prefs.getBoolean("reduce_motion",false)};
        new AlertDialog.Builder(this).setTitle("Lucid appearance").setMultiChoiceItems(names,checked,(dialog,which,isChecked)->{
            android.content.SharedPreferences.Editor e=prefs.edit();
            if(which<3){e.putInt("accent",colors[which]);ACCENT=colors[which];}
            else if(which==3)e.putBoolean("bubbles",isChecked);
            else if(which==4)e.putBoolean("rounded",isChecked);
            else if(which==5)e.putBoolean("compact",isChecked);
            else e.putBoolean("reduce_motion",isChecked);
            e.apply();
            if(which<3){dialog.dismiss();recreate();}
        }).setPositiveButton("Apply",(d,w)->recreate()).setNegativeButton("Cancel",null).show();
    }
    private void showAbout(){
        String device=android.os.Build.DEVICE;
        String codename="lavender".equalsIgnoreCase(device)?"lavender":"unverified";
        String text="LUCID OS\\n\\nDevice: "+android.os.Build.MODEL+" ("+device+")\\nTarget codename: "+codename+"\\nAndroid: "+android.os.Build.VERSION.RELEASE+" (SDK "+android.os.Build.VERSION.SDK_INT+")\\nBuild ID: "+android.os.Build.DISPLAY+"\\nFingerprint: "+android.os.Build.FINGERPRINT+"\\n\\nLucid OS is a work in progress. Build identity is reported by this installed system and does not by itself certify a verified Lucid release.";
        new AlertDialog.Builder(this).setTitle("About Lucid OS").setMessage(text).setPositiveButton("Android device details",(d,w)->{try{startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));}catch(Exception ignored){}}).setNegativeButton("Close",null).show();
    }
\n    private int selectedAccent(int[] colors){for(int i=0;i<colors.length;i++)if(colors[i]==ACCENT)return i;return 0;}
    private void launch(String pkg){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null)startActivity(i);else new AlertDialog.Builder(this).setMessage("This Lucid component is not installed in the current build yet.").setPositiveButton("OK",null).show();}catch(Exception e){}}
    private String groupName(String g){switch(g){case "CONNECTIVITY":return "CONNECTIONS";case "PERSONAL":return "PERSONALIZATION";case "LUCID":return "LUCID EXPERIENCE";case "SECURITY":return "SECURITY & PRIVACY";case "DEVICE":return "DEVICE";default:return g;}}
    private String icon(int i){String[] a={"⌁","◉","▦","◌","☼","♫","✧","◈","◌","◇","⌖","▤","▧","⚙","ⓘ","↻"};return a[i%a.length];}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setBackground(shape(CARD,22));return l;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}