package com.lucid.os.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Build;
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
import java.util.Locale;

/** Lucid OTA client: manifest validation, resumable-safe download, SHA-256 verification and explicit recovery handoff. */
public final class MainActivity extends Activity {
    private static final String MANIFEST="https://raw.githubusercontent.com/NAANG360/Lucid-OS/main/ota/stable.json";
    private final int bg=Color.rgb(12,13,19),card=Color.rgb(24,25,35),fg=Color.rgb(245,245,252),muted=Color.rgb(163,165,184),accent=Color.rgb(139,119,255);
    private TextView version,details,status; private ProgressBar progress; private Button check,download,install; private JSONObject update; private File verifiedZip;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);build();checkUpdate();}
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(22),dp(28),dp(22),dp(28));scroll.addView(page);
        page.addView(text("L U C I D   O S",11,accent,true));
        page.addView(text("System updater",32,fg,true),lp(0,dp(4),0,0,0,0));
        page.addView(text("Updates with verification before installation.",14,muted,false),lp(0,0,0,0,0,dp(22)));
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setPadding(dp(18),dp(18),dp(18),dp(18));hero.setBackground(shape(card,22));
        hero.addView(text("AVAILABLE BUILD",10,accent,true));version=text("Checking for updates…",22,fg,true);hero.addView(version,lp(0,dp(8),0,0,0,0));
        details=text("Target device: lavender",13,muted,false);hero.addView(details,lp(0,dp(8),0,0,0,0));page.addView(hero,lp(0,0,0,0,0,dp(18)));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);page.addView(progress,lp(0,dp(12),0,0,0,dp(8)));
        status=text("Fetching the public OTA manifest…",12,muted,false);page.addView(status,lp(0,0,0,0,0,dp(18)));
        check=button("CHECK FOR UPDATES");check.setOnClickListener(v->checkUpdate());page.addView(check,lp(0,dp(50),0,0,0,dp(8)));
        download=button("DOWNLOAD & VERIFY");download.setEnabled(false);download.setOnClickListener(v->downloadUpdate());page.addView(download,lp(0,dp(50),0,0,0,dp(8)));
        install=button("INSTALL VIA RECOVERY");install.setEnabled(false);install.setOnClickListener(v->confirmInstall());page.addView(install,lp(0,dp(50),0,0,0,dp(14)));
        TextView note=text("The archive is checked against the published size and SHA-256 before any install action. Installation requires a compatible root provider and TWRP/OrangeFox recovery. Nothing flashes silently; you confirm before reboot.",12,muted,false);page.addView(note);
        setContentView(scroll);
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private void checkUpdate(){
        check.setEnabled(false);download.setEnabled(false);install.setEnabled(false);status.setText("Fetching OTA manifest…");
        new Thread(()->{HttpURLConnection c=null;try{
            c=(HttpURLConnection)new URL(MANIFEST).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("Accept","application/json");
            if(c.getResponseCode()!=200)throw new IOException("Manifest HTTP "+c.getResponseCode());
            JSONObject j;try(InputStream in=c.getInputStream()){j=new JSONObject(readText(in));}
            if(!"lavender".equals(j.optString("device")))throw new IOException("Manifest target is not lavender");
            if(j.has("installable")&&!j.optBoolean("installable",false))throw new IOException("This ROM is marked non-installable pending device/metadata validation.");
            String url=j.getString("url"),sha=j.getString("sha256").toLowerCase(Locale.ROOT);
            if(!url.startsWith("https://")||sha.length()!=64||!sha.matches("[0-9a-f]{64}"))throw new IOException("Invalid HTTPS URL or SHA-256 in manifest");
            long size=j.optLong("size",-1);if(size<=0)throw new IOException("Manifest has no valid archive size");
            runOnUiThread(()->{update=j;version.setText(j.optString("version","Lucid build"));details.setText("Target: lavender  •  "+human(size));status.setText(j.optString("changelog","Update available."));download.setEnabled(true);check.setEnabled(true);});
        }catch(Exception e){runOnUiThread(()->{status.setText("Update check failed: "+e.getMessage());check.setEnabled(true);});}finally{if(c!=null)c.disconnect();}}).start();
    }
    private void downloadUpdate(){
        if(update==null)return;download.setEnabled(false);check.setEnabled(false);install.setEnabled(false);progress.setProgress(0);status.setText("Starting download…");
        new Thread(()->{HttpURLConnection c=null;File part=null;try{
            String url=update.getString("url"),expectedSha=update.getString("sha256").toLowerCase(Locale.ROOT);long expected=update.getLong("size");
            File dir=new File(getExternalFilesDir(null),"updates");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create update directory");
            part=new File(dir,"lucid-update.zip.part");File target=new File(dir,"lucid-"+safeName(update.optString("version","update"))+".zip");
            c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setInstanceFollowRedirects(true);
            if(c.getResponseCode()!=200)throw new IOException("ROM download HTTP "+c.getResponseCode());
            long total=c.getContentLengthLong();if(total>0&&total!=expected)throw new IOException("Server size differs from manifest");
            MessageDigest md=MessageDigest.getInstance("SHA-256");long count=0;
            try(InputStream in=new BufferedInputStream(c.getInputStream());OutputStream out=new BufferedOutputStream(new FileOutputStream(part))){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){out.write(b,0,n);md.update(b,0,n);count+=n;final long cur=count;final int pct=(int)Math.min(100,cur*100/expected);runOnUiThread(()->{progress.setProgress(pct);status.setText("Downloading • "+pct+"% • "+human(cur)+" / "+human(expected));});}}
            if(count!=expected)throw new IOException("Size mismatch: expected "+expected+" bytes, got "+count);
            String actual=hex(md.digest());if(!actual.equals(expectedSha))throw new IOException("SHA-256 mismatch; archive rejected.");
            if(target.exists()&&!target.delete())throw new IOException("Cannot replace previous archive");if(!part.renameTo(target))throw new IOException("Cannot finalize downloaded archive");
            verifiedZip=target;runOnUiThread(()->{progress.setProgress(100);status.setText("Verified • SHA-256 "+actual+"\nSaved in app update storage.");download.setEnabled(true);check.setEnabled(true);install.setEnabled(true);new AlertDialog.Builder(this).setTitle("ROM verified").setMessage("Size and SHA-256 both match the OTA manifest. You can now stage a recovery install.").setPositiveButton("OK",null).show();});
        }catch(Exception e){if(part!=null&&part.exists())part.delete();String msg=e.getMessage();runOnUiThread(()->{status.setText("Download failed: "+msg);download.setEnabled(true);check.setEnabled(true);});}finally{if(c!=null)c.disconnect();}}).start();
    }
    private void confirmInstall(){
        if(verifiedZip==null||!verifiedZip.isFile()){status.setText("Download and verify the ROM first.");return;}
        new AlertDialog.Builder(this).setTitle("Stage recovery installation?")
        .setMessage("This copies the verified ROM to shared storage, writes a TWRP/OrangeFox OpenRecoveryScript, and reboots to recovery. Make sure your recovery supports OpenRecoveryScript and you have a backup. The app will not wipe user data.")
        .setNegativeButton("CANCEL",null).setPositiveButton("STAGE & REBOOT",(d,w)->stageRecovery()).show();
    }
    private void stageRecovery(){
        install.setEnabled(false);status.setText("Checking root and staging recovery…");
        new Thread(()->{
            String path=verifiedZip.getAbsolutePath();
            String dest="/sdcard/Download/LucidOS-update.zip";
            String command="set -e; test -f "+shq(path)+"; command -v su >/dev/null 2>&1; mkdir -p /sdcard/Download /cache/recovery; cp "+shq(path)+" "+shq(dest)+"; chmod 0644 "+shq(dest)+"; printf '%s\\n' 'install "+dest+"' 'reboot' > /cache/recovery/openrecoveryscript; chmod 0644 /cache/recovery/openrecoveryscript; sync; echo LUCID_RECOVERY_STAGED";
            String result=runAsRoot(command);
            boolean ok=result.contains("LUCID_RECOVERY_STAGED");
            runOnUiThread(()->{
                if(!ok){status.setText("Could not stage recovery install. Root/recovery may be unavailable.\n"+result);install.setEnabled(true);return;}
                new AlertDialog.Builder(this).setTitle("Recovery install staged").setMessage("The verified ROM was copied and the recovery script was written. Reboot to recovery now?").setNegativeButton("NOT NOW",(a,b)->{status.setText("Staged. Reboot to TWRP/OrangeFox when ready.");install.setEnabled(true);}).setPositiveButton("REBOOT RECOVERY",(a,b)->rebootRecovery()).show();
            });
        }).start();
    }
    private void rebootRecovery(){new Thread(()->{String r=runAsRoot("reboot recovery");runOnUiThread(()->{status.setText("Recovery reboot request returned. "+r);install.setEnabled(true);});}).start();}
    private String runAsRoot(String command){
        Process p=null;try{p=Runtime.getRuntime().exec("su");try(DataOutputStream os=new DataOutputStream(p.getOutputStream())){os.writeBytes(command+" 2>&1\nexit\n");os.flush();}
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=p.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}p.waitFor();return out.toString("UTF-8");
        }catch(Exception e){return "ROOT_ERROR: "+e.getClass().getSimpleName()+": "+e.getMessage();}finally{if(p!=null)p.destroy();}
    }
    private String readText(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}
    private String shq(String s){return "'"+s.replace("'","'\\''")+"'";}
    private String safeName(String s){return s.replaceAll("[^A-Za-z0-9._-]","_");}
    private String hex(byte[] a){StringBuilder s=new StringBuilder();for(byte b:a)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    private String human(long n){if(n<=0)return "size unknown";return String.format(Locale.ROOT,"%.1f GB",n/1073741824.0);}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?1:0));return t;}
    private GradientDrawable shape(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}
    private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w==0?-1:w,h==0?-2:h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}