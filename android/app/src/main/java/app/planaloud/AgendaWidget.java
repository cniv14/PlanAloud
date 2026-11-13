package app.planaloud;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.json.*;

public final class AgendaWidget extends AppWidgetProvider {
    private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final java.util.concurrent.atomic.AtomicBoolean queued=new java.util.concurrent.atomic.AtomicBoolean();
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("settings",0);}
    static boolean dark(Context c){String value=prefs(c).getString("theme","system");return value.equals("dark")||value.equals("system")&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
    public static void refresh(Context context){
        Context c=context.getApplicationContext();if(!queued.compareAndSet(false,true))return;
        worker.execute(()->{queued.set(false);AppWidgetManager m=AppWidgetManager.getInstance(c);update(c,m,m.getAppWidgetIds(new ComponentName(c,AgendaWidget.class)));});
    }
    private static PendingIntent open(Context c,JSONObject r,boolean quick){
        String id=r==null?"":r.optString("id");Intent i=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(r!=null)i.putExtra("record_id",id).putExtra("series_id",r.optString("series_id")).putExtra("occurrence_date",r.optString("occurrence_date"));
        if(quick)i.putExtra("quick",true).putExtra("quick_kind","task");i.setData(Uri.parse("planaloud://widget/"+(quick?"new":id.isEmpty()?"home":id)));
        return PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){onUpdate(c,m,new int[]{id});}
    public void onUpdate(Context c,AppWidgetManager m,int[] ids){PendingResult pending=goAsync();worker.execute(()->{try{update(c,m,ids);}finally{if(pending!=null)pending.finish();}});}
    private static void update(Context c,AppWidgetManager manager,int[] ids){
        if(ids.length==0)return;boolean dark=dark(c);int ink=dark?0xffedf3f8:0xff1f2830,muted=dark?0xffc5d3de:0xff465765,accent=ThemeColors.accent(c,dark);LocalDate today=LocalDate.now();
        String style=prefs(c).getString("widget_background","glass");
        String weather=OpenWeatherSources.cached(c,"");
        Bitmap background=style.equals("solid")?null:glassBackground(c,style,dark,accent);
        JSONObject notice=null;int count=0;try{if(prefs(c).getBoolean("widget_inbox",true)){count=Data.get(c).inboxCount();notice=Data.get(c).list("inbox",1).optJSONObject(0);}}catch(Exception ignored){}
        for(int id:ids){
            try{
                boolean grid="grid".equals(prefs(c).getString("widget_layout","grid"))&&manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,240)>=205;
                int container=grid?R.id.widget_grid:R.id.widget_list;
                RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
                v.setInt(R.id.widget_root,"setBackgroundResource",background==null?(dark?R.drawable.widget_solid_dark:R.drawable.widget_solid):android.R.color.transparent);
                v.setViewVisibility(R.id.widget_background_image,background==null?View.GONE:View.VISIBLE);if(background!=null)v.setImageViewBitmap(R.id.widget_background_image,background);
                v.setViewVisibility(R.id.widget_list,grid?View.GONE:View.VISIBLE);v.setViewVisibility(R.id.widget_grid,grid?View.VISIBLE:View.GONE);
                Intent adapter=new Intent(c,AgendaCollection.class).setData(Uri.parse("planaloud://widget/rows/"+id+"/"+(grid?"grid":"list")));
                v.setRemoteAdapter(container,adapter);v.setEmptyView(container,R.id.widget_empty);
                Intent tap=new Intent(c,WidgetActionActivity.class).setData(Uri.parse("planaloud://widget/action/"+id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.setPendingIntentTemplate(container,PendingIntent.getActivity(c,id,tap,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE));
                v.setTextViewText(R.id.widget_date,today.format(DateTimeFormatter.ofPattern("M月d日 EEE",Locale.CHINA)));v.setTextColor(R.id.widget_date,ink);
                v.setTextViewText(R.id.widget_weather,weather.isEmpty()?"今日待办 · 上下滑动":weather);v.setTextColor(R.id.widget_weather,muted);
                v.setTextColor(R.id.widget_empty,muted);v.setTextColor(R.id.widget_add,accent);v.setTextColor(R.id.widget_inbox,ink);
                v.setViewVisibility(R.id.widget_inbox,notice==null?View.GONE:View.VISIBLE);if(notice!=null){v.setTextViewText(R.id.widget_inbox,"收集箱 "+count+" 条 · "+notice.optString("title"));v.setOnClickPendingIntent(R.id.widget_inbox,open(c,notice,false));}
                v.setOnClickPendingIntent(R.id.widget_add,open(c,null,true));v.setOnClickPendingIntent(R.id.widget_header,open(c,null,false));v.setOnClickPendingIntent(R.id.widget_empty,open(c,null,true));
                manager.updateAppWidget(id,v);manager.notifyAppWidgetViewDataChanged(id,container);
            }catch(RuntimeException ignored){}
        }
    }
    private static Bitmap glassBackground(Context c,String style,boolean dark,int accent){
        File file=style.equals("image")?WallpaperImages.widget(c):
                style.equals("wallpaper")?WallpaperRules.selected(c):null;
        Bitmap source=file==null?null:WallpaperImages.decode(file,250);
        int w=300,h=220;
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(out);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        RectF area=new RectF(0,0,w,h);
        Path clip=new Path();
        clip.addRoundRect(area,17,17,Path.Direction.CW);
        canvas.clipPath(clip);

        if(source!=null){
            float scale=Math.max((float)w/source.getWidth(),(float)h/source.getHeight());
            float bw=source.getWidth()*scale,bh=source.getHeight()*scale;
            paint.setAlpha(146);
            canvas.drawBitmap(source,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);
            source.recycle();
        }

        int mask=Math.max(48,Math.min(94,prefs(c).getInt("widget_tint",73)))*255/100;
        int base=dark?0xff17222e:0xfff2f8fb;
        int shade=androidx.core.graphics.ColorUtils.blendARGB(base,accent,.12f);
        paint.setColor(Color.WHITE);
        paint.setShader(new LinearGradient(0,0,w,h,
                LiquidGlass.tint(base,mask),LiquidGlass.tint(shade,mask),Shader.TileMode.CLAMP));
        canvas.drawRect(area,paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setShader(new LinearGradient(0,0,w,h,dark?0x8ad9f2ff:0xcaffffff,
                LiquidGlass.tint(accent,112),Shader.TileMode.CLAMP));
        canvas.drawRoundRect(1,1,w-1,h-1,17,17,paint);
        return out;
    }
}
