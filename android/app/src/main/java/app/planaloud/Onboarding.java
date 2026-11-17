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
    private String mode;
    private Onboarding(MainActivity a,boolean firstLaunch,Bundle saved){
        this.a=a;this.firstLaunch=firstLaunch;
        step=saved==null?0:Math.max(0,Math.min(2,saved.getInt("guide_step",0)));
        mode=saved==null?a.prefs.getString("usage_mode","local"):saved.getString("guide_mode","local");
        if(!mode.equals("cloud"))mode="local";
    }
    static void prepare(SharedPreferences prefs,boolean newInstall){
        if(newInstall&&!prefs.getBoolean("onboarding_completed",false))prefs.edit().putBoolean("onboarding_pending",true).apply();
    }
    static boolean pending(SharedPreferences prefs){return prefs.getBoolean("onboarding_pending",false)&&!prefs.getBoolean("onboarding_completed",false);}
    static boolean restoring(Bundle state){return state!=null&&state.getBoolean("guide_showing",false);}
    static void show(MainActivity a){open(a,false,null);}
    static void open(MainActivity a,boolean firstLaunch,Bundle saved){a.onboarding=new Onboarding(a,firstLaunch,saved);a.onboarding.render();}
    void save(Bundle state){state.putBoolean("guide_showing",true);state.putBoolean("guide_first",firstLaunch);state.putInt("guide_step",step);state.putString("guide_mode",mode);}
    private void render(){
        LinearLayout root=a.editorPage("使用引导",()->{if(step>0){step--;render();}else finish(true,false);});
        LinearLayout header=Ui.row(a);header.setPadding(Ui.dp(a,20),Ui.dp(a,8),Ui.dp(a,16),Ui.dp(a,8));
        LinearLayout heading=Ui.column(a);heading.addView(Ui.text(a,step==0?"欢迎使用序言":step==1?"先认识几个常用操作":"准备开始",23,true));heading.addView(Ui.help(a,"使用引导 · "+(step+1)+" / 3"));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(Ui.button(a,"跳过",false,()->finish(true,false)),new LinearLayout.LayoutParams(Ui.dp(a,84),-2));root.addView(header);
        ScrollView scroll=new ScrollView(a);LinearLayout body=Ui.padded(a);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(step==0)chooseMode(body);else if(step==1)operations(body);else summary(body);
        LinearLayout footer=Ui.row(a);footer.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),Ui.dp(a,12));root.addView(footer);
        if(step>0)Ui.cell(a,footer,Ui.button(a,"上一步",false,()->{step--;render();}));
        if(step<2)Ui.cell(a,footer,Ui.button(a,"下一步",true,()->{step++;render();}));
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
    private void operations(LinearLayout body){
        GuidePractice.add(a,body);
        tip(body,"今天 · 新建待办","点「＋ 新建待办」记录要做的事；点事项查看、编辑，勾选方框完成。长按可查看快捷操作。");
        tip(body,"日程 · 月历与课表","点日期查看当天安排，从「课表」导入课程。左右滑动切换已显示的栏目；今天向左滑先进入一周预览。");
        tip(body,"问序言 AI","在设置中配置模型与 API Key 后，用自然语言查询或安排日程。输入 / 可滚动查看指令，支持图片和文本附件；聊天顶部可切换自动批准，助手操作可撤销。");
        tip(body,"设置 · 更多功能","笔记、收集箱等功能可从更多功能打开。外观设置可调整主界面栏目与卡片列数；数据与同步中可备份或连接服务器。");
    }
    private void summary(LinearLayout body){
        tip(body,mode.equals("cloud")?"云端同步，随时可配置":"先从本地开始",mode.equals("cloud")?"下一步填写服务器地址和账号。登录成功后才启用自动同步；也可以稍后配置。":"核心功能无需联网。以后可在「设置 → 同步」连接服务器，已有本地内容会保留。");
        tip(body,"按需开启权限","日程提醒、语音、天气定位和悬浮小窗会在使用时申请相应权限。可以先使用基本功能。");
        body.addView(Ui.help(a,"想再看一遍时，在设置中打开「使用引导」，或输入 /guide。"));
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
