package app.planaloud;
import org.json.*;
import java.time.*;

/** Date-only deadlines do not become fabricated appointments. */
public final class PlanTime {
    public static LocalDateTime start(JSONObject r){String value=r.optString("start_at");if(!value.isEmpty())return LocalDateTime.parse(value);value=r.optString("due_date");return value.isEmpty()?null:LocalDate.parse(value).atTime(r.optString("due_time").isEmpty()?LocalTime.of(9,0):deadlineTime(r));}
    private static LocalTime deadlineTime(JSONObject r){try{return LocalTime.parse(r.optString("due_time"));}catch(DateTimeException e){return LocalTime.of(9,0);}}
    public static LocalDateTime end(JSONObject r){String value=r.optString("end_at");if(!value.isEmpty())return LocalDateTime.parse(value);LocalDateTime start=start(r);return start==null?null:r.optString("start_at").isEmpty()?start.toLocalDate().plusDays(1).atStartOfDay():start.plusHours(1);}
    public static String label(JSONObject r){if(r.optString("start_at").isEmpty())return r.optString("due_date").isEmpty()?r.optString("time_hint","时间待定"):"截止 "+PlanningFields.deadlineLabel(r);return r.optBoolean("all_day")?r.optString("start_at").substring(0,10)+(r.optString("end_at").isEmpty()||!LocalDateTime.parse(r.optString("end_at")).toLocalDate().isAfter(LocalDateTime.parse(r.optString("start_at")).toLocalDate().plusDays(1))?"":" 至 "+LocalDateTime.parse(r.optString("end_at")).toLocalDate().minusDays(1))+" · 全天":r.optString("start_at").replace('T',' ');}
    public static String dailyLabel(JSONObject r,LocalDate day){
        String label;
        if(r.optString("start_at").isEmpty()){label=r.optString("due_date").isEmpty()?r.optString("time_hint"):"截止 "+PlanningFields.deadlineLabel(r);if(label.isEmpty())label="时间待定";}
        else {LocalDateTime start=start(r);label=r.optBoolean("all_day")?"全天":start.format(java.time.format.DateTimeFormatter.ofPattern(start.toLocalDate().equals(day)?"HH:mm":"M/d HH:mm"));if(!r.optBoolean("all_day")&&!r.optString("end_at").isEmpty()){LocalDateTime end=LocalDateTime.parse(r.optString("end_at"));label+=" – "+end.format(java.time.format.DateTimeFormatter.ofPattern(end.toLocalDate().equals(day)?"HH:mm":"M/d HH:mm"));}}
        return ("project".equals(r.optString("kind"))?"长任务 · ":"course".equals(r.optString("kind"))?"课程 · ":"")+label;
    }
    static boolean active(JSONObject r){
        return r.optString("completed").isEmpty()&&!cancelled(r);
    }
    static LocalDateTime deadline(JSONObject r){
        if(r.optString("due_date").isEmpty())return null;
        LocalDate date=LocalDate.parse(r.optString("due_date"));
        return r.optString("due_time").isEmpty()?date.plusDays(1).atStartOfDay():date.atTime(deadlineTime(r));
    }
    static String countdown(JSONObject r,LocalDateTime now){
        LocalDateTime end=deadline(r);
        if(end==null||!active(r))return "";
        Duration delta=Duration.between(now,end);
        if(delta.isZero())return "已截止";
        boolean overdue=delta.isNegative();
        Duration distance=delta.abs();
        long minutes=distance.toMinutes();
        String span=minutes>=1440?(minutes/1440)+"天"+(minutes%1440/60)+"小时":
                minutes>=60?(minutes/60)+"小时"+(minutes%60)+"分钟":
                minutes>0?minutes+"分钟":"不到1分钟";
        return overdue?"已逾期 "+span:"距截止 "+span;
    }
    static String priority(JSONObject r,LocalDateTime now){
        if(!active(r))return "";
        LocalDateTime end=deadline(r);
        if(end!=null&&!end.isAfter(now.plusDays(1)))return "near";
        String priority=r.optString("priority");
        if(!priority.isEmpty())return priority;
        return end!=null&&end.isAfter(now.plusDays(7))?"low":
                "task".equals(r.optString("kind"))||"project".equals(r.optString("kind"))?"normal":"";
    }
    static String taskLabel(JSONObject r,LocalDate day,boolean deadlineOnly,LocalDateTime now){
        String countdown=countdown(r,now);
        if(deadlineOnly||r.optString("start_at").isEmpty()&&!countdown.isEmpty())
            return countdown.isEmpty()?"截止 "+PlanningFields.deadlineLabel(r):countdown;
        return dailyLabel(r,day)+(countdown.isEmpty()?"":" · "+countdown);
    }
    public static String planning(JSONObject r){
        String due=r.optString("due_date"),priority=r.optString("priority");int minutes=r.optInt("estimate_minutes");
        java.util.ArrayList<String> labels=new java.util.ArrayList<>();
        if(!due.isEmpty()&&!r.optString("start_at").isEmpty())labels.add("截止 "+PlanningFields.deadlineLabel(r));
        if(minutes>0)labels.add("预计 "+minutes+" 分钟");
        if("high".equals(priority))labels.add("重要");else if("urgent".equals(priority))labels.add("紧急");else if("low".equals(priority))labels.add("低优先");else if("normal".equals(priority))labels.add("普通优先");
        return String.join(" · ",labels);
    }
    public static boolean cancelled(JSONObject r){return "cancelled".equals(r.optString("status"));}
}
