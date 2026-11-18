package app.planaloud;
import android.content.Context;
import java.time.*;
import java.util.*;
import org.json.*;

/** Dated appointments shared by the two-day widget and timetable home. */
final class DayAgenda {
    static List<JSONObject> load(Context c,LocalDate date)throws Exception{
        List<JSONObject> rows=new ArrayList<>();Set<String> seen=new HashSet<>();Data db=Data.get(c);
        for(JSONArray source:new JSONArray[]{db.scheduled(date,date.plusDays(1),false),db.deadlines(date)})for(int i=0;i<source.length();i++){
            JSONObject r=source.getJSONObject(i);if(r.optBoolean("deleted")||r.optBoolean("archived")||!r.optString("completed").isEmpty()||"waiting".equals(r.optString("activation"))||PlanTime.cancelled(r))continue;
            if(seen.add(r.optString("id")+"|"+r.optString("occurrence_date")))rows.add(r);
        }
        rows.sort(Comparator.comparing((JSONObject r)->PlanTime.start(r),Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(r->r.optString("title")));return rows;
    }
    static String meta(JSONObject r,LocalDate date){String when=r.optString("start_at").isEmpty()&&r.optString("due_date").isEmpty()?"时间待定":PlanTime.dailyLabel(r,date);String location=r.optString("location");return when+(location.isEmpty()?"":"  "+location);}
}
