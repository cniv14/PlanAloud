package app.planaloud;
import android.content.Context;
import android.view.View;
import android.widget.*;
import com.google.android.material.radiobutton.MaterialRadioButton;

final class UiStyle {
    static String value(Context c){return c.getSharedPreferences("settings",0).getString("ui_style","classic");}
    static boolean neo(Context c){return "neo".equals(value(c));}
    static boolean timetable(Context c){return "timetable".equals(value(c));}
    static String name(Context c){return neo(c)?"Neo":timetable(c)?"课表":"经典";}
    static String description(Context c){return name(c)+(neo(c)?" · AI 今日与周预览":timetable(c)?" · 清爽课表卡片":" · 原有首页与导航");}
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("界面风格",()->new SettingsPanels(a).appearance());page.addView(Ui.text(a,"界面风格",22,true));page.addView(Ui.help(a,"三种风格共用日程、笔记与设置；应用后有 10 秒确认时间。"));RadioGroup group=new RadioGroup(a);page.addView(group);
        String[] values={"classic","neo","timetable"},names={"经典","Neo","课表"},details={"保留原有首页、文字按钮、AI 入口与底部导航。","仅今日与周预览两个主页面，今日事项与 AI 入口；周表支持按时间／事件排布，日程仅查看，修改交给 AI 或指令，不显示手动新建和编辑入口。","参考 SUAT Timetable 的清爽课表卡片：今天与明天双栏，事件侧边色条、时间和地点；周预览使用紧凑彩色卡片。"};int[] ids=new int[3];
        for(int i=0;i<3;i++){ids[i]=View.generateViewId();MaterialRadioButton choice=new MaterialRadioButton(a);choice.setId(ids[i]);choice.setText(names[i]);choice.setMinHeight(Ui.dp(a,48));group.addView(choice);group.addView(Ui.help(a,details[i]));if(values[i].equals(value(a)))group.check(ids[i]);}
        page.addView(Ui.button(a,"应用风格",true,()->{for(int i=0;i<ids.length;i++)if(group.getCheckedRadioButtonId()==ids[i]){a.changeUiStyle(values[i]);break;}}));
    }
}
