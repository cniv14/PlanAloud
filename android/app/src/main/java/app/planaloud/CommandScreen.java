package app.planaloud;

import android.text.*;
import android.widget.*;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/** Discoverable palette, plus allowlisted routes for UI-only actions from the terminal. */
final class CommandScreen {
    private static final Map<String,Route> routes=new LinkedHashMap<>();
    private static final Map<String,Ticket> tickets=new LinkedHashMap<>();
    private static final class Route{final String label;final Consumer<MainActivity> open;Route(String name,Consumer<MainActivity> action){label=name;open=action;}}
    private static final class Ticket{final String page;final long expires;Ticket(String page){this.page=page;expires=android.os.SystemClock.elapsedRealtime()+30000;}}
    private static void route(String id,String name,Consumer<MainActivity> action){routes.put(id,new Route(name,action));}
    static {
        route("today","今天",a->a.openPage("agenda"));route("week","一周预览",a->a.showWeek(LocalDate.now()));route("calendar","月历与日程",a->a.openSchedule(LocalDate.now()));
        route("tasks","全部待办",MainActivity::tasks);route("agenda-editor","日程批量编辑",a->ScheduleEditor.show(a,LocalDate.now(),false));
        route("courses","课程表",ScheduleScreen::courses);route("course-screenshot","截图识别课表",a->a.pickFile(110,"image/*"));route("course-draft","校对课表草稿",a->new TimetableCapture(a,null));route("course-import","导入本地课表",a->a.pickFile(103,"*/*"));
        route("notes","笔记",a->a.openPage("note"));route("notes-import","导入 Markdown",a->a.pickFile(108,"*/*"));route("notes-export","导出全部 Markdown",MainActivity::exportAllNotes);route("note-drafts","恢复笔记草稿",NoteDrafts::show);
        route("inbox","收集箱",MainActivity::showInbox);route("inbox-ai","收集箱交给 AI",InboxAgent::quick);route("capture-rules","通知与群聊收集规则",GroupCaptureSettings::show);
        route("assistant","AI 对话",AssistantPlugin::open);route("ai-settings","AI 模型与 API 配置",AiSettings::show);
        route("projects","项目看板",a->ProjectBoard.show(a,"planned",0));route("checkins","长任务打卡",CheckIns::list);route("focus","专注计时",a->FocusTimer.show(a,null));route("search","搜索与全部安排",PlanHub::show);route("types","自定义任务类型",TaskTypes::show);
        route("almanac","黄历",a->DocumentPlugins.almanac(a,LocalDate.now()));route("weather","位置与天气",a->new SettingsPanels(a).weather());
        route("wallpaper","壁纸图库、轮播和规则",WallpaperScreen::show);route("appearance","主题与动效",a->new SettingsPanels(a).appearance());route("widget","桌面小组件样式",a->new SettingsPanels(a).widget());
        route("sync","同步与备份设置",a->new SettingsPanels(a).sync());route("account","同步账号与地址",a->new SettingsPanels(a).account());route("taskflow","连接 TaskFlow Windows",TaskFlowSettings::show);route("sync-conflicts","同步冲突",a->SyncConflicts.show(a,0));
        route("undo","撤销与误删恢复",UndoHistory::show);route("extensions","内嵌模块与存储",ExtensionHub::show);route("web-import","网页与日历导入",WebImports::show);route("updates","统一更新",UpdateCenter::show);route("licenses","开源许可",a->new SettingsPanels(a).licenses());route("permissions","提醒与精确闹钟权限",AlarmSound::permissions);
        route("voice","语音模块配置",a->a.startActivity(new android.content.Intent().setComponent(new android.content.ComponentName(a,"app.planaloud.voice.MainActivity")).putExtra("settings_only",true)));
        route("settings","设置",a->a.openPage("settings"));route("commands","指令台",CommandScreen::show);
    }
    static JSONArray catalog()throws JSONException{JSONArray result=new JSONArray();for(Map.Entry<String,Route> item:routes.entrySet())result.put(new JSONObject().put("page",item.getKey()).put("title",item.getValue().label));return result;}
    static synchronized String ticket(String page)throws Exception{if(!routes.containsKey(page))throw new Exception("未知页面；用 screen.list 查询");tickets.values().removeIf(t->t.expires<android.os.SystemClock.elapsedRealtime());if(tickets.size()>=20)throw new Exception("待打开的页面过多");String id=UUID.randomUUID().toString();tickets.put(id,new Ticket(page));return id;}
    static void launch(MainActivity a,String id){Ticket t;synchronized(CommandScreen.class){t=tickets.remove(id);}if(t==null||t.expires<android.os.SystemClock.elapsedRealtime())a.message("页面指令已失效");else open(a,t.page);}
    static String validPage(String page)throws Exception{if(!routes.containsKey(page))throw new Exception("未知页面；用 screen.list 查询");return page;}
    static void open(MainActivity a,String page){Route r=routes.get(page);if(r==null)a.message("未知页面："+page);else r.open.accept(a);}
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("指令台",a::home);page.addView(Ui.text(a,"指令台",26,true));page.addView(Ui.help(a,"搜索功能，或输入命令与 JSON。AI 和终端使用相同的命令。"));
        TextInputEditText input=Ui.field(a,page,"例如：open wallpaper 或 agenda.today","",false);
        TextView output=Ui.help(a,"");output.setTextIsSelectable(true);output.setTypeface(android.graphics.Typeface.MONOSPACE);
        page.addView(Ui.button(a,"运行指令",true,()->{String value=input.getText().toString().trim();if(value.isEmpty())return;int split=value.indexOf(' ');String name=split<0?value:value.substring(0,split),rest=split<0?"":value.substring(split+1).trim();if(name.equals("open")){open(a,rest);return;}final JSONObject[] result={null};a.job(()->{result[0]=DayCommands.execute(a,name,rest.isEmpty()?new JSONObject():new JSONObject(rest),new DayCommands.Access("command"));return result[0].optString("message","指令已完成");},()->{if(result[0].has("ticket"))launch(a,result[0].optString("ticket"));else output.setText(result[0].toString());a.refreshBackdrop();},output::setText); }));
        page.addView(output);LinearLayout cards=Ui.column(a);page.addView(cards);
        Runnable update=()->{
            String query=input.getText().toString().trim().toLowerCase(Locale.ROOT);if(query.startsWith("open "))query=query.substring(5).trim();cards.removeAllViews();List<android.view.View> found=new ArrayList<>();
            for(Map.Entry<String,Route> item:routes.entrySet())if((item.getKey()+item.getValue().label).toLowerCase(Locale.ROOT).contains(query))found.add(Ui.tile(a,item.getValue().label,"open "+item.getKey(),()->open(a,item.getKey())));
            for(DayCommands.Entry entry:DayCommands.entries())if((entry.id+entry.help).toLowerCase(Locale.ROOT).contains(query))found.add(Ui.tile(a,entry.help,entry.id,()->{input.setText(entry.id+" "+entry.example);input.setSelection(input.length());}));
            if(found.isEmpty()){cards.addView(Ui.help(a,"未匹配到指令；输入 help 查看参数。"));return;}
            int columns=a.getResources().getConfiguration().fontScale>=1.35f?1:2;LinearLayout line=null;for(int i=0;i<Math.min(16,found.size());i++){if(i%columns==0){line=Ui.row(a);cards.addView(line);}Ui.cell(a,line,found.get(i));}if(columns==2&&Math.min(16,found.size())%2==1)Ui.cell(a,line,new android.view.View(a));if(found.size()>16)cards.addView(Ui.help(a,"另有 "+(found.size()-16)+" 项，继续输入以筛选。"));
        };
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){update.run();}public void afterTextChanged(Editable e){}});update.run();
        MaterialSwitch adb=new MaterialSwitch(a);adb.setText("允许已授权电脑通过 ADB 运行命令");adb.setChecked(a.prefs.getBoolean("command_adb",false));page.addView(adb);adb.setOnCheckedChangeListener((v,on)->a.prefs.edit().putBoolean("command_adb",on).apply());
        MaterialSwitch approval=new MaterialSwitch(a);approval.setText("允许 AI 自动批准待核验操作");approval.setChecked(a.prefs.getBoolean("assistant_auto_approve",false));page.addView(approval);approval.setOnCheckedChangeListener((v,on)->a.prefs.edit().putBoolean("assistant_auto_approve",on).apply());
        page.addView(Ui.help(a,"默认在聊天里批准。自动批准开启后，AI 可以执行删除、候选转日程等带审批的操作；系统权限仍需手动确认。"));
    }
}
