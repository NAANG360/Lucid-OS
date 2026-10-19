package com.lucid.os.controlcenter;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(242,240,248), INK=Color.rgb(34,34,55), BLUE=Color.rgb(8,102,255), MUTED=Color.rgb(102,101,128);
    private AudioManager audio;
    private CameraManager camera;
    private String cameraId;
    private boolean torchOn;
    private static final int CAMERA_PERMISSION_REQUEST = 41;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w=getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(BG);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        try {
            camera=(CameraManager)getSystemService(CAMERA_SERVICE);
            for(String id:camera.getCameraIdList()) {
                Boolean hasFlash=camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if(Boolean.TRUE.equals(hasFlash)){cameraId=id;break;}
            }
        } catch(Exception ignored) {}
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22),dp(24),dp(22),dp(26)); page.setBackgroundColor(BG);
        TextView eyebrow=label("L U C I D   /   CONTROL CENTER",11,BLUE,true); page.addView(eyebrow);
        TextView title=label("At a glance.",32,INK,true); LinearLayout.LayoutParams tp=params(-1,-2); tp.topMargin=dp(7); page.addView(title,tp);
        TextView sub=label("Your device, one calm surface.",14,MUTED,false); LinearLayout.LayoutParams sp=params(-1,-2); sp.topMargin=dp(3); sp.bottomMargin=dp(18); page.addView(sub,sp);
        LinearLayout pair=new LinearLayout(this); pair.setOrientation(LinearLayout.HORIZONTAL);
        pair.addView(action("Wi-Fi","Manage network",Settings.ACTION_WIFI_SETTINGS),new LinearLayout.LayoutParams(0,dp(100),1));
        pair.addView(action("Bluetooth","Connected devices",Settings.ACTION_BLUETOOTH_SETTINGS),new LinearLayout.LayoutParams(0,dp(100),1));
        page.addView(pair);
        LinearLayout pair2=new LinearLayout(this); pair2.setOrientation(LinearLayout.HORIZONTAL);
        pair2.addView(action("Display","Brightness & display",Settings.ACTION_DISPLAY_SETTINGS),new LinearLayout.LayoutParams(0,dp(100),1));
        pair2.addView(action("Sound","Volume & vibration",Settings.ACTION_SOUND_SETTINGS),new LinearLayout.LayoutParams(0,dp(100),1));
        LinearLayout.LayoutParams p2=params(-1,-2); p2.topMargin=dp(10); page.addView(pair2,p2);
        page.addView(sliderCard("Brightness","Adjust screen brightness",true));
        page.addView(sliderCard("Volume","Adjust media volume",false));
        LinearLayout tools=new LinearLayout(this); tools.setOrientation(LinearLayout.HORIZONTAL);
        Button torch=button("Flashlight"); torch.setOnClickListener(v->toggleTorch()); tools.addView(torch,new LinearLayout.LayoutParams(0,dp(52),1));
        Button settings=button("All settings"); settings.setOnClickListener(v->open(Settings.ACTION_SETTINGS)); LinearLayout.LayoutParams bp=params(0,dp(52)); bp.weight=1; bp.leftMargin=dp(8); tools.addView(settings,bp);
        LinearLayout.LayoutParams toolsP=params(-1,-2); toolsP.topMargin=dp(12); page.addView(tools,toolsP);
        TextView foot=label("LUCID OS  •  LIGHT, WITHOUT THE NOISE",10,MUTED,true); foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp=params(-1,-2); fp.topMargin=dp(20); page.addView(foot,fp);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this); scroll.setFillViewport(true); scroll.addView(page); setContentView(scroll);
    }
    private View sliderCard(String title,String desc,boolean brightness) {
        LinearLayout c=card(); c.setOrientation(LinearLayout.VERTICAL);
        TextView h=label(title,18,INK,true); c.addView(h);
        TextView d=label(desc,12,MUTED,false); LinearLayout.LayoutParams dp=params(-1,-2); dp.topMargin=dp(2); c.addView(d,dp);
        SeekBar bar=new SeekBar(this); bar.setMax(brightness?255:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        if(brightness) {
            int value=128; try { value=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128); } catch(Exception ignored) {}
            bar.setProgress(value);
        } else bar.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean user){
                if(!user)return;
                if(brightness){
                    if(!Settings.System.canWrite(MainActivity.this)){
                        Toast.makeText(MainActivity.this,"Allow Lucid Control Center to modify system settings to control brightness.",Toast.LENGTH_LONG).show();
                        try { startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))); } catch(Exception ignored) {}
                        return;
                    }
                    Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value);
                } else audio.setStreamVolume(AudioManager.STREAM_MUSIC,value,0);
            }
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        LinearLayout.LayoutParams bp=params(-1,-2); bp.topMargin=dp(8); c.addView(bar,bp);
        LinearLayout.LayoutParams cp=params(-1,-2); cp.topMargin=dp(12); c.setLayoutParams(cp); return c;
    }
    private View action(String title,String subtitle,String action) {
        LinearLayout c=card(); c.setGravity(Gravity.CENTER_VERTICAL); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(15),dp(12),dp(12),dp(12));
        c.addView(label(title,17,INK,true)); TextView s=label(subtitle,11,MUTED,false); LinearLayout.LayoutParams p=params(-1,-2); p.topMargin=dp(4); c.addView(s,p);
        c.setOnClickListener(v->open(action)); LinearLayout.LayoutParams cp=params(0,dp(100)); cp.weight=1; cp.rightMargin=dp(5); c.setLayoutParams(cp); c.setFocusable(true); c.setClickable(true); return c;
    }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setPadding(dp(16),dp(14),dp(16),dp(14)); GradientDrawable gd=new GradientDrawable(); gd.setColor(Color.WHITE); gd.setCornerRadius(dp(23)); c.setBackground(gd); c.setElevation(dp(2)); return c; }
    private Button button(String text){ Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); GradientDrawable gd=new GradientDrawable(); gd.setColor(BLUE); gd.setCornerRadius(dp(17)); b.setBackground(gd); return b; }
    private TextView label(String s,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void open(String action){try{startActivity(new Intent(action));}catch(Exception e){Toast.makeText(this,"This setting is unavailable on this Android build.",Toast.LENGTH_SHORT).show();}}
    private void toggleTorch(){
        if(camera==null||cameraId==null){Toast.makeText(this,"No flashlight camera is available.",Toast.LENGTH_SHORT).show();return;}
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST);
            return;
        }
        try{torchOn=!torchOn;camera.setTorchMode(cameraId,torchOn);Toast.makeText(this,torchOn?"Flashlight on":"Flashlight off",Toast.LENGTH_SHORT).show();}
        catch(Exception e){torchOn=!torchOn;Toast.makeText(this,"Flashlight could not be changed: "+e.getClass().getSimpleName(),Toast.LENGTH_SHORT).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==CAMERA_PERMISSION_REQUEST && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) toggleTorch();
        else if(requestCode==CAMERA_PERMISSION_REQUEST) Toast.makeText(this,"Camera permission is needed to control the flashlight.",Toast.LENGTH_SHORT).show();
    }
}