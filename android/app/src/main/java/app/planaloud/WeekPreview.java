package app.planaloud;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.*;

/** Seven date columns with aligned, variable-height rows. Floating work is not repeated in every cell. */
final class WeekPreview {
    private static final String[] BANDS={"全天","上午","下午","晚上","截止"};
    static void show(MainActivity a,LinearLayout page,LocalDate first,java.util.function.Consumer<LocalDate> change,Runnable refresh){
        LinearLayout controls=Ui.row(a);page.addView(controls);
        Ui.cell(a,controls,Ui.button(a,"‹ 上一周",false,()->change.accept(first.minusWeeks(1))));
        Ui.cell(a,controls,Ui.button(a,"回到今天",false,()->change.accept(LocalDate.now())));
        Ui.cell(a,controls,Ui.button(a,"下一周 ›",false,()->change.accept(first.plusWeeks(1))));
        page.addView(Ui.text(a,first+" — "+first.plusDays(6),20,true));
        page.addView(Ui.help(a,"横向滑动查看七天 · 点击事项打开详情 · 长按打开操作菜单"));
        HorizontalScrollView scroller=new HorizontalScrollView(a);scroller.setFillViewport(true);scroller.setHorizontalScrollBarEnabled(true);
        scroller.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN)v.getParent().requestDisallowInterceptTouchEvent(true);return false;});
        TableLayout table=new TableLayout(a);scroller.addView(table);page.addView(scroller,new LinearLayout.LayoutParams(-1,-2));
        int width=Ui.dp(a,Math.max(108,Math.min(155,(a.getResources().getConfiguration().screenWidthDp-86)/3)));
        TableRow header=new TableRow(a);table.addView(header);heading(a,header,"时段",Ui.dp(a,54),false,null);
        List<List<List<JSONObject>>> days=new ArrayList<>();
        for(int i=0;i<7;i++){
            LocalDate date=first.plusDays(i);
            String label=date.format(DateTimeFormatter.ofPattern("EEE M/d",Locale.CHINA))+" "+Holidays.label(date);
            String forecast=Forecast.summary(a,date);if(forecast.contains(" · "))forecast=forecast.substring(0,forecast.indexOf(" · "));
            heading(a,header,label.trim()+"\n"+forecast,width,date.equals(LocalDate.now()),()->a.openSchedule(date));
            List<List<JSONObject>> bands=new ArrayList<>();for(int b=0;b<BANDS.length;b++)bands.add(new ArrayList<>());days.add(bands);
            try{
                Data db=Data.get(a);JSONArray scheduled=db.scheduled(date,date.plusDays(1),false),deadlines=db.deadlines(date);Set<String> dueIds=new HashSet<>();
                for(int n=0;n<scheduled.length();n++){JSONObject r=scheduled.getJSONObject(n);if(!r.optString("completed").isEmpty()||"waiting".equals(r.optString("activation")))continue;
                    String start=r.optString("start_at");if(start.isEmpty()){if(dueIds.add(r.optString("id")))bands.get(4).add(r);continue;}
                    int hour=PlanTime.start(r).getHour();bands.get(r.optBoolean("all_day")?0:hour<12?1:hour<18?2:3).add(r);
                }
                for(int n=0;n<deadlines.length();n++){JSONObject r=deadlines.getJSONObject(n);if(dueIds.add(r.optString("id")))bands.get(4).add(r);}
            }catch(Exception e){a.message(e.getMessage());}
        }
        for(int band=0;band<BANDS.length;band++){
            TableRow row=new TableRow(a);table.addView(row);heading(a,row,BANDS[band],Ui.dp(a,54),false,null);
            for(int i=0;i<7;i++){
                LocalDate date=first.plusDays(i);LinearLayout cell=Ui.column(a);cell.setPadding(Ui.dp(a,4),Ui.dp(a,3),Ui.dp(a,4),Ui.dp(a,6));cell.setMinimumHeight(Ui.dp(a,64));
                if((i+band)%2==0)cell.setBackgroundColor(Ui.color(a,com.google.android.material.R.attr.colorSurfaceContainerLow));
                row.addView(cell,new TableRow.LayoutParams(width,-1));
                List<JSONObject> items=days.get(i).get(band);
                if(items.isEmpty()){TextView dash=Ui.help(a,"—");dash.setGravity(Gravity.CENTER);cell.addView(dash);continue;}
                for(int n=0;n<Math.min(4,items.size());n++){JSONObject r=items.get(n);
                    String when=PlanTime.taskLabel(r,date,band==4,LocalDateTime.now());
                    TextView entry=Ui.text(a,when+"\n"+r.optString("title"),13,true);entry.setMaxLines(3);entry.setEllipsize(android.text.TextUtils.TruncateAt.END);entry.setMinimumHeight(Ui.dp(a,48));entry.setPadding(Ui.dp(a,7),Ui.dp(a,5),Ui.dp(a,7),Ui.dp(a,5));entry.setGravity(Gravity.CENTER_VERTICAL);
                    android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setCornerRadius(Ui.dp(a,9));shape.setColor(Ui.color(a,band==4?com.google.android.material.R.attr.colorTertiaryContainer:com.google.android.material.R.attr.colorSecondaryContainer));
                    entry.setTextColor(Ui.color(a,band==4?com.google.android.material.R.attr.colorOnTertiaryContainer:com.google.android.material.R.attr.colorOnSecondaryContainer));
                    int priority=Ui.priorityColor(a,PlanTime.priority(r,LocalDateTime.now()));if(priority!=0){shape.setColor(priority);entry.setTextColor(Ui.ink(a));}entry.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(Ui.primary(a)&0x33ffffff),shape,null));
                    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=Ui.dp(a,4);cell.addView(entry,p);
                    entry.setOnClickListener(v->a.detail(r));entry.setOnLongClickListener(v->{TaskQuickActions.menu(a,entry,r,refresh);return true;});
                }
                if(items.size()>4)cell.addView(Ui.button(a,"另有 "+(items.size()-4)+" 项",false,()->DailyTasks.show(a,date)));
            }
        }
        page.addView(Ui.help(a,"跨天事项按原开始时刻归类；没有日期的事项保留在今天的待安排区。"));
        Ui.enter(scroller,1);
    }
    private static void heading(MainActivity a,TableRow row,String text,int width,boolean today,Runnable action){
        TextView label=Ui.text(a,text,12,true);label.setGravity(Gravity.CENTER);label.setPadding(Ui.dp(a,3),Ui.dp(a,8),Ui.dp(a,3),Ui.dp(a,8));label.setMinimumHeight(Ui.dp(a,52));label.setMaxLines(3);
        label.setBackgroundColor(Ui.color(a,today?com.google.android.material.R.attr.colorPrimaryContainer:com.google.android.material.R.attr.colorSurfaceContainer));
        if(today)label.setTextColor(Ui.color(a,com.google.android.material.R.attr.colorOnPrimaryContainer));
        if(action!=null)label.setOnClickListener(v->action.run());row.addView(label,new TableRow.LayoutParams(width,-1));
    }
}
