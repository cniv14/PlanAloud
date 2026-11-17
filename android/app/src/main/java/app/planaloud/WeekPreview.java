package app.planaloud;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.*;

/** Continuous day columns. All-day work is the backdrop for that day's timed appointments. */
final class WeekPreview {
    private static final String[] BANDS={"上午","下午","晚上","截止"};
    static void show(MainActivity a,LinearLayout page,LocalDate first,java.util.function.Consumer<LocalDate> change,Runnable refresh){
        LinearLayout controls=Ui.row(a);page.addView(controls);
        Ui.cell(a,controls,Ui.button(a,"‹ 上一周",false,()->change.accept(first.minusWeeks(1))));
        Ui.cell(a,controls,Ui.button(a,"回到今天",false,()->change.accept(LocalDate.now())));
        Ui.cell(a,controls,Ui.button(a,"下一周 ›",false,()->change.accept(first.plusWeeks(1))));
        page.addView(Ui.text(a,first+" — "+first.plusDays(6),20,true));
        page.addView(Ui.help(a,"在周表内向任意方向拖动 · 全天底色覆盖当天 · 点击安排查看详情"));
        List<List<List<JSONObject>>> days=new ArrayList<>();List<List<JSONObject>> allDay=new ArrayList<>();int[] counts=new int[4];int maxAll=0;
        for(int i=0;i<7;i++){
            LocalDate date=first.plusDays(i);List<List<JSONObject>> bands=new ArrayList<>();for(int n=0;n<4;n++)bands.add(new ArrayList<>());days.add(bands);List<JSONObject> full=new ArrayList<>();allDay.add(full);
            try{
                Data db=Data.get(a);Set<String> due=new HashSet<>();JSONArray scheduled=db.scheduled(date,date.plusDays(1),false),deadlines=db.deadlines(date);
                for(int n=0;n<scheduled.length();n++){
                    JSONObject r=scheduled.getJSONObject(n);if(!r.optString("completed").isEmpty()||"waiting".equals(r.optString("activation")))continue;
                    if(r.optBoolean("all_day")){full.add(r);continue;}
                    if(r.optString("start_at").isEmpty()){if(due.add(r.optString("id")))bands.get(3).add(r);continue;}
                    int hour=PlanTime.start(r).getHour();bands.get(hour<12?0:hour<18?1:2).add(r);
                }
                for(int n=0;n<deadlines.length();n++){JSONObject r=deadlines.getJSONObject(n);if(due.add(r.optString("id")))bands.get(3).add(r);}
            }catch(Exception e){a.message(e.getMessage());}
            for(int n=0;n<4;n++)counts[n]=Math.max(counts[n],bands.get(n).size());maxAll=Math.max(maxAll,full.size());
        }
        int width=Ui.dp(a,Math.max(126,Math.min(166,(a.getResources().getConfiguration().screenWidthDp-56)/2))),header=Ui.dp(a,76),fullHeight=Ui.dp(a,Math.max(64,Math.min(3,maxAll)*44+16+(maxAll>3?48:0)));
        int[] heights=new int[4];for(int i=0;i<4;i++)heights[i]=Ui.dp(a,Math.max(96,Math.min(4,counts[i])*82+(counts[i]>4?48:0)+12));
        TwoAxisScroll pan=new TwoAxisScroll(a);pan.setContentDescription("可横向和纵向拖动的周日程");LinearLayout columns=Ui.row(a);columns.setGravity(Gravity.TOP);pan.addView(columns);
        LinearLayout hours=Ui.column(a);columns.addView(hours,new LinearLayout.LayoutParams(Ui.dp(a,50),-2));hours.addView(label(a,"时段",true),new LinearLayout.LayoutParams(-1,header));hours.addView(label(a,"全天",false),new LinearLayout.LayoutParams(-1,fullHeight));for(int i=0;i<4;i++)hours.addView(label(a,BANDS[i],false),new LinearLayout.LayoutParams(-1,heights[i]));
        for(int i=0;i<7;i++){
            LocalDate date=first.plusDays(i);LinearLayout column=Ui.column(a);LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(width,-2);cell.setMargins(Ui.dp(a,3),0,Ui.dp(a,3),0);columns.addView(column,cell);
            TextView day=label(a,date.format(DateTimeFormatter.ofPattern("EEE M/d",Locale.CHINA))+" "+Holidays.label(date)+"\n"+shortForecast(a,date),true);day.setOnClickListener(v->a.openSchedule(date));
            if(date.equals(LocalDate.now())){day.setTextColor(Ui.primary(a));day.setTypeface(null,android.graphics.Typeface.BOLD);}column.addView(day,new LinearLayout.LayoutParams(-1,header));
            LinearLayout body=Ui.column(a);body.setTag(allDay.get(i).isEmpty()?"week-day":"week-all-day");
            if(!allDay.get(i).isEmpty()){
                GradientDrawable tint=new GradientDrawable();tint.setColor(LiquidGlass.tint(Ui.color(a,com.google.android.material.R.attr.colorTertiaryContainer),120));tint.setCornerRadius(Ui.dp(a,12));body.setBackground(tint);
            }
            column.addView(body);LinearLayout full=slot(a,fullHeight);body.addView(full);List<JSONObject> fullItems=allDay.get(i);
            for(int n=0;n<Math.min(3,fullItems.size());n++)addEntry(a,full,fullItems.get(n),"全天",true,date,refresh);
            if(fullItems.size()>3)full.addView(Ui.button(a,"更多全天安排",false,()->DailyTasks.show(a,date)));
            for(int band=0;band<4;band++){
                LinearLayout slot=slot(a,heights[band]);body.addView(slot);List<JSONObject> items=days.get(i).get(band);
                for(int n=0;n<Math.min(4,items.size());n++){JSONObject record=items.get(n);addEntry(a,slot,record,PlanTime.taskLabel(record,date,band==3,LocalDateTime.now()),false,date,refresh);}
                if(items.size()>4)slot.addView(Ui.button(a,"另有 "+(items.size()-4)+" 项",false,()->DailyTasks.show(a,date)));
            }
        }
        page.addView(pan,new LinearLayout.LayoutParams(-1,Ui.dp(a,Math.max(260,Math.min(560,a.getResources().getConfiguration().screenHeightDp-360)))));
        page.addView(Ui.help(a,"跨天事项按原开始时刻归类；没有日期的事项保留在今天的待安排区。"));
    }
    private static String shortForecast(MainActivity a,LocalDate date){String text=Forecast.summary(a,date);int cut=text.indexOf(" · ");return cut<0?text:text.substring(0,cut);}
    private static LinearLayout slot(MainActivity a,int height){LinearLayout slot=Ui.column(a);slot.setPadding(Ui.dp(a,5),Ui.dp(a,4),Ui.dp(a,5),Ui.dp(a,4));slot.setLayoutParams(new LinearLayout.LayoutParams(-1,height));return slot;}
    private static TextView label(MainActivity a,String text,boolean header){TextView view=Ui.text(a,text,header?13:12,header);view.setGravity(Gravity.CENTER);view.setPadding(Ui.dp(a,3),Ui.dp(a,6),Ui.dp(a,3),Ui.dp(a,6));return view;}
    private static void addEntry(MainActivity a,LinearLayout slot,JSONObject r,String when,boolean full,LocalDate date,Runnable refresh){
        TextView entry=Ui.text(a,(full?"":when+"\n")+r.optString("title"),13,true);entry.setMaxLines(full?2:3);entry.setEllipsize(android.text.TextUtils.TruncateAt.END);entry.setGravity(Gravity.CENTER_VERTICAL);entry.setPadding(Ui.dp(a,7),Ui.dp(a,5),Ui.dp(a,7),Ui.dp(a,5));
        if(!full){GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(Ui.dp(a,9));int priority=Ui.priorityColor(a,PlanTime.priority(r,LocalDateTime.now()));shape.setColor(priority!=0?priority:Ui.color(a,com.google.android.material.R.attr.colorSecondaryContainer));entry.setBackground(shape);}
        slot.addView(entry,new LinearLayout.LayoutParams(-1,Ui.dp(a,full?44:78)));entry.setContentDescription((full?"全天 · ":when+" · ")+r.optString("title"));entry.setOnClickListener(v->a.detail(r));entry.setOnLongClickListener(v->{TaskQuickActions.menu(a,entry,r,refresh);return true;});
    }
}
