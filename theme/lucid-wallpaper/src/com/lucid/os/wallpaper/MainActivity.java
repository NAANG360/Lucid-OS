package com.lucid.os.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.IOException;

public final class MainActivity extends Activity {
    private final int bg=Color.rgb(12,13,19), fg=Color.rgb(246,244,252), muted=Color.rgb(166,164,188);
    private final String[] names={"Violet Dusk","Bubbles","Blue Pulse","Cyan Horizon"};
    private final int[][] palettes={
        {Color.rgb(22,16,48),Color.rgb(119,75,205),Color.rgb(207,160,255)},
        {Color.rgb(14,20,40),Color.rgb(92,68,170),Color.rgb(227,193,255)},
        {Color.rgb(5,23,66),Color.rgb(8,102,255),Color.rgb(118,191,255)},
        {Color.rgb(5,38,48),Color.rgb(0,175,200),Color.rgb(160,255,237)}
    };
    private int selected=0;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);render();}
    private void render(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(20),dp(20),dp(30));scroll.addView(page);
        TextView brand=text("L U C I D   O S",11,Color.rgb(196,151,255),true);page.addView(brand);
        TextView title=text("Wallpapers",32,fg,true);page.addView(title);
        page.addView(text("Atmosphere for your screen.",14,muted,false));
        for(int i=0;i<names.length;i++){
            final int ix=i;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(shape(Color.rgb(24,25,35),20));
            Preview preview=new Preview(this,palettes[i],i==1);card.addView(preview,new LinearLayout.LayoutParams(-1,dp(190)));
            TextView label=text(names[i],17,fg,true);label.setPadding(dp(3),dp(10),dp(3),dp(7));card.addView(label);
            TextView apply=text(i==selected?"SELECTED  ✓":"PREVIEW & APPLY  ↗",12,Color.rgb(196,151,255),true);apply.setGravity(Gravity.CENTER);apply.setPadding(dp(12),dp(12),dp(12),dp(12));apply.setBackground(shape(Color.rgb(39,39,51),14));apply.setOnClickListener(v->{selected=ix;applyWallpaper(ix);});card.addView(apply);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);page.addView(card,lp);
        }
        setContentView(scroll);
    }
    private void applyWallpaper(int index){
        try { int w=getResources().getDisplayMetrics().widthPixels;int h=(int)(w*1.8f);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);drawWallpaper(c,w,h,palettes[index],index==1);WallpaperManager.getInstance(this).setBitmap(bitmap);Toast.makeText(this,"Wallpaper applied.",Toast.LENGTH_SHORT).show();render(); }
        catch(IOException|RuntimeException e){Toast.makeText(this,"Could not apply wallpaper: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void drawWallpaper(Canvas c,int w,int h,int[] colors,boolean bubbles){
        Paint p=new Paint(3);p.setShader(new LinearGradient(0,0,w,h,colors,new float[]{0,.52f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        if(bubbles){int[][] circles={{w/4,h/4,w/5},{w*3/4,h/3,w/4},{w/2,h*2/3,w/3},{w/5,h*4/5,w/6}};for(int[] a:circles){p.setShader(new RadialGradient(a[0],a[1],a[2],new int[]{0x88FFFFFF,0x22D7C6FF,0x009B80FF},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawCircle(a[0],a[1],a[2],p);}p.setShader(null);}
        p.setColor(0x22FFFFFF);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w/240f));c.drawCircle(w*.72f,h*.22f,w*.19f,p);c.drawCircle(w*.72f,h*.22f,w*.14f,p);p.setStyle(Paint.Style.FILL);
    }
    private final class Preview extends View{final int[] colors;final boolean bubbles;final Paint p=new Paint(3);Preview(android.content.Context c,int[] cs,boolean b){super(c);colors=cs;bubbles=b;}protected void onDraw(Canvas c){super.onDraw(c);drawWallpaper(c,getWidth(),getHeight(),colors,bubbles);}}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}