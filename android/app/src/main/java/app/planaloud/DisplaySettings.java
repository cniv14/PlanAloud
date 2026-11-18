package app.planaloud;

import android.widget.*;
import com.google.android.material.slider.Slider;
import app.planaloud.ui.UiScale;

final class DisplaySettings {
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("界面与字体大小",()->new SettingsPanels(a).appearance());page.addView(Ui.text(a,"界面与字体大小",22,true));page.addView(Ui.help(a,"界面缩放控制控件与留白，字体大小单独控制文字；都以手机当前设置为基准，只影响序言。"));
        Slider ui=slider(a,page,"界面缩放",70,130,UiScale.interfacePercent(a.prefs.getInt("ui_scale",100))),font=slider(a,page,"字体大小",80,160,UiScale.fontPercent(a.prefs.getInt("font_scale",100)));
        TextView preview=Ui.text(a,"预览：今天 09:00 整理资料",16,true);page.addView(preview);font.addOnChangeListener((s,v,user)->preview.setTextSize(16*v/app.planaloud.ui.UiScale.fontPercent(a.prefs.getInt("font_scale",100))));
        page.addView(Ui.button(a,"应用",true,()->{a.prefs.edit().putInt("ui_scale",Math.round(ui.getValue())).putInt("font_scale",Math.round(font.getValue())).commit();a.recreate();}));page.addView(Ui.button(a,"恢复默认",false,()->{a.prefs.edit().remove("ui_scale").remove("font_scale").commit();a.recreate();}));page.addView(Ui.help(a,"应用后保留当前页面；AI、笔记与语音等页面在重新打开时使用新的大小。系统字体和桌面小组件不会被改动。"));
    }
    private static Slider slider(MainActivity a,LinearLayout page,String name,int min,int max,int value){TextView label=Ui.text(a,name+" · "+value+"%",16,true);page.addView(label);Slider slider=new Slider(a);slider.setContentDescription(name);slider.setValueFrom(min);slider.setValueTo(max);slider.setStepSize(5);slider.setValue(Math.round(value/5f)*5);slider.addOnChangeListener((s,v,user)->label.setText(name+" · "+Math.round(v)+"%"));page.addView(slider);return slider;}
}
