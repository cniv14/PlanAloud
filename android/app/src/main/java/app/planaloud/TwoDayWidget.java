package app.planaloud;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A separately registered, two-column today/tomorrow widget. */
public final class TwoDayWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){PendingResult pending=goAsync();Context app=c.getApplicationContext();AgendaWidget.worker.execute(()->{try{update(app,manager,ids);AgendaWidget.schedule(app,manager);}finally{if(pending!=null)pending.finish();}});}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle opts){onUpdate(c,m,new int[]{id});}
    @Override public void onDisabled(Context c){AgendaWidget.schedule(c,AppWidgetManager.getInstance(c));super.onDisabled(c);}
    @Override public void onReceive(Context c,Intent i){if(Intent.ACTION_DATE_CHANGED.equals(i.getAction())||Intent.ACTION_TIME_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction())){AgendaWidget.refresh(c);return;}super.onReceive(c,i);}
    static void update(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)try{
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_two_day);LocalDate today=LocalDate.now();v.setTextViewText(R.id.two_day_date,today.format(DateTimeFormatter.ofPattern("MM.dd  EEEE",Locale.CHINA)));
        Intent home=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).setData(Uri.parse("planaloud://two-day/home/"+id));v.setOnClickPendingIntent(R.id.two_day_date,PendingIntent.getActivity(c,id,home,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        int[] lists={R.id.two_day_today_list,R.id.two_day_tomorrow_list},empty={R.id.two_day_today_empty,R.id.two_day_tomorrow_empty};
        for(int d=0;d<2;d++){Intent adapter=new Intent(c,TwoDayCollection.class).putExtra("day_offset",d).setData(Uri.parse("planaloud://two-day/"+id+"/"+d));v.setRemoteAdapter(lists[d],adapter);v.setEmptyView(lists[d],empty[d]);Intent action=new Intent(c,WidgetActions.class).setData(Uri.parse("planaloud://two-day/action/"+id+"/"+d));v.setPendingIntentTemplate(lists[d],PendingIntent.getBroadcast(c,id*2+d,action,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE));}
        manager.updateAppWidget(id,v);for(int list:lists)manager.notifyAppWidgetViewDataChanged(id,list);
    }catch(RuntimeException error){Diagnostics.record(c,"two-day-widget",error);}}
}
