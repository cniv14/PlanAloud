package app.planaloud;

import java.time.*;
import java.util.*;
import org.json.JSONObject;

/** Date-only background spans; no artificial appointment times are introduced. */
final class WeekSpans {
    static final class Span {final JSONObject record;final int first,last;int lane;Span(JSONObject r,int first,int last){record=r;this.first=first;this.last=last;}int titleDay(LocalDate week,LocalDate today){int day=(int)java.time.temporal.ChronoUnit.DAYS.between(week,today);return day>=first&&day<=last?day:-1;}}
    final List<Span> spans=new ArrayList<>();int lanes;
    static LocalDateTime end(JSONObject r){if("project".equals(r.optString("kind"))&&r.optString("end_at").isEmpty()&&!r.optString("due_date").isEmpty()){LocalDateTime due=PlanTime.deadline(r),start=PlanTime.start(r);if(start==null||due.isAfter(start))return due;}return PlanTime.end(r);}
    static boolean eligible(JSONObject r){try{if("project".equals(r.optString("kind"))&&r.optString("start_at").isEmpty())return true;if(r.optString("start_at").isEmpty())return false;LocalDateTime start=PlanTime.start(r),finish=end(r);if(finish==null)return false;return (r.optBoolean("all_day")||"project".equals(r.optString("kind")))&&Duration.between(start,finish).toMinutes()>=(r.optBoolean("all_day")?1441:1440);}catch(RuntimeException e){return false;}}
    WeekSpans(WeekSchedule week){
        Map<String,JSONObject> unique=new LinkedHashMap<>();for(JSONObject r:week.backgroundProjects)add(unique,r);for(int d=0;d<7;d++){for(JSONObject r:week.allDay.get(d))add(unique,r);for(List<JSONObject> slot:week.days.get(d))for(JSONObject r:slot)add(unique,r);}
        for(JSONObject r:unique.values())try{
            LocalDate first=r.optString("start_at").isEmpty()?week.first:PlanTime.start(r).toLocalDate();LocalDateTime finish=end(r);LocalDate last=finish==null?week.first.plusDays(6):(finish.toLocalTime().equals(LocalTime.MIDNIGHT)?finish.toLocalDate().minusDays(1):finish.toLocalDate());
            if(last.isBefore(week.first)||first.isAfter(week.first.plusDays(6)))continue;int from=(int)Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(week.first,first)),to=(int)Math.min(6,java.time.temporal.ChronoUnit.DAYS.between(week.first,last));if(to>=from)spans.add(new Span(r,from,to));
        }catch(RuntimeException ignored){}
        spans.sort(Comparator.comparingInt((Span s)->s.first).thenComparingInt(s->s.last).thenComparing(s->s.record.optString("id")));List<Integer> ends=new ArrayList<>();for(Span s:spans){int lane=0;while(lane<ends.size()&&ends.get(lane)>=s.first)lane++;if(lane==ends.size())ends.add(s.last);else ends.set(lane,s.last);s.lane=lane;}lanes=ends.size();
    }
    private static void add(Map<String,JSONObject> values,JSONObject r){if(eligible(r)&&r.optString("completed").isEmpty()&&!r.optBoolean("deleted"))values.put(r.optString("id",r.optString("title"))+"|"+r.optString("occurrence_date"),r);}
    boolean contains(int day){for(Span s:spans)if(s.first<=day&&day<=s.last)return true;return false;}
}
