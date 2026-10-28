package com.lucid.os.settings;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.content.SharedPreferences;
/** Read-only cross-app source of Lucid appearance tokens. */
public final class AppearanceProvider extends ContentProvider {
 public static final String AUTHORITY = "com.lucid.os.settings.appearance";
 public static final Uri PROFILE_URI = Uri.parse("content://" + AUTHORITY + "/profile");
 private static final String[] COLUMNS = {"accent","bubbles","rounded","compact","reduce_motion","profile"};
 @Override public boolean onCreate(){return true;}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sortOrder){
  if(getContext()==null||uri==null||!"profile".equals(uri.getLastPathSegment()))return null;
  SharedPreferences p=getContext().getSharedPreferences("lucid_appearance",0);
  int accent=p.getInt("accent",0xff8b77ff);
  String[] cols=projection==null?COLUMNS:projection; MatrixCursor c=new MatrixCursor(cols);
  Object[] row={accent,p.getBoolean("bubbles",true)?1:0,p.getBoolean("rounded",true)?1:0,p.getBoolean("compact",false)?1:0,p.getBoolean("reduce_motion",false)?1:0,profileName(accent)};
  java.util.Map<String,Object> map=new java.util.HashMap<>(); for(int i=0;i<COLUMNS.length;i++)map.put(COLUMNS[i],row[i]);
  Object[] selected=new Object[cols.length]; for(int i=0;i<cols.length;i++)selected[i]=map.get(cols[i]); c.addRow(selected); return c;
 }
 private String profileName(int a){if(a==0xff0866ff)return "Blue Pulse";if(a==0xff00afc8)return "Cyan Horizon";return "Violet Dusk";}
 @Override public String getType(Uri uri){return "vnd.android.cursor.item/vnd.lucid.appearance";}
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("read only");}
 @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException("read only");}
 @Override public int update(Uri uri,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("read only");}
}