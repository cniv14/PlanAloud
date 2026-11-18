package app.planaloud;

import android.widget.*;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.Arrays;

final class HomeLayout {
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("主界面布局",()->new SettingsPanels(a).appearance());
        page.addView(Ui.text(a,"主界面栏目",22,true));
        page.addView(Ui.help(a,"勾选需要的栏目，底部栏数量随之调整。周预览与设置入口始终保留；全部开启时，笔记和收集箱合并到更多入口；隐藏功能仍可从更多功能或 / 指令打开。"));
        String[] keys={"home_today","home_schedule","home_notes","home_inbox"},labels={"今天","日程","笔记","收集箱"};
        for(int i=0;i<keys.length;i++){
            String key=keys[i];MaterialSwitch toggle=new MaterialSwitch(a);toggle.setText(labels[i]);toggle.setChecked(a.prefs.getBoolean(key,i<2));page.addView(toggle);
            toggle.setOnCheckedChangeListener((button,on)->{a.prefs.edit().putBoolean(key,on).apply();a.refreshNavigation();});
        }
        Spinner columns=TaskTypes.spinner(a,page,"内容卡片列数",Arrays.asList("自动适配","1 列","2 列","3 列","4 列"));
        columns.setSelection(Math.max(0,Math.min(4,a.prefs.getInt("home_columns",0))));
        columns.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,android.view.View view,int position,long id){a.prefs.edit().putInt("home_columns",position).apply();}
            public void onNothingSelected(AdapterView<?> parent){}
        });
        page.addView(Ui.help(a,"列数应用于今天待办、笔记、收集箱和更多功能中的卡片。"));
    }
}
