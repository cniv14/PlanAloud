package app.planaloud;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.JSONObject;

/** Original timetable presentation, inspired by SUAT Timetable's compact colored cards. */
final class TimetableHome {
    static void show(MainActivity a,LocalDate date){
        LinearLayout page=a.timetablePage();page.setTag("timetable-home");page.setPadding(Ui.dp(a,12),Ui.dp(a,8),Ui.dp(a,12),0);
        LinearLayout heading=Ui.row(a);page.addView(heading);TextView title=Ui.text(a,date.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA)),22,true);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));icon(a,heading,"新建待办",android.R.drawable.ic_input_add,()->a.newRecord("task"));icon(a,heading,"课表设置",R.drawable.ic_more,()->menu(a,heading,date));
        LinearLayout dates=Ui.row(a);page.addView(dates);icon(a,dates,"前一天",R.drawable.ic_back,()->a.showTimetableDay(date.minusDays(1)));TextView range=Ui.text(a,date.getYear()+" / "+date.getMonthValue()+" · "+(date.equals(LocalDate.now())?"今天":date.toString()),13,false);range.setGravity(Gravity.CENTER);range.setOnClickListener(v->a.showTimetableDay(LocalDate.now()));range.setContentDescription("回到今天");dates.addView(range,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));ImageButton next=icon(a,dates,"后一天",R.drawable.ic_back,()->a.showTimetableDay(date.plusDays(1)));next.setRotation(180);
        if(date.equals(LocalDate.now()))page.addView(new DayTimeline(a),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout columns=Ui.row(a);columns.setGravity(Gravity.TOP);page.addView(columns,new LinearLayout.LayoutParams(-1,0,1));
        for(int d=0;d<2;d++){LocalDate day=date.plusDays(d);LinearLayout column=Ui.column(a);column.setPadding(Ui.dp(a,d==0?0:5),0,Ui.dp(a,d==0?5:0),0);columns.addView(column,new LinearLayout.LayoutParams(0,-1,1));TextView caption=Ui.text(a,(day.equals(LocalDate.now())?"今天":day.equals(LocalDate.now().plusDays(1))?"明天":day.format(DateTimeFormatter.ofPattern("M/d EEE",Locale.CHINA))),15,true);caption.setTextColor(Ui.primary(a));caption.setPadding(0,Ui.dp(a,8),0,Ui.dp(a,10));column.addView(caption);
            ScrollView scroll=new ScrollView(a);scroll.setClipToPadding(false);LinearLayout entries=Ui.column(a);entries.setPadding(0,0,0,Ui.dp(a,12));scroll.addView(entries);column.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));try{java.util.List<JSONObject> rows=DayAgenda.load(a,day);for(JSONObject r:rows)entry(a,entries,r,day);if(rows.isEmpty())entries.addView(Ui.help(a,"暂无安排"));}catch(Exception e){entries.addView(Ui.help(a,"日程读取失败，请重试"));Diagnostics.record(a,"timetable-home",e);}
        }
    }
    private static void entry(MainActivity a,LinearLayout column,JSONObject r,LocalDate day){
        int color=DayEvents.color(r);LinearLayout card=Ui.row(a);card.setGravity(Gravity.CENTER_VERTICAL);card.setMinimumHeight(Ui.dp(a,84));GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(Ui.dp(a,12));bg.setColor(androidx.core.graphics.ColorUtils.blendARGB(Ui.surface(a),color,AgendaWidget.dark(a)?.18f:.12f));card.setBackground(bg);LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(-1,-2);at.bottomMargin=Ui.dp(a,8);column.addView(card,at);
        View stripe=new View(a);stripe.setBackgroundColor(color);LinearLayout.LayoutParams stripeAt=new LinearLayout.LayoutParams(Ui.dp(a,3),Ui.dp(a,54));stripeAt.leftMargin=Ui.dp(a,8);card.addView(stripe,stripeAt);LinearLayout text=Ui.column(a);text.setPadding(Ui.dp(a,8),Ui.dp(a,10),Ui.dp(a,4),Ui.dp(a,10));TextView title=Ui.text(a,r.optString("title"),14,true);title.setMaxLines(3);title.setEllipsize(TextUtils.TruncateAt.END);text.addView(title);TextView meta=Ui.text(a,DayAgenda.meta(r,day),11,false);meta.setTextColor(Ui.muted(a));meta.setMaxLines(3);meta.setEllipsize(TextUtils.TruncateAt.END);text.addView(meta);card.addView(text,new LinearLayout.LayoutParams(0,-2,1));text.setOnClickListener(v->a.detail(r));text.setOnLongClickListener(v->{TaskQuickActions.menu(a,text,r,a::home);return true;});TextView done=Ui.text(a,"○",20,false);done.setGravity(Gravity.CENTER);done.setContentDescription("完成「"+r.optString("title")+"」");done.setTextColor(Ui.primary(a));card.addView(done,new LinearLayout.LayoutParams(Ui.dp(a,40),Ui.dp(a,48)));done.setOnClickListener(v->{try{TaskActions.complete(a,r);a.changed();a.home();}catch(Exception e){a.message(e.getMessage());}});card.setOnClickListener(v->a.detail(r));
    }
    private static ImageButton icon(MainActivity a,LinearLayout row,String name,int drawable,Runnable action){ImageButton b=new ImageButton(a);b.setBackgroundColor(Color.TRANSPARENT);b.setImageResource(drawable);b.setImageTintList(ColorStateList.valueOf(Ui.primary(a)));b.setContentDescription(name);b.setTooltipText(name);b.setOnClickListener(v->action.run());row.addView(b,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,48)));return b;}
    private static void menu(MainActivity a,View anchor,LocalDate date){PopupMenu menu=new PopupMenu(a,anchor);String[] names={"回到今天","周预览","日历与课表","界面风格","设置"};for(String name:names)menu.getMenu().add(name);menu.setOnMenuItemClickListener(i->{switch(i.getTitle().toString()){case "回到今天":a.showTimetableDay(LocalDate.now());break;case "周预览":a.showWeek(date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));break;case "日历与课表":a.openSchedule(date);break;case "界面风格":UiStyle.show(a);break;default:a.openPage("settings");}return true;});menu.show();}
}
