package com.lucid.os.about;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Device information sourced from the running Android build, never hard-coded demo data. */
public final class MainActivity extends Activity {
 private final List<String> details=new ArrayList<>();
 private static final int BG=0xfff5f4fa, INK=0xff1d1e2b, MUTED=0xff676878;
 private LinearLayout root;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state); Window w=getWindow();w.setStatusBarColor(BG);w.setNavigationBarColor(BG);
  w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(14),dp(20),dp(28));scroll.addView(root);
  addText("DEVICE",12,0xff0866ff,true,0);addText("About this device",28,INK,true,8);
  addText(value(Build.MODEL),16,MUTED,false,2);addText("Lucid OS",14,INK,true,2);
  LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(16),dp(6),dp(16),dp(6));card.setBackground(shape(Color.WHITE,18));
  LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,-2);clp.topMargin=dp(20);root.addView(card,clp);
  row(card,"Device",Build.DEVICE);row(card,"Model",Build.MODEL);row(card,"Manufacturer",Build.MANUFACTURER);
  row(card,"Android version",Build.VERSION.RELEASE);row(card,"Android SDK",String.valueOf(Build.VERSION.SDK_INT));
  row(card,"Build number",Build.DISPLAY);row(card,"Build fingerprint",Build.FINGERPRINT);
  row(card,"Security update",Build.VERSION.SDK_INT>=23?Build.VERSION.SECURITY_PATCH:"Not reported");
  if(Build.VERSION.SDK_INT>=31)row(card,"Processor",Build.SOC_MODEL);row(card,"Build type",Build.TYPE);
  Button copy=new Button(this);copy.setText("Copy device information");copy.setAllCaps(false);
  copy.setOnClickListener(v->copyDetails());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(52));bp.topMargin=dp(16);root.addView(copy,bp);
  addText("Values are read from Android on this device.",12,MUTED,false,18);setContentView(scroll);
 }
 private void row(LinearLayout card,String key,String raw){
  String value=value(raw);LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setPadding(0,dp(10),0,dp(10));
  cell.addView(text(key,13,MUTED,false));TextView val=text(value,15,INK,true);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(3);cell.addView(val,vp);card.addView(cell);
  View divider=new View(this);divider.setBackgroundColor(0xffe8e7ef);card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));details.add(key+": "+value);
 }
 private void addText(String s,int size,int color,boolean bold,int top){TextView t=text(s,size,color,bold);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);root.addView(t,p);}
 private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return t;}
 private String value(String s){return s==null||s.trim().isEmpty()?"Not reported":s;}
 private android.graphics.drawable.GradientDrawable shape(int c,int r){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
 private void copyDetails(){StringBuilder b=new StringBuilder("Lucid OS device information\n");for(String s:details)b.append(s).append('\n');ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);if(cm!=null){cm.setPrimaryClip(ClipData.newPlainText("Device information",b.toString()));Toast.makeText(this,"Device information copied",Toast.LENGTH_SHORT).show();}}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}