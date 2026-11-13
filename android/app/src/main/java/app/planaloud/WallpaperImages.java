package app.planaloud;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;

final class WallpaperImages {
    private interface Source{InputStream open()throws IOException;}
    private static Bitmap decode(Source source,int limit)throws IOException{
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        try(InputStream in=source.open()){BitmapFactory.decodeStream(in,null,bounds);}
        if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IOException("无法读取图片");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>limit)options.inSampleSize*=2;
        try(InputStream in=source.open()){Bitmap image=BitmapFactory.decodeStream(in,null,options);if(image==null)throw new IOException("无法读取图片");return image;}
    }
    static Bitmap decode(File file,int limit){try{return decode(()->new FileInputStream(file),limit);}catch(IOException e){return null;}}
    static String add(Context c,File file,String name)throws Exception{Bitmap b=decode(()->new FileInputStream(file),1400);try{return WallpaperRules.add(c,name,b);}finally{b.recycle();}}
    static void importRule(Context c,Uri uri)throws Exception{
        String name="壁纸";try(android.database.Cursor r=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(r!=null&&r.moveToFirst()&&r.getString(0)!=null)name=r.getString(0);}catch(Exception ignored){}
        Bitmap b=decode(()->c.getContentResolver().openInputStream(uri),1400);try{WallpaperRules.add(c,name,b);}finally{b.recycle();}
    }
    static void importWidget(Context c,Uri uri)throws Exception{Bitmap b=decode(()->c.getContentResolver().openInputStream(uri),720);try{writeWidget(c,b);}finally{b.recycle();}}
    static void importWidget(Context c,File file)throws Exception{Bitmap b=decode(()->new FileInputStream(file),720);try{writeWidget(c,b);}finally{b.recycle();}}
    private static void writeWidget(Context c,Bitmap b)throws IOException{
        android.util.AtomicFile file=new android.util.AtomicFile(widget(c));FileOutputStream out=null;
        try{out=file.startWrite();if(!b.compress(Bitmap.CompressFormat.JPEG,86,out))throw new IOException("无法保存小组件背景");file.finishWrite(out);}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
        c.getSharedPreferences("settings",0).edit().putString("widget_background","image").apply();
    }
    static File widget(Context c){return new File(c.getFilesDir(),"widget-wallpaper.jpg");}
}
