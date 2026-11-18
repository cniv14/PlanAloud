package app.planaloud;

import java.time.*;
import java.util.*;
import org.json.JSONObject;

/** Minute-accurate geometry shared by the interactive week and PNG export. */
final class WeekGeometry {
    static final class Timed {
        final JSONObject record;final int start,end;int lane,lanes=1;
        Timed(JSONObject record,int start,int end){this.record=record;this.start=start;this.end=end;}
    }
    final WeekSchedule week;final List<List<Timed>> days=new ArrayList<>();final List<List<JSONObject>> deadlines=new ArrayList<>();
    int firstMinute=480,lastMinute=1320;
    WeekGeometry(WeekSchedule week){
        this.week=week;
        for(int d=0;d<7;d++){
            LocalDateTime midnight=week.first.plusDays(d).atStartOfDay();List<Timed> timed=new ArrayList<>();days.add(timed);List<JSONObject> due=new ArrayList<>();deadlines.add(due);
            for(int b=0;b<3;b++)for(JSONObject r:week.days.get(d).get(b))try{int from=(int)Math.max(0,Duration.between(midnight,PlanTime.start(r)).toMinutes()),to=(int)Math.min(1440,Duration.between(midnight,PlanTime.end(r)).toMinutes());if(to<=from)continue;timed.add(new Timed(r,from,to));firstMinute=Math.min(firstMinute,from/60*60);lastMinute=Math.max(lastMinute,(to+59)/60*60);}catch(RuntimeException ignored){}
            for(JSONObject r:week.days.get(d).get(3))if(r.optString("start_at").isEmpty())due.add(r);
            timed.sort(Comparator.comparingInt((Timed t)->t.start).thenComparingInt(t->t.end));
            List<Timed> group=new ArrayList<>();List<Integer> ends=new ArrayList<>();int groupEnd=-1;
            for(Timed event:timed){
                if(event.start>=groupEnd){for(Timed old:group)old.lanes=ends.size();group.clear();ends.clear();groupEnd=-1;}
                int lane=0;while(lane<ends.size()&&ends.get(lane)>event.start)lane++;if(lane==ends.size())ends.add(event.end);else ends.set(lane,event.end);event.lane=lane;group.add(event);groupEnd=Math.max(groupEnd,event.end);
            }
            for(Timed old:group)old.lanes=ends.size();
        }
    }
    int minutes(){return lastMinute-firstMinute;}
    float top(Timed event,float pixelsPerMinute){return (event.start-firstMinute)*pixelsPerMinute;}
    float height(Timed event,float pixelsPerMinute){return (event.end-event.start)*pixelsPerMinute;}
}
