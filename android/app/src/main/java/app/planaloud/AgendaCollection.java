package app.planaloud;

import android.content.*;
import android.widget.*;
import java.time.LocalDate;
import org.json.*;

public final class AgendaCollection extends RemoteViewsService {
    public RemoteViewsFactory onGetViewFactory(Intent i){return new Rows(getApplicationContext(),i.getData()!=null&&"list".equals(i.getData().getLastPathSegment()));}
    private static final class Rows implements RemoteViewsFactory {
        final Context c;final boolean detailed;volatile JSONArray rows=new JSONArray();
        Rows(Context c,boolean detailed){this.c=c;this.detailed=detailed;}
        public void onCreate(){}
        public void onDataSetChanged(){try{rows=Data.get(c).dayTasks(LocalDate.now(),false);}catch(Exception e){rows=new JSONArray();}}
        public void onDestroy(){rows=new JSONArray();}
        public int getCount(){return rows.length();}
        public int getViewTypeCount(){return 1;}
        public long getItemId(int n){return n;}
        public boolean hasStableIds(){return false;}
        public RemoteViews getLoadingView(){return null;}
        public RemoteViews getViewAt(int n){
            JSONObject r=rows.optJSONObject(n);if(r==null)return null;boolean dark=AgendaWidget.dark(c);
            RemoteViews v=new RemoteViews(c.getPackageName(),detailed?R.layout.widget_item_detailed:R.layout.widget_item);
            java.time.LocalDateTime now=java.time.LocalDateTime.now();
            int shade;
            switch(PlanTime.priority(r,now)){
                case "near": shade=dark?R.drawable.widget_priority_high_dark:R.drawable.widget_priority_high; break;
                case "high": shade=dark?R.drawable.widget_priority_important_dark:R.drawable.widget_priority_important; break;
                case "urgent": shade=dark?R.drawable.widget_priority_normal_dark:R.drawable.widget_priority_normal; break;
                case "normal": shade=dark?R.drawable.widget_priority_low_dark:R.drawable.widget_priority_low; break;
                case "low": shade=dark?R.drawable.widget_priority_relaxed_dark:R.drawable.widget_priority_relaxed; break;
                default: shade=dark?R.drawable.widget_glass_dark:R.drawable.widget_glass;
            }
            v.setInt(R.id.widget_item,"setBackgroundResource",shade);
            v.setTextViewText(R.id.widget_item_title,r.optString("title"));String plan=PlanTime.planning(r);
            String meta=PlanTime.taskLabel(r,now.toLocalDate(),false,now);
            if(detailed){
                if(r.optString("start_at").isEmpty()&&!r.optString("due_date").isEmpty()&&!PlanTime.countdown(r,now).isEmpty())
                    meta+="\n截止 "+PlanningFields.deadlineLabel(r);
                if(!plan.isEmpty())meta+="\n"+plan;
                if(!r.optString("location").isEmpty())meta+="\n"+r.optString("location");
                if(!r.optString("content").isEmpty())meta+="\n"+r.optString("content");
            }else if(!plan.isEmpty())meta+=" · "+plan;
            v.setTextViewText(R.id.widget_item_meta,meta);
            v.setTextColor(R.id.widget_item_title,dark?0xffeef5fa:0xff1b2730);v.setTextColor(R.id.widget_item_meta,dark?0xffc8d7e4:0xff445664);v.setTextColor(R.id.widget_item_done,ThemeColors.accent(c,dark));
            Intent row=new Intent().putExtra("record_id",r.optString("id")).putExtra("series_id",r.optString("series_id")).putExtra("occurrence_date",r.optString("occurrence_date"));
            v.setOnClickFillInIntent(R.id.widget_item,row);v.setOnClickFillInIntent(R.id.widget_item_open,row);v.setOnClickFillInIntent(R.id.widget_item_done,new Intent(row).putExtra("complete",true));return v;
        }
    }
}
