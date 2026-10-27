package com.lucid.os.desktop;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/** UI for the ROM-side desktop controller. Privileged operations remain in a reviewed ROM script. */
public final class MainActivity extends Activity {
    private final int bg=Color.rgb(12,13,19), card=Color.rgb(25,26,38), fg=Color.rgb(245,244,252), muted=Color.rgb(167,166,187), accent=Color.rgb(139,119,255);
    private LinearLayout root, logBox;
    private TextView status;
    private String lastLog="";
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,28,22,24);root.setBackgroundColor(bg);
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        TextView brand=new TextView(this);brand.setText("LUCID / DESKTOP");brand.setTextColor(accent);brand.setTextSize(13);brand.setTypeface(Typeface.DEFAULT_BOLD);root.addView(brand);
        TextView title=new TextView(this);title.setText("Your phone. A desktop.");title.setTextColor(fg);title.setTextSize(29);title.setTypeface(Typeface.create("sans-serif-medium",0));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=10;root.addView(title,tp);
        TextView desc=new TextView(this);desc.setText("Start the Ubuntu environment on-device, then render its desktop through the X11 display server. Lucid checks prerequisites before it launches anything.");desc.setTextColor(muted);desc.setTextSize(14);desc.setLineSpacing(3,1);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.topMargin=8;dp.bottomMargin=20;root.addView(desc,dp);
        status=new TextView(this);status.setText("STATUS  •  Not checked");status.setTextColor(fg);status.setTextSize(14);status.setPadding(16,16,16,16);status.setBackground(shape(card,18));root.addView(status);
        addButton("Start Lucid Desktop","Start the chroot desktop after the X11 display is running",()->runController("start"));
        addButton("Stop desktop","Stop the desktop session without rebooting",()->runController("stop"));
        addButton("Check requirements","Check root, rootfs, X11 server, mounts and DNS",()->runController("check"));
        addButton("Open X11 server","Open the installed Termux:X11 app",()->openX11());
        addButton("View diagnostics","Read the latest desktop diagnostic log",()->showLog());
        TextView note=new TextView(this);note.setText("Requires a ROM build that includes /system/bin/lucid_desktop.sh and a compatible X11 server app. This controller cannot manufacture Android root or an X server by itself.");note.setTextColor(muted);note.setTextSize(12);note.setPadding(4,20,4,4);root.addView(note);
    }
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(1,Color.rgb(49,48,68));return d;}
    private void addButton(String title,String sub,Runnable action){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(16,14,16,14);row.setBackground(shape(card,16));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=10;root.addView(row,rp);
        TextView t=new TextView(this);t.setText(title);t.setTextColor(fg);t.setTextSize(16);t.setTypeface(Typeface.DEFAULT_BOLD);row.addView(t);
        TextView s=new TextView(this);s.setText(sub);s.setTextColor(muted);s.setTextSize(12);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=4;row.addView(s,sp);
        row.setOnClickListener(v->action.run());
    }
    private void runController(String action){
        status.setText("STATUS  •  Running "+action+"…");
        new Thread(()->{
            String output=execRoot("if [ -x /system/bin/lucid_desktop.sh ]; then /system/bin/lucid_desktop.sh "+action+"; else echo 'Missing /system/bin/lucid_desktop.sh: this ROM build does not include the desktop controller.'; exit 127; fi");
            lastLog=output;
            runOnUiThread(()->{status.setText(output.contains("LUCID_DESKTOP_OK")?"STATUS  •  Ready":output.contains("LUCID_DESKTOP_STARTED")?"STATUS  •  Starting":output.contains("LUCID_DESKTOP_STOPPED")?"STATUS  •  Stopped":"STATUS  •  Needs attention");new AlertDialog.Builder(this).setTitle("Lucid Desktop · "+action).setMessage(output.isEmpty()?"No output returned. Check ROM integration and root permissions.":output).setPositiveButton("OK",null).show();});
        }).start();
    }
    private String execRoot(String command){
        Process p=null;try{
            p=Runtime.getRuntime().exec("su");
            DataOutputStream os=new DataOutputStream(p.getOutputStream());
            os.writeBytes(command+" 2>&1\nexit\n");os.flush();
            InputStream in=p.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;
            while((n=in.read(b))>0)out.write(b,0,n);
            p.waitFor();return out.toString("UTF-8");
        }catch(Exception e){return "Could not run the ROM controller as root: "+e.getClass().getSimpleName()+": "+e.getMessage()+"\nEnsure this ROM has its configured root provider and desktop init integration.";}
        finally{if(p!=null)p.destroy();}
    }
    private void openX11(){
        Intent i=getPackageManager().getLaunchIntentForPackage("com.termux.x11");
        if(i==null){new AlertDialog.Builder(this).setTitle("X11 server not found").setMessage("Install a compatible Termux:X11 server and companion package first. A display server is not built into Android.").setPositiveButton("OK",null).show();return;}
        try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Could not open X11 server.",Toast.LENGTH_LONG).show();}
    }
    private void showLog(){if(lastLog.isEmpty())lastLog=execRoot("cat /data/local/lucid_desktop/lucid-desktop.log 2>/dev/null || cat /data/local/tmp/lucid-desktop.log 2>/dev/null || echo 'No log found yet. Run Check requirements.'");new AlertDialog.Builder(this).setTitle("Desktop diagnostics").setMessage(lastLog).setPositiveButton("Copy", (d,w)->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("Lucid Desktop log",lastLog));Toast.makeText(this,"Diagnostics copied",Toast.LENGTH_SHORT).show();}).setNegativeButton("Close",null).show();}
}