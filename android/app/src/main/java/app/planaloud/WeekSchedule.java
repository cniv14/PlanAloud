package app.planaloud;

import android.content.Context;
import java.time.*;
import java.util.*;
import org.json.*;

/** The same complete week snapshot feeds the viewport and the PNG export. */
final class WeekSchedule {
    static final String[] BANDS={"上午","下午","晚上","截止"};
    final LocalDate first;
    final List<List<List<JSONObject>>> days=new ArrayList<>();
    final List<List<JSONObject>> allDay=new ArrayList<>();
    final int[] counts=new int[4];int maxAll;
    WeekSchedule(LocalDate first){this.first=first;for(int d=0;d<7;d++){List<List<JSONObject>> bands=new ArrayList<>();for(int b=0;b<4;b++)bands.add(new ArrayList<>());days.add(bands);allDay.add(new ArrayList<>());}}
    static WeekSchedule load(Context c,LocalDate first)throws Exception{
        WeekSchedule week=new WeekSchedule(first);Data db=Data.get(c);
        for(int d=0;d<7;d++){
            LocalDate date=first.plusDays(d);Set<String> due=new HashSet<>();JSONArray scheduled=db.scheduled(date,date.plusDays(1),false),deadlines=db.deadlines(date);
            for(int n=0;n<scheduled.length();n++){
                JSONObject r=scheduled.getJSONObject(n);if(!r.optString("completed").isEmpty()||"waiting".equals(r.optString("activation")))continue;
                if(r.optBoolean("all_day")){week.allDay.get(d).add(r);continue;}
                if(r.optString("start_at").isEmpty()){if(due.add(r.optString("id")))week.days.get(d).get(3).add(r);continue;}
                int hour=PlanTime.start(r).getHour();week.days.get(d).get(hour<12?0:hour<18?1:2).add(r);
            }
            for(int n=0;n<deadlines.length();n++){JSONObject r=deadlines.getJSONObject(n);if(due.add(r.optString("id")))week.days.get(d).get(3).add(r);}
        }
        week.count();return week;
    }
    void count(){for(int d=0;d<7;d++){maxAll=Math.max(maxAll,allDay.get(d).size());for(int b=0;b<4;b++)counts[b]=Math.max(counts[b],days.get(d).get(b).size());}}
    static WeekSchedule example(LocalDate first)throws Exception{
        WeekSchedule week=new WeekSchedule(first);
        week.allDay.get(0).add(new JSONObject().put("title","项目准备 · 全天"));
        week.days.get(0).get(0).add(new JSONObject().put("title","整理资料").put("start_at",first.atTime(9,0).toString()).put("end_at",first.atTime(10,0).toString()));
        week.days.get(2).get(1).add(new JSONObject().put("title","小组讨论").put("start_at",first.plusDays(2).atTime(14,0).toString()).put("end_at",first.plusDays(2).atTime(15,0).toString()));week.count();return week;
    }
}
