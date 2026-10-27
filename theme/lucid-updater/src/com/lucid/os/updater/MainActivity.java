package com.lucid.os.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public final class MainActivity extends Activity {
    private static final String MANIFEST="https://raw.githubusercontent.com/NAANG360/Lucid-OS/main/ota/stable.json";
    private final int bg=Color.rgb(12,13,19),card=Color.rgb(24,25,35),fg=Color.rgb(245,245,252),muted=Color.rgb(163,165,184),accent=Color.rgb(139,119,255);
    private TextView version,details,status;private ProgressBar progress;private Button check,download;private JSONObject update;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);build();checkUpdate();}
    private void build(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(22),dp(28),dp(22),dp(28));page.setBackgroundColor(bg);
        page.addView(text("L U C I D   O S",11,accent,true));page.addView(text("System updater",32,fg,true),lp(0,dp(4),0,0,0,0));
        page.addView(text("Your next build, without the guesswork.",14,muted,false),lp(0,0,0,0,0,dp(22)));
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(shape(card,22));
        hero.addView(text("AVAILABLE BUILD",10,accent,true));version=text("Checking for updates…",22,fg,true);hero.addView(version,lp(0,dp(8),0,0,0,0));
        details=text("Target device: lavender",13,muted,false);hero.addView(details,lp(0,dp(8),0,0,0,0));page.addView(hero,lp(0,0,0,0,0,dp(18)));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setProgress(0);page.addView(progress,lp(0,dp(12),0,0,0,dp(8)));
        status=text("Fetching the public OTA manifest…",12,muted,false);page.addView(status,lp(0,0,0,0,0,dp(18)));
        check=new Button(this);check.setText("CHECK AGAIN");check.setOnClickListener(v->checkUpdate());page.addView(check,lp(0,dp(50),0,0,0,dp(8)));
        download=new Button(this);download.setText("DOWNLOAD & VERIFY");download.setEnabled(false);download.setOnClickListener(v->downloadUpdate());page.addView(download,lp(0,dp(50),0,0,0,dp(12)));
        TextView note=text("Lucid verifies the downloaded archive against the OTA manifest SHA-256. This prototype downloads and verifies the ROM; flashing still requires a compatible recovery or ROM-integrated privileged installer.",12,muted,false);page.addView(note);
        setContentView(page);
    }
    private void checkUpdate(){
        check.setEnabled(false);download.setEnabled(false);status.setText("Fetching OTA manifest…");
        new Thread(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(MANIFEST).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("Accept","application/json");if(c.getResponseCode()!=200)throw new IOException("Manifest HTTP "+c.getResponseCode());ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}JSONObject j=new JSONObject(out.toString("UTF-8"));if(!"lavender".equals(j.optString("device")))throw new IOException("Manifest target is not lavender");String url=j.getString("url"),sha=j.getString("sha256");if(!url.startsWith("https://")||sha.length()!=64)throw new IOException("Invalid manifest URL or checksum");runOnUiThread(()->{update=j;version.setText(j.optString("version","Lucid build"));details.setText("Target: "+j.optString("device")+"  •  "+human(j.optLong("size",0)));status.setText(j.optString("changelog","Update available."));download.setEnabled(true);check.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{status.setText("Update check failed: "+e.getMessage());check.setEnabled(true);});}finally{if(c!=null)c.disconnect();}}).start();
    }
    private void downloadUpdate(){
        if(update==null)return;download.setEnabled(false);check.setEnabled(false);progress.setProgress(0);status.setText("Starting download…");
        new Thread(()->{HttpURLConnection c=null;File part=null;try{String url=update.getString("url");long expected=update.optLong("size",-1);String expectedSha=update.getString("sha256").toLowerCase();File dir=new File(getExternalFilesDir(null),"updates");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create update directory");part=new File(dir,"lucid-update.zip.part");File target=new File(dir,"lucid-"+update.optString("version","update")+".zip");c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(true);if(c.getResponseCode()!=200)throw new IOException("ROM download HTTP "+c.getResponseCode());long total=c.getContentLengthLong();if(expected>0&&total>0&&total!=expected)throw new IOException("Server size differs from manifest");MessageDigest md=MessageDigest.getInstance("SHA-256");long count=0;try(InputStream in=new BufferedInputStream(c.getInputStream());OutputStream out=new BufferedOutputStream(new FileOutputStream(part))){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){out.write(b,0,n);md.update(b,0,n);count+=n;final int pct=(int)Math.min(100,count*100/Math.max(1,expected>0?expected:total));runOnUiThread(()->{progress.setProgress(pct);status.setText("Downloading • "+pct+"% • "+human(count));});}}if(expected>0&&count!=expected)throw new IOException("Downloaded size mismatch: "+count+" bytes");String actual=hex(md.digest());if(!actual.equals(expectedSha))throw new IOException("SHA-256 mismatch. Archive rejected.");if(target.exists()&&!target.delete())throw new IOException("Cannot replace previous archive");if(!part.renameTo(target))throw new IOException("Cannot finalize downloaded archive");runOnUiThread(()->{progress.setProgress(100);status.setText("Download verified. SHA-256: "+actual+"\nSaved: "+target.getAbsolutePath());new AlertDialog.Builder(this).setTitle("ROM verified").setMessage("The archive passed the manifest size and SHA-256 checks. To install, hand off to a compatible recovery/installer.").setPositiveButton("OK",null).show();download.setEnabled(true);check.setEnabled(true);});}catch(Exception e){if(part!=null&&part.exists())part.delete();String msg=e.getMessage();runOnUiThread(()->{status.setText("Download failed: "+msg);download.setEnabled(true);check.setEnabled(true);});}finally{if(c!=null)c.disconnect();}}).start();
    }
    private String hex(byte[] a){StringBuilder s=new StringBuilder();for(byte b:a)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
    private String human(long n){if(n<=0)return "size unknown";return String.format(java.util.Locale.ROOT,"%.1f GB",n/1073741824.0);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?1:0));return t;}
    private GradientDrawable shape(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w==0?-1:w,h==0?-2:h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}