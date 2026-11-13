package app.planaloud;
import android.widget.*;
import java.time.*;
import org.json.*;

final class ScheduleScreen {
 static void show(MainActivity a,LinearLayout page,LocalDate initial){
  LinearLayout below=Ui.column(a);MonthGrid.add(a,page,initial,date->{a.setScheduleDate(date);day(a,below,date);});page.addView(below);day(a,below,initial);
 }
 private static void day(MainActivity a,LinearLayout below,LocalDate date){below.removeAllViews();below.addView(Ui.text(a,date+" · "+ChineseDates.description(date),17,true));String holiday=Holidays.description(date);if(!holiday.isEmpty())below.addView(Ui.help(a,holiday));java.util.List<String> labels=CalendarDay.labels(a,date);if(!labels.isEmpty())below.addView(Ui.help(a,String.join(" · ",labels)));Forecast.add(a,below,date,()->day(a,below,date));
  LinearLayout menu=Ui.row(a);below.addView(menu);
  Ui.cell(a,menu,Ui.iconButton(a,"课表",R.drawable.ic_course,false,()->courses(a)));
  Ui.cell(a,menu,Ui.iconButton(a,"黄历",R.drawable.ic_calendar,false,()->almanac(a,date)));
  LinearLayout actions=Ui.row(a);below.addView(actions);
  Ui.cell(a,actions,Ui.iconButton(a,"添加日程",android.R.drawable.ic_input_add,true,()->{try{a.edit(Data.make("event","").put("start_at",date.atTime(9,0).toString()));}catch(Exception e){a.message(e.getMessage());}}));
  Ui.cell(a,actions,Ui.iconButton(a,"编辑日程",android.R.drawable.ic_menu_edit,false,()->ScheduleEditor.show(a,date,false)));
  LinearLayout tools=Ui.row(a);below.addView(tools);
  Ui.cell(a,tools,Ui.iconButton(a,"日历标注",android.R.drawable.ic_menu_edit,false,()->CalendarDay.edit(a,date,()->a.openSchedule(date))));
  Ui.cell(a,tools,Ui.iconButton(a,"更多操作",android.R.drawable.ic_menu_more,false,()->CommandScreen.show(a)));
  DailyTasks.rows(a,below,date,()->day(a,below,date));
 }
 static void courses(MainActivity a){LinearLayout page=a.fullPage("课表",a::home);page.addView(Ui.text(a,"课表",24,true));page.addView(Ui.help(a,"课程统一显示在每日待办；支持单双周、截图识别和网页导入。"));if(TimetableCapture.hasDraft(a))page.addView(Ui.button(a,"恢复待确认课表",false,()->new TimetableCapture(a,null)));page.addView(Ui.button(a,"AI 截图识别课表",true,()->a.pickFile(110,"image/*")));page.addView(Ui.button(a,"编辑课表",false,()->ScheduleEditor.show(a,LocalDate.now(),true)));page.addView(Ui.button(a,"导入 CSV / ICS / HTML",false,()->a.pickFile(103,"*/*")));page.addView(Ui.button(a,"网页课表与通知导入",false,()->WebImports.show(a)));page.addView(Ui.button(a,"离线截图文字识别",false,()->ExtensionHub.pickOcr(a)));page.addView(Ui.button(a,"＋ 新建课程",false,()->a.newRecord("course")));}
 static void almanac(MainActivity a,LocalDate date){DocumentPlugins.almanac(a,date);}
}
