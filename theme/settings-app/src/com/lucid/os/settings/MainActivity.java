package com.lucid.os.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(242,240,248),INK=Color.rgb(34,34,55),BLUE=Color.rgb(8,102,255),MUTED=Color.rgb(102,101,128);
    @Override public void onCreate(Bundle state){
        super.onCreate(state); Window w=getWindow();w.setStatusBarColor(BG);w.setNavigationBarColor(BG);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(26),dp(22),dp(28));root.setBackgroundColor(BG);
        root.addView(text("L U C I D   /   SYSTEM",11,BLUE,true));
        TextView title=text("Settings",34,INK,true);LinearLayout.LayoutParams p=params(-1,-2);p.topMargin=dp(8);root.addView(title,p);
        TextView sub=text("Make this device yours.",14,MUTED,false);LinearLayout.LayoutParams sp=params(-1,-2);sp.topMargin=dp(3);sp.bottomMargin=dp(18);root.addView(sub,sp);
        addSection(root,"DEVICE",new String[]{"Network & internet","Connected devices","Display & brightness","Sound & vibration","Storage","Battery","Security & privacy","Location","Accessibility","System & updates"},new String[]{Settings.ACTION_WIRELESS_SETTINGS,Settings.ACTION_BLUETOOTH_SETTINGS,Settings.ACTION_DISPLAY_SETTINGS,Settings.ACTION_SOUND_SETTINGS,Settings.ACTION_INTERNAL_STORAGE_SETTINGS,Settings.ACTION_BATTERY_SAVER_SETTINGS,Settings.ACTION_SECURITY_SETTINGS,Settings.ACTION_LOCATION_SOURCE_SETTINGS,Settings.ACTION_ACCESSIBILITY_SETTINGS,Settings.ACTION_SETTINGS});
        addSection(root,"PERSONALIZATION",new String[]{"Wallpapers & system colors","Control Center","Home screen & gestures","Notifications","Apps","About this device"},new String[]{"com.lucid.os.wallpapers","com.lucid.os.controlcenter",Settings.ACTION_HOME_SETTINGS,Settings.ACTION_NOTIFICATION_SETTINGS,Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,"lucid://about"});
        TextView note=text("Lucid Settings is an early functional dashboard. Platform-owned pages remain available for controls that need Android's privileged Settings provider.",11,MUTED,false);LinearLayout.LayoutParams np=params(-1,-2);np.topMargin=dp(14);root.addView(note,np);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
    }
    private void addSection(LinearLayout root,String heading,String[] labels,String[] actions){
        TextView h=text(heading,10,MUTED,true);LinearLayout.LayoutParams hp=params(-1,-2);hp.topMargin=dp(14);hp.bottomMargin=dp(8);root.addView(h,hp);
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(22));group.setBackground(bg);group.setClipToOutline(true);
        for(int i=0;i<labels.length;i++){
            final String action=actions[i];LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(15),dp(16),dp(15));row.addView(text(labels[i],15,INK,false),new LinearLayout.LayoutParams(0,-2,1));
            TextView arrow=text("›",25,BLUE,false);row.addView(arrow);
            row.setClickable(true);row.setFocusable(true);row.setOnClickListener(v->launch(action));
            group.addView(row,new LinearLayout.LayoutParams(-1,-2));
            if(i<labels.length-1){View divider=new View(this);divider.setBackgroundColor(Color.rgb(235,233,242));LinearLayout.LayoutParams dp=params(-1,1);dp.leftMargin=dp(16);dp.rightMargin=dp(16);group.addView(divider,dp);}
        }
        root.addView(group);
    }
    private void launch(String action){
        if(action.equals("lucid://about")){showAbout();return;}
        try{
            Intent i;
            if(action.equals("com.lucid.os.wallpapers") || action.equals("com.lucid.os.controlcenter")){i=new Intent();i.setClassName(this,action+".MainActivity");}
            else i=new Intent(action);
            startActivity(i);
        }catch(Exception e){Toast.makeText(this,"That settings page is not exposed by this Android build.",Toast.LENGTH_SHORT).show();}
    }
    private void showAbout(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(22),dp(26),dp(22),dp(28));page.setBackgroundColor(BG);
        TextView back=text("‹   Settings",15,BLUE,true);back.setPadding(0,0,0,dp(16));back.setOnClickListener(v->onCreate(null));page.addView(back);
        page.addView(text("ABOUT THIS DEVICE",10,MUTED,true));
        TextView title=text("LucidOS",36,INK,true);LinearLayout.LayoutParams tp=params(-1,-2);tp.topMargin=dp(8);page.addView(title,tp);
        TextView sub=text("A system that feels like yours.",14,MUTED,false);LinearLayout.LayoutParams sp=params(-1,-2);sp.topMargin=dp(4);sp.bottomMargin=dp(20);page.addView(sub,sp);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(8),dp(18),dp(8));GradientDrawable gd=new GradientDrawable();gd.setColor(Color.WHITE);gd.setCornerRadius(dp(22));card.setBackground(gd);
        addInfo(card,"Device","Redmi Note 7 / lavender");addInfo(card,"Operating system","LucidOS");addInfo(card,"Version","0.1 (development)");addInfo(card,"Build status","Development build");
        page.addView(card);
        TextView disclaimer=text("This is the LucidOS identity page. Some device and build details remain dependent on the platform image being finalized.",12,MUTED,false);LinearLayout.LayoutParams np=params(-1,-2);np.topMargin=dp(16);page.addView(disclaimer,np);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(page);setContentView(scroll);
    }
    private void addInfo(LinearLayout card,String key,String value){
        TextView k=text(key.toUpperCase(),10,MUTED,true);LinearLayout.LayoutParams kp=params(-1,-2);kp.topMargin=dp(12);card.addView(k,kp);
        TextView v=text(value,16,INK,false);LinearLayout.LayoutParams vp=params(-1,-2);vp.topMargin=dp(3);vp.bottomMargin=dp(12);card.addView(v,vp);
        View line=new View(this);line.setBackgroundColor(Color.rgb(235,233,242));card.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return t;}
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}