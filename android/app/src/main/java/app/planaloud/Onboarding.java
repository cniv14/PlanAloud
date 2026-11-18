package app.planaloud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.android.material.radiobutton.MaterialRadioButton;

/** First-install setup with an always-visible skip action; replayable from Settings. */
final class Onboarding {
    private final MainActivity a;
    private final boolean firstLaunch;
    private int step;
    private String mode,theme,style,finish,weekLayout,weekRange;
    private int ui,font,columns,rows,widget;

    private Onboarding(MainActivity a,boolean firstLaunch,Bundle saved){
        this.a=a;this.firstLaunch=firstLaunch;
        step=saved==null?0:Math.max(0,Math.min(3,saved.getInt("guide_step",0)));
        mode=saved==null?a.prefs.getString("usage_mode","local"):saved.getString("guide_mode","local");
        if(!mode.equals("cloud"))mode="local";
        theme=saved==null?a.prefs.getString("theme","system"):saved.getString("guide_theme","system");style=saved==null?a.prefs.getString("ui_style","classic"):saved.getString("guide_style","classic");finish=saved==null?a.prefs.getString("card_texture","none"):saved.getString("guide_finish","none");
        ui=saved==null?a.prefs.getInt("ui_scale",100):saved.getInt("guide_ui",100);font=saved==null?a.prefs.getInt("font_scale",100):saved.getInt("guide_font",100);columns=saved==null?a.prefs.getInt("home_columns",1):saved.getInt("guide_columns",1);rows=saved==null?a.prefs.getInt("home_rows",0):saved.getInt("guide_rows",0);widget=saved==null?("grid".equals(a.prefs.getString("widget_layout","list"))?1:0):saved.getInt("guide_widget",0);
        weekLayout=saved==null?a.prefs.getString("week_layout","time"):saved.getString("guide_week_layout","time");weekRange=saved==null?(!"fixed".equals(a.prefs.getString("week_range","rolling"))?"rolling":a.prefs.getString("week_first_day","monday")):saved.getString("guide_week_range","rolling");

    }
    static void prepare(SharedPreferences prefs,boolean newInstall){
        if(newInstall&&!prefs.getBoolean("onboarding_completed",false))prefs.edit().putBoolean("onboarding_pending",true).apply();
    }
    static boolean pending(SharedPreferences prefs){return prefs.getBoolean("onboarding_pending",false)&&!prefs.getBoolean("onboarding_completed",false);}
    static boolean restoring(Bundle state){return state!=null&&state.getBoolean("guide_showing",false);}
    static void show(MainActivity a){open(a,false,null);}
    static void open(MainActivity a,boolean firstLaunch,Bundle saved){a.onboarding=new Onboarding(a,firstLaunch,saved);a.onboarding.render();}
    void save(Bundle state){state.putBoolean("guide_showing",true);state.putBoolean("guide_first",firstLaunch);state.putInt("guide_step",step);state.putString("guide_mode",mode);state.putString("guide_theme",theme);state.putString("guide_style",style);state.putString("guide_finish",finish);state.putInt("guide_ui",ui);state.putInt("guide_font",font);state.putInt("guide_columns",columns);state.putInt("guide_rows",rows);state.putInt("guide_widget",widget);state.putString("guide_week_layout",weekLayout);state.putString("guide_week_range",weekRange);}
    private void render(){
        LinearLayout root=a.editorPage("使用引导",()->{if(step>0){step--;render();}else finish(true,false);});
        LinearLayout header=Ui.row(a);header.setPadding(Ui.dp(a,20),Ui.dp(a,8),Ui.dp(a,16),Ui.dp(a,8));
        LinearLayout heading=Ui.column(a);heading.addView(Ui.text(a,step==0?"欢迎使用序言":step==1?"先设置喜欢的外观":step==2?"认识几个常用操作":"准备开始",23,true));heading.addView(Ui.help(a,"使用引导 · "+(step+1)+" / 4"));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(Ui.button(a,"跳过",false,()->finish(true,false)),new LinearLayout.LayoutParams(Ui.dp(a,84),-2));root.addView(header);
        ScrollView scroll=new ScrollView(a);LinearLayout body=Ui.padded(a);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(step==0)chooseMode(body);else if(step==1)appearance(body);else if(step==2)operations(body);else summary(body);
        LinearLayout footer=Ui.row(a);footer.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),Ui.dp(a,12));root.addView(footer);
        if(step>0)Ui.cell(a,footer,Ui.button(a,"上一步",false,()->{step--;render();}));
        if(step<3)Ui.cell(a,footer,Ui.button(a,step==1?"保存并继续":"下一步",true,()->{if(step==1)applyAppearance(true);else{step++;render();}}));
        else{
            Ui.cell(a,footer,Ui.button(a,mode.equals("cloud")?"配置云端同步":"开始本地使用",true,()->finish(false,mode.equals("cloud"))));
            if(mode.equals("cloud"))body.addView(Ui.button(a,"稍后配置，先开始使用",false,()->finish(false,false)));
        }
    }
    private void chooseMode(LinearLayout body){
        body.addView(Ui.text(a,"选择使用方式",20,true));body.addView(Ui.help(a,"两种方式都把内容保存在本机，之后可以在设置中切换。"));
        RadioGroup group=new RadioGroup(a);group.setOrientation(LinearLayout.VERTICAL);body.addView(group);
        int local=View.generateViewId(),cloud=View.generateViewId();
        MaterialRadioButton offline=new MaterialRadioButton(a);offline.setId(local);offline.setText("本地使用");offline.setMinHeight(Ui.dp(a,48));group.addView(offline);group.addView(Ui.help(a,"无需注册或服务器，可离线管理待办、日程、课程和笔记。AI、天气与同步可按需配置。"));
        MaterialRadioButton online=new MaterialRadioButton(a);online.setId(cloud);online.setText("云端同步");online.setMinHeight(Ui.dp(a,48));group.addView(online);group.addView(Ui.help(a,"使用自托管或可信的序言服务器，在设备间同步。完成引导后填写服务器地址并登录；没有服务器也可先使用本地功能。"));
        group.check(mode.equals("cloud")?cloud:local);group.setOnCheckedChangeListener((g,id)->mode=id==cloud?"cloud":"local");
    }
    private void appearance(LinearLayout body){
        body.addView(Ui.help(a,"可现在设置，也可跳过使用默认值。界面风格与大小修改后，需要在红字十秒倒计时内确认；超时自动还原。"));
        select(body,"主题",new String[]{"跟随系统","浅色","深色"},new String[]{"system","light","dark"},theme,value->theme=value);
        select(body,"UI 风格",new String[]{"经典","Neo · AI 主导","课表"},new String[]{"classic","neo","timetable"},style,value->style=value);
        size(body,"界面缩放",70,130,ui,value->ui=value);size(body,"字体大小",80,160,font,value->font=value);
        select(body,"主界面卡片列数",new String[]{"自动","1 列（默认）","2 列","3 列","4 列"},new String[]{"0","1","2","3","4"},String.valueOf(columns),value->columns=Integer.parseInt(value));
        select(body,"每组首批显示行数",new String[]{"自动","1 行","2 行","3 行","4 行","5 行","6 行","7 行","8 行"},new String[]{"0","1","2","3","4","5","6","7","8"},String.valueOf(rows),value->rows=Integer.parseInt(value));
        select(body,"近期日程小组件排布",new String[]{"1 列（默认）","2 列"},new String[]{"0","1"},String.valueOf(widget),value->widget=Integer.parseInt(value));
        select(body,"卡片质感（单独设置）",CardFinish.LABELS,CardFinish.VALUES,finish,value->finish=value);
        select(body,"周日程排布",new String[]{"按时间","按事件"},new String[]{"time","events"},weekLayout,value->weekLayout=value);
        select(body,"周范围",new String[]{"活动周","固定周 · 周一开始","固定周 · 周日开始"},new String[]{"rolling","monday","sunday"},weekRange,value->weekRange=value);
        body.addView(Ui.help(a,"列数默认 1；首批行数用于今天各组待办，更多可继续展开。Neo 仅有今日与周预览，使用 AI 或指令管理。双日小组件仍按今天／明天分两栏；卡片质感默认无。"));body.addView(Ui.button(a,"应用并预览",false,()->applyAppearance(false)));
    }
    private void select(LinearLayout body,String label,String[] names,String[] values,String current,java.util.function.Consumer<String> change){Spinner spinner=TaskTypes.spinner(a,body,label,java.util.Arrays.asList(names));spinner.setSelection(Math.max(0,java.util.Arrays.asList(values).indexOf(current)));spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int index,long id){change.accept(values[index]);}public void onNothingSelected(AdapterView<?> p){}});}
    private void size(LinearLayout body,String label,int min,int max,int current,java.util.function.IntConsumer change){TextView heading=Ui.text(a,label+" · "+current+"%",15,true);body.addView(heading);com.google.android.material.slider.Slider slider=new com.google.android.material.slider.Slider(a);slider.setValueFrom(min);slider.setValueTo(max);slider.setStepSize(5);slider.setValue(Math.max(min,Math.min(max,Math.round(current/5f)*5)));slider.setContentDescription(label);slider.addOnChangeListener((control,value,user)->{change.accept(Math.round(value));heading.setText(label+" · "+Math.round(value)+"%");});body.addView(slider);}
    private void applyAppearance(boolean advance){
        boolean changedTheme=!theme.equals(a.prefs.getString("theme","system"));a.prefs.edit().putString("theme",theme).putInt("home_columns",columns).putInt("home_rows",rows).putString("widget_layout",widget==0?"list":"grid").putString("card_texture",finish).putString("week_layout",weekLayout).putString("week_range",weekRange.equals("rolling")?"rolling":"fixed").putString("week_first_day",weekRange.equals("sunday")?"sunday":"monday").commit();if(advance)step=2;
        boolean trial=app.planaloud.ui.DisplayTrial.begin(a,ui,font,style);AgendaWidget.refresh(a);if(changedTheme)DayFlowApp.applyTheme(theme);if(trial||changedTheme)a.recreate();else render();
    }
    private void operations(LinearLayout body){
        if(!UiStyle.neo(a)){GuidePractice.add(a,body);GuidePractice.week(a,body);}
        tip(body,"今天 · 新建待办","经典点击「＋ 新建待办」，Neo 使用 AI 或指令新建；点事项查看、编辑，勾选方框完成。长按可查看快捷操作。");
        tip(body,"日程 · 月历与课表","点日期查看当天安排，从「课表」导入课程。经典保留原有导航；Neo 隐藏手动编辑入口，用 AI 或指令管理事项，右下软件图标打开 AI；左右滑动切换已显示的栏目；底栏「周预览」是独立页面，今天向左滑也可进入。周表可切换按时间／按事件排布，并选择活动周或固定周（周一／周日开始）；按时间模式默认 08:00–22:00，超出自动延长。可斜向拖动、双指缩放，顶部星期和左侧时间固定；经典周表整屏显示，仅表格内部滚动。长期任务横跨日期，名称仅在今天显示；点「导出 PNG」保存完整七天。");
        tip(body,"每天的时间进度","首页彩色进度条在上下方用斜线和短横线就近标注事件名称，当前位置表示现在；默认折叠，仅标注当前时间段的事件；点击进度条展开全天标注，再点折叠，长按查看完整信息。展开后名称全部同屏显示，上下各最多两行，密集时缩短名称，不分页。色块与贴近主条的细条区分，始终按时间定位；当前时间竖线加长。桌面小组件也显示事件色段。通过系统桌面长按空白处 → 小组件 → 序言，可分别添加「近期日程」和「今天与明天」双栏组件，直接勾选即可完成事项，不会弹出应用。点事项内容仍会打开详情。");
        tip(body,"问序言 AI","在设置中配置模型与 API Key 后，用自然语言查询或安排日程。按住麦克风开始录音，松开识别，上滑取消；语音配置位于 AI 设置 → 语音识别。输入 / 可滚动查看指令，支持图片和文本附件；聊天顶部可切换自动批准；关闭后需确认的操作会显示确认卡，已执行操作可撤销。图片按原图上传，可多选；不要把未核验的课表草稿当作已导入课程。");
        tip(body,"设置 · 更多功能","笔记、收集箱等功能可从更多功能打开。外观设置可切换经典／Neo／课表风格，并分别调整界面缩放、字体大小、主界面栏目与卡片列数；界面风格与缩放调整后须在 10 秒内确认保留，否则自动恢复。数据与同步中可备份或连接服务器。");
    }
    private void summary(LinearLayout body){
        tip(body,mode.equals("cloud")?"云端同步，随时可配置":"先从本地开始",mode.equals("cloud")?"下一步填写服务器地址和账号。登录成功后才启用自动同步；也可以稍后配置。":"核心功能无需联网。以后可在「设置 → 同步」连接服务器，已有本地内容会保留。");
        tip(body,"按需开启权限","日程提醒、语音、天气定位和悬浮小窗会在使用时申请相应权限。可以先使用基本功能。");
        body.addView(Ui.help(a,"想再看一遍时，在设置中打开「使用引导」，或输入 /guide；完整功能说明可从更多功能或 /manual 查阅。"));
    }
    private void tip(LinearLayout body,String title,String text){
        com.google.android.material.card.MaterialCardView card=Ui.card(a);LinearLayout content=Ui.padded(a);content.addView(Ui.text(a,title,18,true));content.addView(Ui.help(a,text));card.addView(content);body.addView(card);
    }
    private void finish(boolean skipped,boolean configureCloud){
        SharedPreferences.Editor edit=a.prefs.edit();
        if(!skipped||firstLaunch){String choice=skipped?"local":mode;edit.putString("usage_mode",choice);if(choice.equals("local"))edit.putBoolean("sync_auto",false);}
        edit.putBoolean("onboarding_completed",true).remove("onboarding_pending").apply();
        a.onboardingFinished(firstLaunch,configureCloud&&!skipped);
    }
}
