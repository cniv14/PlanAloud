package app.planaloud;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import com.google.android.material.button.*;

final class WeekOptions {
    static boolean events(Context c){return "events".equals(c.getSharedPreferences("settings",0).getString("week_layout","time"));}
    static LocalDate first(Context c,LocalDate anchor){android.content.SharedPreferences p=c.getSharedPreferences("settings",0);if(!"fixed".equals(p.getString("week_range","rolling")))return anchor;return anchor.with(TemporalAdjusters.previousOrSame("sunday".equals(p.getString("week_first_day","monday"))?DayOfWeek.SUNDAY:DayOfWeek.MONDAY));}
    static String rangeLabel(Context c){android.content.SharedPreferences p=c.getSharedPreferences("settings",0);return "fixed".equals(p.getString("week_range","rolling"))?("sunday".equals(p.getString("week_first_day","monday"))?"固定周 · 周日开始":"固定周 · 周一开始"):"活动周";}
    static void controls(MainActivity a,LinearLayout page,LocalDate first){
        LinearLayout row=Ui.row(a);row.setTag("week-options");page.addView(row);MaterialButtonToggleGroup group=new MaterialButtonToggleGroup(a);group.setSingleSelection(true);group.setSelectionRequired(true);row.addView(group,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));boolean cards=events(a);
        for(int i=0;i<2;i++){boolean choice=i==1;MaterialButton b=Ui.button(a,choice?"按事件":"按时间",false,()->{if(choice!=cards){a.prefs.edit().putString("week_layout",choice?"events":"time").apply();a.weekOptionsChanged(first);}});b.setId(View.generateViewId());b.setCheckable(true);b.setTextSize(12);b.setContentDescription(choice?"按事件排布，不严格对齐时间轴":"按时间排布，对齐时间轴");group.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));if(choice==cards)group.check(b.getId());}
        group.setOnLongClickListener(v->{WeekPreview.resetZoom(a);return true;});
        MaterialButton range=Ui.iconButton(a,"",R.drawable.ic_calendar,false,()->range(a));range.setContentDescription("周范围："+rangeLabel(a)+"，点击切换");range.setTooltipText(range.getContentDescription());range.setOnLongClickListener(v->{WeekPreview.resetZoom(a);return true;});row.addView(range,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,48)));
    }
    static void range(MainActivity a){String[] choices={"活动周 · 当前日期起七天","固定周 · 周一开始","固定周 · 周日开始"};int selected=!"fixed".equals(a.prefs.getString("week_range","rolling"))?0:"sunday".equals(a.prefs.getString("week_first_day","monday"))?2:1;new com.google.android.material.dialog.MaterialAlertDialogBuilder(a).setTitle("周预览范围").setSingleChoiceItems(choices,selected,(d,n)->{a.prefs.edit().putString("week_range",n==0?"rolling":"fixed").putString("week_first_day",n==2?"sunday":"monday").apply();d.dismiss();a.weekOptionsChanged(LocalDate.now());}).setNegativeButton("取消",null).show();}
}
