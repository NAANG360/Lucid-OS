package com.lucid.os.controlcentre;

import android.app.Activity;
import android.app.NotificationManager;
import android.hardware.camera2.CameraManager;
import android.app.AlertDialog;
import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(13,14,19), CARD=Color.rgb(28,29,38), CARD2=Color.rgb(39,39,51);
    private static final int FG=Color.rgb(246,244,251), MUTED=Color.rgb(172,171,190), ACCENT=Color.rgb(196,151,255);
    private LinearLayout root, tiles, devices;
    private TextView scanStatus;
    private NsdManager nsd;
    private WifiManager wifi;
    private WifiManager.MulticastLock multicastLock;
    private final List<NsdServiceInfo> found = new ArrayList<>();
    private final String[] serviceTypes = {"_http._tcp.","_https._tcp.","_smb._tcp.","_airplay._tcp.","_googlecast._tcp.","_companion-link._tcp."};
    private int scansPending;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        nsd=(NsdManager)getSystemService(NSD_SERVICE); wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(12),dp(20),dp(30)); scroll.addView(root);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("lucid",27,FG,true); brand.setLetterSpacing(-.04f); top.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        top.addView(pill("CONTROL CENTRE",ACCENT)); root.addView(top);
        root.addView(text("Everything, within reach.",24,FG,true),margins(-1,-2,0,dp(10),0,dp(3)));
        root.addView(text("A calmer control surface for your phone and nearby devices.",14,MUTED,false),margins(-1,-2,0,0,0,dp(18)));

        LinearLayout quick=cardColumn();
        quick.addView(section("QUICK CONTROLS"));
        tiles=new LinearLayout(this); tiles.setOrientation(LinearLayout.VERTICAL);
        addTileRow(tiles, new String[]{"Internet","Bluetooth"},new String[]{"Wi-Fi, mobile network","Pair and manage devices"},new int[]{0,1});
        addTileRow(tiles, new String[]{"Flashlight","Do Not Disturb"},new String[]{"Open flashlight controls","Notification preferences"},new int[]{2,3});
        addTileRow(tiles, new String[]{"Display","Sound"},new String[]{"Brightness and screen","Volume and audio settings"},new int[]{4,5});
        quick.addView(tiles);
        quick.addView(section("LEVELS"),margins(-1,-2,0,dp(14),0,0));
        quick.addView(levelControl("Brightness", true),margins(-1,-2,0,dp(2),0,dp(8)));
        quick.addView(levelControl("Media volume", false),margins(-1,-2,0,0,0,0));
        root.addView(quick,margins(-1,-2,0,0,0,dp(14)));

        LinearLayout connect=cardColumn();
        LinearLayout heading=new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("Lucid Connect",21,FG,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView scan=pill("SCAN ↗",ACCENT); scan.setOnClickListener(v->scanServices()); heading.addView(scan); connect.addView(heading);
        connect.addView(text("Discover services nearby. Drag a discovered device into the centre to open its available connection actions.",13,MUTED,false),margins(-1,-2,0,dp(5),0,dp(8)));
        FrameLayout orbit=new FrameLayout(this); orbit.setBackground(round(CARD2,22));
        orbit.setMinimumHeight(dp(230));
        View ring=new View(this); GradientDrawable rg=new GradientDrawable();rg.setShape(GradientDrawable.OVAL);rg.setColor(Color.TRANSPARENT);rg.setStroke(dp(1),Color.rgb(79,72,101));ring.setBackground(rg);
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(dp(178),dp(178),Gravity.CENTER);orbit.addView(ring,rp);
        TextView centre=text("L",31,BG,true); centre.setGravity(Gravity.CENTER);centre.setBackground(round(ACCENT,60));
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.CENTER);orbit.addView(centre,cp);
        TextView n1=node("This phone"); FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(dp(100),dp(43),Gravity.TOP|Gravity.CENTER_HORIZONTAL);np.topMargin=dp(15);orbit.addView(n1,np);
        TextView n2=node("Nearby devices"); FrameLayout.LayoutParams n2p=new FrameLayout.LayoutParams(dp(124),dp(43),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);n2p.bottomMargin=dp(15);orbit.addView(n2,n2p);
        TextView n3=node("Drag to connect"); FrameLayout.LayoutParams n3p=new FrameLayout.LayoutParams(dp(112),dp(43),Gravity.CENTER_VERTICAL|Gravity.LEFT);n3p.leftMargin=dp(8);orbit.addView(n3,n3p);
        TextView n4=node("Local network"); FrameLayout.LayoutParams n4p=new FrameLayout.LayoutParams(dp(112),dp(43),Gravity.CENTER_VERTICAL|Gravity.RIGHT);n4p.rightMargin=dp(8);orbit.addView(n4,n4p);
        connect.addView(orbit,margins(-1,dp(230),0,dp(8),0,dp(10)));
        scanStatus=text("Ready to discover compatible local services.",12,MUTED,false);connect.addView(scanStatus);
        devices=new LinearLayout(this);devices.setOrientation(LinearLayout.VERTICAL);connect.addView(devices,margins(-1,-2,0,dp(8),0,0));
        TextView privacy=text("Discovery stays on your local network. A service being visible does not grant access; compatible services may still require pairing or authentication.",12,MUTED,false);
        privacy.setPadding(dp(12),dp(12),dp(12),dp(12));privacy.setBackground(round(BG,14));connect.addView(privacy,margins(-1,-2,0,dp(10),0,0));
        root.addView(connect,margins(-1,-2,0,0,0,dp(14)));

        LinearLayout footer=cardColumn();footer.addView(section("SYSTEM INTEGRATION"));
        footer.addView(text("This app is the Lucid control and discovery surface. Replacing Android's actual notification shade requires building it into SystemUI in the ROM; an ordinary APK cannot replace privileged Quick Settings.",13,MUTED,false));
        TextView settings=text("Open Android system settings  ↗",14,FG,true);settings.setPadding(0,dp(14),0,dp(2));settings.setOnClickListener(v->openSettings());footer.addView(settings);root.addView(footer);
        setContentView(scroll);
    }

    private void addTileRow(LinearLayout parent,String[] names,String[] subtitles,int[] actions) {
        for(int i=0;i<names.length;i++) {
            final int action=actions[i]; LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(11),dp(10),dp(11),dp(10));row.setBackground(round(CARD2,15));
            LinearLayout textCol=new LinearLayout(this);textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.addView(text(names[i],15,FG,true));textCol.addView(text(subtitles[i],11,MUTED,false),margins(-1,-2,0,dp(2),0,0));
            row.addView(textCol,new LinearLayout.LayoutParams(0,-2,1));
            TextView go=pill("OPEN",ACCENT);row.addView(go);row.setOnClickListener(v->runControl(action));
            parent.addView(row,margins(-1,-2,0,dp(5),0,0));
        }
    }

    private View levelControl(String label, boolean brightness) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout title=new LinearLayout(this); title.setGravity(Gravity.CENTER_VERTICAL);
        title.addView(text(label,14,FG,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView value=text("",12,MUTED,false); title.addView(value); box.addView(title);
        SeekBar seek=new SeekBar(this); seek.setMax(100);
        if(brightness) {
            int current=128;
            try { current=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,128); } catch(Exception ignored) {}
            seek.setProgress(Math.max(0,Math.min(100,current*100/255)));
            value.setText(seek.getProgress()+"%");
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar b,int p,boolean fromUser){value.setText(p+"%");}
                public void onStartTrackingTouch(SeekBar b){}
                public void onStopTrackingTouch(SeekBar b){
                    if(!Settings.System.canWrite(MainActivity.this)) {
                        Toast.makeText(MainActivity.this,"Allow Lucid Control Centre to modify system settings to control brightness.",Toast.LENGTH_LONG).show();
                        try { startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()))); } catch(Exception ignored) {}
                        return;
                    }
                    try { Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,Math.max(1,b.getProgress()*255/100)); }
                    catch(Exception e){Toast.makeText(MainActivity.this,"Brightness could not be changed.",Toast.LENGTH_SHORT).show();}
                }
            });
        } else {
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
            int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), cur=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            seek.setProgress(max==0?0:cur*100/max); value.setText(seek.getProgress()+"%");
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar b,int p,boolean fromUser){value.setText(p+"%");if(fromUser){int m=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);audio.setStreamVolume(AudioManager.STREAM_MUSIC,Math.round(m*p/100f),0);}}
                public void onStartTrackingTouch(SeekBar b){}
                public void onStopTrackingTouch(SeekBar b){}
            });
        }
        box.addView(seek); return box;
    }

    private void runControl(int action) {
        try {
            switch(action) {
                case 0: startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)); break;
                case 1: startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); break;
                case 2: toggleFlashlight(); break;
                case 3: toggleDnd(); break;
                case 4: startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); break;
                default: startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)); break;
            }
        } catch(Exception e) { openSettings(); }
    }

    private void toggleFlashlight(){
        if(android.os.Build.VERSION.SDK_INT>=23 && checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},901);return;}
        try{CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);String[] ids=cm.getCameraIdList();for(String id:ids){android.hardware.camera2.CameraCharacteristics ch=cm.getCameraCharacteristics(id);Boolean flash=ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);if(Boolean.TRUE.equals(flash)){boolean on=getPreferences(MODE_PRIVATE).getBoolean("torch_on",false);cm.setTorchMode(id,!on);getPreferences(MODE_PRIVATE).edit().putBoolean("torch_on",!on).apply();Toast.makeText(this,!on?"Flashlight on":"Flashlight off",Toast.LENGTH_SHORT).show();return;}}Toast.makeText(this,"No flashlight found.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Flashlight unavailable: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void toggleDnd(){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm==null)return;
        if(!nm.isNotificationPolicyAccessGranted()){try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));}catch(Exception e){openSettings();}Toast.makeText(this,"Allow Lucid Control Centre to manage Do Not Disturb, then tap again.",Toast.LENGTH_LONG).show();return;}
        try{int current=nm.getCurrentInterruptionFilter();nm.setInterruptionFilter(current==NotificationManager.INTERRUPTION_FILTER_NONE?NotificationManager.INTERRUPTION_FILTER_ALL:NotificationManager.INTERRUPTION_FILTER_NONE);Toast.makeText(this,current==NotificationManager.INTERRUPTION_FILTER_NONE?"Do Not Disturb off":"Do Not Disturb on",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Could not change Do Not Disturb.",Toast.LENGTH_SHORT).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==901&&grantResults.length>0&&grantResults[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)toggleFlashlight();}
    private void openSettings(){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}}

    private void scanServices() {
        if(nsd==null){scanStatus.setText("Local service discovery is unavailable on this build.");return;}
        found.clear();devices.removeAllViews();scanStatus.setText("Scanning for compatible local services…");
        if(wifi!=null)try{multicastLock=wifi.createMulticastLock("lucid-nsd");multicastLock.setReferenceCounted(false);multicastLock.acquire();}catch(Exception ignored){}
        scansPending=serviceTypes.length;
        for(String type:serviceTypes) {
            try {
                nsd.discoverServices(type,NsdManager.PROTOCOL_DNS_SD,new NsdManager.DiscoveryListener(){
                    public void onStartDiscoveryFailed(String t,int e){finishScan();}
                    public void onStopDiscoveryFailed(String t,int e){}
                    public void onDiscoveryStarted(String t){}
                    public void onDiscoveryStopped(String t){}
                    public void onServiceFound(NsdServiceInfo info) {
                        final String key=info.getServiceName()+"|"+info.getServiceType();
                        runOnUiThread(()->{boolean duplicate=false;for(NsdServiceInfo f:found)if((f.getServiceName()+"|"+f.getServiceType()).equals(key))duplicate=true;if(!duplicate){found.add(info);addFound(info);}});
                    }
                    public void onServiceLost(NsdServiceInfo info){runOnUiThread(()->{scanStatus.setText("A discovered service is no longer available.");});}
                });
            } catch(Exception e){finishScan();}
        }
        new android.os.Handler().postDelayed(()->{
            scanStatus.setText(found.isEmpty()?"No compatible advertised services found. Ensure devices are on the same Wi-Fi and advertise a supported service.":found.size()+" service(s) discovered on this network.");
            releaseMulticast();
        },6500);
    }

    private void finishScan(){runOnUiThread(()->{if(--scansPending<=0)releaseMulticast();});}
    private void releaseMulticast(){try{if(multicastLock!=null&&multicastLock.isHeld())multicastLock.release();}catch(Exception ignored){}}

    private void addFound(NsdServiceInfo info) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(12),dp(12),dp(12));row.setBackground(round(CARD2,14));
        LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(info.getServiceName(),15,FG,true));col.addView(text(info.getServiceType(),11,MUTED,false));
        row.addView(col,new LinearLayout.LayoutParams(0,-2,1));TextView action=pill("DETAILS",ACCENT);row.addView(action);
        row.setOnTouchListener(new View.OnTouchListener(){float x,y;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){if(Math.abs(e.getX()-x)+Math.abs(e.getY()-y)>dp(18)){showService(info);return true;}showService(info);return true;}return true;}});
        row.setOnClickListener(v->showService(info));devices.addView(row,margins(-1,-2,0,dp(5),0,0));
    }

    private void showService(NsdServiceInfo info) {
        new AlertDialog.Builder(this).setTitle(info.getServiceName())
            .setMessage("Service type: "+info.getServiceType()+"\n\nLucid discovered this service on your local network. Discovery alone does not establish a trusted connection. Only proceed if you recognise the device; service-specific connection and authentication support may be required.")
            .setPositiveButton("OK",null).setNeutralButton("Network settings",(d,w)->{try{startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));}catch(Exception e){openSettings();}}).show();
    }

    private LinearLayout cardColumn(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(14),dp(14),dp(14),dp(14));l.setBackground(round(CARD,21));return l;}
    private TextView section(String s){TextView t=text(s,10,ACCENT,true);t.setLetterSpacing(.12f);t.setPadding(0,0,0,dp(10));return t;}
    private TextView node(String s){TextView t=text(s,11,FG,true);t.setGravity(Gravity.CENTER);t.setBackground(round(CARD,22));t.setElevation(dp(3));t.setOnDragListener((v,e)->{if(e.getAction()==android.view.DragEvent.ACTION_DROP){scanServices();return true;}return true;});return t;}
    private TextView pill(String s,int color){TextView t=text(s,10,color,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(11),dp(8),dp(11),dp(8));t.setBackground(round(CARD2,30));return t;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout.LayoutParams margins(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}