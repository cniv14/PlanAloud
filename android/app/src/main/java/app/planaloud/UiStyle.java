package app.planaloud;

import android.content.Context;
import android.view.View;
import android.widget.*;
import com.google.android.material.radiobutton.MaterialRadioButton;

/** A layout preference; switching never changes records, theme colors or model settings. */
final class UiStyle {
    static boolean neo(Context context){return "neo".equals(context.getSharedPreferences("settings",0).getString("ui_style","classic"));}
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("界面风格",()->new SettingsPanels(a).appearance());page.addView(Ui.text(a,"界面风格",22,true));page.addView(Ui.help(a,"两种风格共用同一份日程、笔记和设置，可随时切换。"));RadioGroup group=new RadioGroup(a);page.addView(group);int classic=View.generateViewId(),neo=View.generateViewId();
        MaterialRadioButton old=new MaterialRadioButton(a);old.setId(classic);old.setText("经典");old.setMinHeight(Ui.dp(a,48));group.addView(old);group.addView(Ui.help(a,"保留原有首页、文字按钮、AI 入口与底部导航。"));
        MaterialRadioButton fresh=new MaterialRadioButton(a);fresh.setId(neo);fresh.setText("Neo");fresh.setMinHeight(Ui.dp(a,48));group.addView(fresh);group.addView(Ui.help(a,"以整屏周时间轴／列表为主，顶部切换视图、翻周和保存；右下角用软件图标打开 AI。其他功能从右上角菜单进入。"));group.check(neo(a)?neo:classic);
        page.addView(Ui.button(a,"应用风格",true,()->a.changeUiStyle(group.getCheckedRadioButtonId()==neo?"neo":"classic")));
    }
}
