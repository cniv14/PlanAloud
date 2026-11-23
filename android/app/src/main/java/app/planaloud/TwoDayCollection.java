package app.planaloud;
import android.content.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;

public final class TwoDayCollection extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent i){return new Rows(getApplicationContext(),i.getIntExtra("day_offset",0));}
    static final class Rows implements RemoteViewsFactory {
        final Context c;final int offset;List<JSONObject> rows=Collections.emptyList();LocalDate day;
        Rows(Context c,int offset){this.c=c;this.offset=offset==1?1:0;}
        public void onCreate(){}
        public void onDataSetChanged(){day=LocalDate.now().plusDays(offset);try{rows=DayAgenda.load(c,day);}catch(Exception error){Diagnostics.record(c,"two-day-widget",error);rows=Collections.emptyList();}}
        public void onDestroy(){rows=Collections.emptyList();}public int getCount(){return rows.size();}public int getViewTypeCount(){return 1;}public long getItemId(int p){return p;}public boolean hasStableIds(){return false;}public RemoteViews getLoadingView(){return null;}
        public RemoteViews getViewAt(int p){if(p<0||p>=rows.size())return null;JSONObject r=rows.get(p);RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_two_day_item);v.setTextViewText(R.id.two_day_item_title,r.optString("title"));v.setTextViewText(R.id.two_day_item_meta,DayAgenda.meta(r,day));v.setInt(R.id.two_day_stripe,"setBackgroundColor",DayEvents.color(r));
            Intent open=new Intent().putExtra("record_id",r.optString("id")).putExtra("series_id",r.optString("series_id")).putExtra("occurrence_date",r.optString("occurrence_date"));v.setOnClickFillInIntent(R.id.two_day_item,open);v.setOnClickFillInIntent(R.id.two_day_item_open,open);v.setOnClickFillInIntent(R.id.two_day_item_done,new Intent(open).putExtra("complete",true));v.setContentDescription(R.id.two_day_item_done,"完成「"+r.optString("title")+"」");return v;}
    }
}
