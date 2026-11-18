package app.planaloud;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.JSONObject;

/** Frozen date/time axes around a minute-accurate, freely pannable timetable. */
final class WeekPreview {
    static void show(MainActivity a,LinearLayout page,LocalDate first,java.util.function.Consumer<LocalDate> change,Runnable refresh){
        LinearLayout controls=Ui.row(a);page.addView(controls);action(a,controls,"上一周",R.drawable.ic_back,()->change.accept(first.minusWeeks(1)));action(a,controls,"回到今天",R.drawable.ic_calendar,()->change.accept(LocalDate.now()));
        android.widget.ImageButton next=new android.widget.ImageButton(a);next.setImageResource(R.drawable.ic_back);next.setRotation(180);next.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.primary(a)));next.setBackgroundColor(Color.TRANSPARENT);next.setContentDescription("下一周");next.setTooltipText("下一周");next.setOnClickListener(v->change.accept(first.plusWeeks(1)));controls.addView(next,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));
        action(a,controls,"导出 PNG",android.R.drawable.ic_menu_save,()->WeekPng.request(a,first));action(a,controls,"使用帮助",android.R.drawable.ic_menu_help,()->WeekPng.help(a));
        page.addView(Ui.text(a,first+" — "+first.plusDays(6),16,true));
        WeekSchedule snapshot;try{snapshot=WeekSchedule.load(a,first);}catch(Exception failure){a.message("无法读取周日程："+failure.getMessage());return;}WeekGeometry geometry=new WeekGeometry(snapshot);
        int width=Ui.dp(a,128),header=Ui.dp(a,44),side=Ui.dp(a,46),height=Math.round(geometry.minutes()*Ui.dp(a,72)/60f);float ppm=height/(float)geometry.minutes();
        LinearLayout table=Ui.column(a);table.setTag("week-table");LinearLayout top=Ui.row(a);table.addView(top,new LinearLayout.LayoutParams(-1,header));top.addView(label(a,"时间",true),new LinearLayout.LayoutParams(side,-1));
        TwoAxisScroll headerClip=new TwoAxisScroll(a);top.addView(headerClip,new LinearLayout.LayoutParams(0,-1,1));LinearLayout headings=Ui.row(a);headings.setTag("week-headings");headerClip.addView(headings,new FrameLayout.LayoutParams(width*7,header));
        LinearLayout lower=Ui.row(a);lower.setGravity(Gravity.TOP);table.addView(lower,new LinearLayout.LayoutParams(-1,0,1));FrameLayout timeClip=new FrameLayout(a);lower.addView(timeClip,new LinearLayout.LayoutParams(side,-1));View hours=new TimeScale(a,geometry,ppm);hours.setTag("week-times");timeClip.addView(hours,new FrameLayout.LayoutParams(side,height));
        TwoAxisScroll pan=new TwoAxisScroll(a);pan.setContentDescription("按实际时间排列，可横向和纵向拖动的周日程");LinearLayout columns=Ui.row(a);columns.setGravity(Gravity.TOP);pan.addView(columns,new FrameLayout.LayoutParams(width*7,height));lower.addView(pan,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout bottom=Ui.row(a);table.addView(bottom,new LinearLayout.LayoutParams(-1,Ui.dp(a,40)));bottom.addView(label(a,"截止",false),new LinearLayout.LayoutParams(side,-1));FrameLayout dueClip=new FrameLayout(a);bottom.addView(dueClip,new LinearLayout.LayoutParams(0,-1,1));LinearLayout dueRow=Ui.row(a);dueRow.setTag("week-deadlines");dueClip.addView(dueRow,new FrameLayout.LayoutParams(width*7,-1));
        pan.onPosition((x,y)->{headerClip.scrollTo(x,0);hours.setTranslationY(-y);dueRow.setTranslationX(-x);});headerClip.onPosition((x,y)->pan.scrollTo(x,pan.getScrollY()));
        for(int d=0;d<7;d++){
            LocalDate date=first.plusDays(d);TextView day=label(a,date.format(DateTimeFormatter.ofPattern("EEE\nM/d",Locale.CHINA)),true);day.setOnClickListener(v->a.openSchedule(date));if(date.equals(LocalDate.now()))day.setTextColor(Ui.primary(a));headings.addView(day,new LinearLayout.LayoutParams(width,header));
            TimeDay body=new TimeDay(a,geometry,ppm);body.setTag(snapshot.allDay.get(d).isEmpty()?"week-day":"week-all-day");GradientDrawable shape=new GradientDrawable();shape.setColor(snapshot.allDay.get(d).isEmpty()?Color.TRANSPARENT:LiquidGlass.tint(Ui.color(a,com.google.android.material.R.attr.colorTertiaryContainer),105));shape.setStroke(Ui.dp(a,1),LiquidGlass.tint(Ui.muted(a),60));body.setBackground(shape);columns.addView(body,new LinearLayout.LayoutParams(width,height));
            java.util.List<JSONObject> all=snapshot.allDay.get(d);int rail=all.isEmpty()?0:Ui.dp(a,18);
            if(!all.isEmpty()){View strip=new AllDayRail(a,all);strip.setContentDescription("全天 · "+titles(all));strip.setOnClickListener(v->choose(a,all,"全天事项"));body.addView(strip,new FrameLayout.LayoutParams(rail,-1));}
            for(WeekGeometry.Timed event:geometry.days.get(d)){
                int available=width-rail-Ui.dp(a,6),eventWidth=Math.max(1,available/event.lanes),eventHeight=Math.max(2,Math.round(geometry.height(event,ppm)));
                TextView entry=Ui.text(a,event.record.optString("title")+(eventHeight>=Ui.dp(a,44)?"\n"+PlanTime.dailyLabel(event.record,date):""),11,true);entry.setPadding(Ui.dp(a,3),0,Ui.dp(a,3),0);entry.setGravity(Gravity.TOP);entry.setEllipsize(android.text.TextUtils.TruncateAt.END);entry.setMaxLines(Math.max(1,eventHeight/Ui.dp(a,16)));entry.setContentDescription(PlanTime.dailyLabel(event.record,date)+" · "+event.record.optString("title"));entry.setTooltipText(entry.getContentDescription());entry.setTag("week-timed-event");
                GradientDrawable tile=new GradientDrawable();tile.setCornerRadius(Ui.dp(a,5));int priority=Ui.priorityColor(a,PlanTime.priority(event.record,LocalDateTime.now()));tile.setColor(priority==0?Ui.color(a,com.google.android.material.R.attr.colorSecondaryContainer):priority);entry.setBackground(tile);
                FrameLayout.LayoutParams at=new FrameLayout.LayoutParams(Math.max(1,eventWidth-Ui.dp(a,2)),eventHeight);at.leftMargin=rail+Ui.dp(a,3)+event.lane*eventWidth;at.topMargin=Math.round(geometry.top(event,ppm));body.addView(entry,at);entry.setOnClickListener(v->a.detail(event.record));entry.setOnLongClickListener(v->{TaskQuickActions.menu(a,entry,event.record,refresh);return true;});
            }
            java.util.List<JSONObject> due=geometry.deadlines.get(d);TextView deadline=label(a,due.isEmpty()?"":due.get(0).optString("title")+(due.size()>1?" +"+(due.size()-1):""),false);deadline.setSingleLine(true);deadline.setEllipsize(android.text.TextUtils.TruncateAt.END);deadline.setContentDescription(due.isEmpty()?"当天无截止待办":"截止待办 · "+titles(due));if(!due.isEmpty())deadline.setOnClickListener(v->choose(a,due,"截止待办"));FrameLayout dueCell=new FrameLayout(a);dueCell.addView(deadline,new FrameLayout.LayoutParams(-1,Ui.dp(a,26),Gravity.TOP));View stripe=new View(a);stripe.setBackgroundColor(LiquidGlass.tint(Ui.primary(a),due.isEmpty()?25:130));FrameLayout.LayoutParams stripeAt=new FrameLayout.LayoutParams(-1,Ui.dp(a,4),Gravity.BOTTOM);stripeAt.bottomMargin=Ui.dp(a,5);dueCell.addView(stripe,stripeAt);dueCell.setContentDescription(deadline.getContentDescription());if(!due.isEmpty())dueCell.setOnClickListener(v->choose(a,due,"截止待办"));dueRow.addView(dueCell,new LinearLayout.LayoutParams(width,-1));
        }
        page.addView(table,new LinearLayout.LayoutParams(-1,Ui.dp(a,Math.max(280,Math.min(900,a.getResources().getConfiguration().screenHeightDp-180)))));
        if(geometry.firstMinute<480||geometry.lastMinute>1320)page.addView(Ui.help(a,"时间范围自动延长至 "+DayEvents.clock(geometry.firstMinute)+"–"+DayEvents.clock(geometry.lastMinute)));
    }
    private static void action(MainActivity a,LinearLayout row,String name,int icon,Runnable run){com.google.android.material.button.MaterialButton button=Ui.iconButton(a,"",icon,false,run);button.setContentDescription(name);button.setTooltipText(name);row.addView(button,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));}
    private static TextView label(MainActivity a,String title,boolean bold){TextView text=Ui.text(a,title,bold?12:11,bold);text.setGravity(Gravity.CENTER);return text;}
    private static String titles(java.util.List<JSONObject> records){StringBuilder names=new StringBuilder();for(JSONObject r:records){if(names.length()>0)names.append("、");names.append(r.optString("title"));}return names.toString();}
    private static void choose(MainActivity a,java.util.List<JSONObject> records,String title){String[] names=new String[records.size()];for(int i=0;i<names.length;i++)names[i]=records.get(i).optString("title");new com.google.android.material.dialog.MaterialAlertDialogBuilder(a).setTitle(title).setItems(names,(d,n)->a.detail(records.get(n))).setNegativeButton("关闭",null).show();}
    private static final class TimeScale extends View {
        final MainActivity a;final WeekGeometry geometry;final float ppm;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TimeScale(MainActivity a,WeekGeometry geometry,float ppm){super(a);this.a=a;this.geometry=geometry;this.ppm=ppm;}
        @Override protected void onDraw(Canvas canvas){paint.setTextSize(Ui.dp(a,10)*a.getResources().getConfiguration().fontScale);paint.setTextAlign(Paint.Align.CENTER);for(int minute=geometry.firstMinute;minute<=geometry.lastMinute;minute+=60){paint.setColor(minute<480||minute>1320?Ui.primary(a):Ui.muted(a));float y=(minute-geometry.firstMinute)*ppm;canvas.drawText(DayEvents.clock(minute),getWidth()/2f,Math.max(-paint.ascent(),Math.min(getHeight()-paint.descent(),y-paint.ascent()/2)),paint);}}
    }
    private static final class TimeDay extends FrameLayout {
        final MainActivity a;final WeekGeometry geometry;final float ppm;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TimeDay(MainActivity a,WeekGeometry geometry,float ppm){super(a);this.a=a;this.geometry=geometry;this.ppm=ppm;setWillNotDraw(false);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);paint.setColor(LiquidGlass.tint(Ui.muted(a),60));paint.setStrokeWidth(Math.max(1,Ui.dp(a,1)));for(int minute=geometry.firstMinute;minute<=geometry.lastMinute;minute+=60){float y=(minute-geometry.firstMinute)*ppm;canvas.drawLine(0,y,getWidth(),y,paint);}}
    }
    private static final class AllDayRail extends View {
        final MainActivity a;final String title;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        AllDayRail(MainActivity a,java.util.List<JSONObject> records){super(a);this.a=a;title="全天 · "+titles(records);}
        @Override protected void onDraw(Canvas canvas){paint.setColor(Ui.muted(a));paint.setTextSize(Ui.dp(a,10)*getResources().getConfiguration().fontScale);canvas.save();canvas.rotate(90);canvas.drawText(title,Ui.dp(a,6),-Ui.dp(a,4),paint);canvas.restore();}
    }
}
