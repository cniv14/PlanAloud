package app.planaloud;

import android.content.*;
import org.json.*;
import java.io.*;
import java.time.*;
import java.util.*;

/** A JSON command registry shared by the terminal, local palette, and authenticated assistant. */
final class DayCommands {
    interface Run{JSONObject run(Context c,JSONObject args,Access access)throws Exception;}
    static final class Access {
        final String source,session;final boolean ai,review,approved;
        Access(String source){this(source,"",false,false,false);}
        Access(String source,String session,boolean ai,boolean review,boolean approved){this.source=source;this.session=session;this.ai=ai;this.review=review;this.approved=approved;}
        Access approved(){return new Access(source,session,ai,review,true);}
    }
    static final class Entry {
        final String id,help,example;final boolean approval,ai;final Run run;
        Entry(String id,String help,String example,boolean approval,boolean ai,Run run){this.id=id;this.help=help;this.example=example;this.approval=approval;this.ai=ai;this.run=run;}
    }
    private static final Map<String,Entry> commands=new LinkedHashMap<>();
    private static final Object APPROVAL_LOCK=new Object();
    private static final String APPROVALS="assistant_pending_approvals";

    private static JSONObject approvals(Context c)throws JSONException{
        JSONObject items=new JSONObject(prefs(c).getString(APPROVALS,"{}"));
        long now=System.currentTimeMillis();
        for(Iterator<String> keys=items.keys();keys.hasNext();)
            if(items.getJSONObject(keys.next()).optLong("expires")<=now)keys.remove();
        return items;
    }

    static JSONArray pendingApprovals(Context c)throws JSONException{
        synchronized(APPROVAL_LOCK){
            JSONObject items=approvals(c);JSONArray rows=new JSONArray();
            for(Iterator<String> keys=items.keys();keys.hasNext();){
                String token=keys.next();JSONObject item=items.getJSONObject(token);
                rows.put(new JSONObject().put("token",token).put("message",item.getString("message")));
            }
            return rows;
        }
    }
    private static void add(String id,String title,String example,boolean approval,boolean ai,Run run){commands.put(id,new Entry(id,title,example,approval,ai,run));}
    static Collection<Entry> entries(){return Collections.unmodifiableCollection(commands.values());}
    static {
        add("help","查找命令及参数示例","{}",false,true,(c,a,x)->catalog(a.optString("query"),x));
        add("agenda.today","查看某天待办","{}",false,true,(c,a,x)->page(Data.get(c).dayTasks(date(a)),a));
        add("agenda.week","查看未来七天安排与天气","{}",false,true,(c,a,x)->week(c,date(a)));
        add("agenda.deadlines","查看当天截止事项","{}",false,true,(c,a,x)->page(Data.get(c).deadlines(date(a)),a));
        add("record.search","搜索待办、笔记、课程、收集箱等","{\"query\":\"课程\"}",false,true,(c,a,x)->page(Data.get(c).business(a.optString("query"),a.optString("kind",null),offset(a)),a,true));
        add("record.get","按 ID 读取事项及版本","{\"id\":\"\"}",false,true,(c,a,x)->summary(resolve(c,a)));
        add("record.create","创建任务、日程、笔记、课程、长任务或收集条目","{\"kind\":\"task\",\"title\":\"阅读\",\"estimate_minutes\":25,\"priority\":\"high\"}",false,true,DayCommands::create);
        add("record.update","按 ID 与版本修改事项（可清空可选字段）","{\"id\":\"\",\"revision\":\"\",\"changes\":{\"due_date\":null}}",false,true,(c,a,x)->mutate(c,a,x,"update"));
        add("record.complete","完成或恢复待办","{\"id\":\"\",\"revision\":\"\",\"completed\":true}",false,true,(c,a,x)->mutate(c,a,x,"complete"));
        add("record.delete","软删除事项，可撤销","{\"id\":\"\",\"revision\":\"\"}",true,true,(c,a,x)->mutate(c,a,x,"delete"));
        add("inbox.confirm","批准一个收集箱候选并转入日程","{\"id\":\"\",\"revision\":\"\",\"kind\":\"task\"}",true,true,(c,a,x)->mutate(c,a,x,"confirm"));
        add("inbox.list","查看收集箱","{}",false,true,(c,a,x)->page(Data.get(c).inboxPage(offset(a)),a,true));
        add("capture.get","查看通知、短信与群聊收集规则","{}",false,true,(c,a,x)->capture(c));
        add("capture.set","配置通知/群聊来源与正则","{\"capture_all_messages\":true}",true,true,(c,a,x)->setCapture(c,a));
        add("undo.latest","撤销最近一次删除或修改","{}",true,true,(c,a,x)->msg(x.ai?UndoHistory.latestAssistant(c):UndoHistory.latest(c,null)));
        add("undo.restore","恢复指定撤销记录","{\"key\":\"\"}",true,false,(c,a,x)->msg(UndoHistory.restore(c,a.getString("key"))));
        add("sync.run","执行已配置的同步","{}",true,true,(c,a,x)->msg(Network.sync(c)));
        add("sync.status","查看同步结果","{}",false,true,(c,a,x)->new JSONObject().put("message",prefs(c).getString("sync_message","尚未同步")).put("last",prefs(c).getString("sync_last","")));
        add("sync.login","登录或注册 DayFlow 账号（用私有参数文件）","{\"address\":\"\",\"username\":\"\",\"password\":\"\"}",true,false,(c,a,x)->msg(PlannerServices.login(c,a.getString("address"),a.getString("username"),a.getString("password"),a.optString("invite"),a.optBoolean("register"))));
        add("sync.connect-taskflow","连接 TaskFlow Windows（用私有参数文件）","{\"address\":\"\",\"password\":\"\"}",true,false,(c,a,x)->msg(TaskFlowAuth.login(c,a.getString("address"),a.getString("password"))));
        add("sync.logout","断开当前同步账号，保留本地数据","{}",true,false,(c,a,x)->msg(TaskFlowSettings.logout(c)));
        add("sync.conflicts","查看同步冲突","{}",false,true,(c,a,x)->page(Data.get(c).conflictsPage(offset(a)),a,true));
        add("sync.resolve","按版本处理同步冲突","{\"id\":\"\",\"revision\":\"\",\"original_revision\":\"\",\"choice\":\"both\"}",true,true,(c,a,x)->{if(x.review)throw new IOException("资料整理会话不能处理同步冲突");Data.get(c).settleConflict(a.getString("id"),a.getString("revision"),a.optString("original_revision"),a.getString("choice"));changed(c);return msg("同步冲突已处理");});
        add("assistant.ask","直接在命令行向已配置的 AI 提问","{\"text\":\"查看明天的安排\"}",false,false,(c,a,x)->AssistantPlugin.commandAsk(c,a));
        add("assistant.approve","批准或拒绝终端 AI 的待批准操作","{\"token\":\"\",\"approved\":true}",false,false,(c,a,x)->AssistantPlugin.commandApprove(c,a));
        add("backup.import","导入终端已上传的 .dayflow 备份","{\"file\":\"\"}",true,false,(c,a,x)->{try(InputStream in=new FileInputStream(upload(c,a))){return msg(Data.get(c).importStream(in));}});
        add("notes.import","导入 Markdown 文本或终端上传的 .md 文件","{\"title\":\"新笔记\",\"content\":\"# 标题\"}",false,true,(c,a,x)->{JSONObject value=new JSONObject(a.toString()).put("kind","note");if(a.has("file")&&x.ai)throw new IOException("AI 无法读取终端上传文件");if(a.has("file"))try(InputStream in=new FileInputStream(upload(c,a))){value.put("content",MarkdownFiles.read(in));}return create(c,value,x);});
        add("calendar.import","导入 CSV、ICS 或 HTML 课表","{\"format\":\"csv\",\"content\":\"\"}",true,true,DayCommands::calendarImport);
        add("course.stage","生成待批准的课程草稿","{\"courses\":[],\"config\":{}}",false,true,(c,a,x)->TimetableCapture.commandStage(c,a));
        add("course.confirm","核对并导入课程草稿","{\"revision\":\"\"}",true,true,(c,a,x)->TimetableCapture.commandConfirm(c,a));
        add("course.draft","查看当前课程草稿","{}",false,true,(c,a,x)->TimetableCapture.commandDraft(c));
        add("project.checkin","为长任务打卡","{\"id\":\"\",\"content\":\"\"}",false,true,DayCommands::checkin);
        add("focus.start","启动专注计时","{\"minutes\":25}",false,true,(c,a,x)->FocusTimer.command(c,"start",a));
        add("focus.status","查看专注计时","{}",false,true,(c,a,x)->FocusTimer.command(c,"status",a));
        add("focus.finish","结束专注并写入笔记","{}",false,true,(c,a,x)->FocusTimer.command(c,"finish",a));
        add("focus.cancel","取消专注计时","{}",true,true,(c,a,x)->FocusTimer.command(c,"cancel",a));
        add("weather.get","查看天气缓存","{}",false,true,(c,a,x)->msg(Forecast.summary(c,date(a))));
        add("weather.refresh","刷新当前城市预报","{}",false,true,(c,a,x)->msg(Forecast.refresh(c)));
        add("almanac.get","查看日期的农历、宜忌与调休","{}",false,true,(c,a,x)->{LocalDate d=date(a);com.nlf.calendar.Lunar l=ChineseDates.lunar(d);return new JSONObject().put("date",d.toString()).put("lunar",ChineseDates.description(d)).put("holiday",Holidays.description(d)).put("good",new JSONArray(l.getDayYi())).put("avoid",new JSONArray(l.getDayJi()));});
        add("wallpaper.get","查看壁纸与规则配置","{}",false,true,(c,a,x)->WallpaperRules.read(c));
        add("wallpaper.set","验证并保存壁纸 JSON 配置","{\"config\":{\"schema\":1,\"images\":[],\"rules\":[],\"rotate_minutes\":0}}",true,true,(c,a,x)->{WallpaperRules.save(c,a.getJSONObject("config"));AgendaWidget.refresh(c);return msg("已保存壁纸规则");});
        add("wallpaper.add","从终端上传壁纸","{\"file\":\"\",\"name\":\"晚霞\"}",false,false,(c,a,x)->new JSONObject().put("id",WallpaperImages.add(c,upload(c,a),a.optString("name","壁纸"))));
        add("wallpaper.remove","删除指定壁纸及其引用规则","{\"id\":\"\"}",true,true,(c,a,x)->{WallpaperRules.remove(c,a.getString("id"));AgendaWidget.refresh(c);return msg("已删除壁纸");});
        add("widget.image","从终端上传小组件背景","{\"file\":\"\"}",false,false,(c,a,x)->{WallpaperImages.importWidget(c,upload(c,a));AgendaWidget.refresh(c);return msg("小组件背景已更新");});
        add("widget.refresh","刷新桌面小组件","{}",false,true,(c,a,x)->{AgendaWidget.refresh(c);return msg("已请求刷新");});
        add("settings.get","查看可脚本配置的偏好（不含密钥）","{}",false,true,(c,a,x)->settings(c));
        add("settings.set","设置主题、动效、布局、同步与更新等偏好","{\"key\":\"widget_layout\",\"value\":\"grid\"}",true,true,DayCommands::setting);
        add("updates.check","检查基础 APK 更新，不启动安装","{}",false,true,(c,a,x)->new JSONObject().put("available",Optional.ofNullable(Updates.check(c)).orElse(new JSONObject())));
        add("updates.modules","下载已签名的模块更新","{}",true,true,(c,a,x)->msg(UpdateCenter.updateModules(c)));
        add("updates.rollback","回退某个功能模块","{\"id\":\"assistant\"}",true,true,(c,a,x)->{String id=a.getString("id");if(!Arrays.asList(ModuleUpdates.IDS).contains(id))throw new IOException("未知模块");return msg(ModuleUpdates.rollback(c,id));});
        add("screen.list","列出所有可打开的功能页面","{}",false,true,(c,a,x)->new JSONObject().put("pages",CommandScreen.catalog()));
        add("screen.open","打开界面（需要系统界面的操作从这里进入）","{\"page\":\"widget\"}",true,true,(c,a,x)->x.ai?new JSONObject().put("screen",CommandScreen.validPage(a.getString("page"))):new JSONObject().put("ticket",CommandScreen.ticket(a.getString("page"))));
    }
    static JSONObject execute(Context c,String id,JSONObject args,Access x)throws Exception{
        Entry e=commands.get(id);if(e==null)throw new IOException("未知命令："+id+"；用 help 查看");
        if(x.ai&&!e.ai)throw new IOException("此命令只允许用户或已授权终端调用");
        if(x.ai&&e.approval&&!x.approved&&!prefs(c).getBoolean("assistant_auto_approve",false)){
            String token=UUID.randomUUID().toString();
            String detail="";
            if(args.has("id")&&(id.startsWith("record.")||id.equals("inbox.confirm"))){JSONObject r=resolve(c,args);detail=r.optString("title")+" · "+PlanTime.label(r);}
            if(id.equals("course.confirm")){JSONObject draft=TimetableCapture.commandDraft(c);if(!draft.getString("revision").equals(args.getString("revision")))throw new IOException("课表草稿已变化");JSONArray items=draft.getJSONArray("items");StringBuilder rows=new StringBuilder(draft.optString("term")+" · "+items.length()+" 门课程");for(int n=0;n<Math.min(6,items.length());n++)rows.append('\n').append(items.getJSONObject(n).optString("title"));detail=rows.toString();}
            if(id.equals("screen.open"))detail=args.optString("page");
            if(id.equals("settings.set"))detail=args.optString("key")+" → "+args.optString("value");
            String message=e.help+(detail.isEmpty()?"":"\n"+detail);
            synchronized(APPROVAL_LOCK){
                JSONObject items=approvals(c);
                if(items.length()>=32)throw new IOException("待批准命令过多，请先到收集箱处理");
                items.put(token,new JSONObject().put("command",id).put("args",args).put("message",message)
                        .put("source",x.source).put("session",x.session).put("review",x.review)
                        .put("expires",System.currentTimeMillis()+24*3600000L));
                if(!prefs(c).edit().putString(APPROVALS,items.toString()).commit())throw new IOException("未能保存待批准操作");
            }
            return new JSONObject().put("requires_approval",true).put("approval_token",token).put("command",id).put("message",message);
        }
        return e.run.run(c,args,x);
    }
    static JSONObject approve(Context c,String token,String session,boolean allowed)throws Exception{
        JSONObject item;
        synchronized(APPROVAL_LOCK){
            JSONObject items=approvals(c);
            item=items.optJSONObject(token);
            if(item==null||(session!=null&&!item.optString("session").equals(session)))
                throw new IOException("这条批准已处理或失效");
            items.remove(token);
            if(!prefs(c).edit().putString(APPROVALS,items.toString()).commit())throw new IOException("未能更新待批准操作");
        }
        if(!allowed)return msg("已拒绝，未执行命令");
        Access access=new Access(item.getString("source"),item.getString("session"),true,item.optBoolean("review"),true);
        return execute(c,item.getString("command"),item.getJSONObject("args"),access);
    }
    private static JSONObject catalog(String query,Access x)throws Exception{JSONArray entries=new JSONArray();String key=query.toLowerCase(Locale.ROOT);for(Entry e:commands.values())if((!x.ai||e.ai)&&(e.id+e.help).toLowerCase(Locale.ROOT).contains(key))entries.put(new JSONObject().put("name",e.id).put("description",e.help).put("arguments",new JSONObject(e.example)).put("approval",e.approval));return new JSONObject().put("commands",entries);}
    static JSONObject msg(String value)throws JSONException{return new JSONObject().put("message",value);}
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("settings",0);}
    private static LocalDate date(JSONObject a){return LocalDate.parse(a.optString("date",LocalDate.now().toString()));}
    private static int offset(JSONObject a){return Math.max(0,a.optInt("offset",0));}
    private static JSONObject week(Context c,LocalDate first)throws Exception{JSONArray days=new JSONArray();for(int i=0;i<7;i++){LocalDate day=first.plusDays(i);Data db=Data.get(c);days.put(new JSONObject().put("date",day.toString()).put("weather",Forecast.summary(c,day)).put("scheduled",page(db.scheduled(day,day.plusDays(1),false),new JSONObject())).put("deadlines",page(db.deadlines(day),new JSONObject())));}return new JSONObject().put("days",days);}
    private static JSONObject page(JSONArray input,JSONObject a)throws Exception{return page(input,a,false);}
    private static JSONObject page(JSONArray input,JSONObject a,boolean alreadyOffset)throws Exception{int base=alreadyOffset?offset(a):0,start=alreadyOffset?0:offset(a),end=Math.min(input.length(),(int)Math.min((long)start+16,Integer.MAX_VALUE));JSONArray out=new JSONArray();for(int i=start;i<end;i++)out.put(summary(input.getJSONObject(i)));return new JSONObject().put("items",out).put("next_offset",(long)base+end).put("has_more",end<input.length());}
    private static JSONObject resolve(Context c,JSONObject a)throws Exception{JSONObject r=Data.get(c).resolve(a.getString("id"),a.optString("series_id"),a.optString("occurrence_date"));if(r==null||r.optBoolean("deleted"))throw new IOException("事项不存在或已删除");return r;}
    private static JSONObject summary(JSONObject r)throws Exception{JSONObject out=new JSONObject();for(String k:new String[]{"id","revision","kind","title","due_date","due_time","estimate_minutes","priority","start_at","end_at","location","completed","status","recurrence","series_id","occurrence_date","deleted","confirmation_status","parent_id","type_id"})if(r.has(k)){Object v=r.get(k);if(v instanceof String&&((String)v).length()>240)v=((String)v).substring(0,240);out.put(k,v);}String content=r.optString("content");return out.put("preview",content.substring(0,Math.min(300,content.length())));}
    private static final Set<String> fields=new HashSet<>(Arrays.asList("title","content","start_at","end_at","due_date","due_time","estimate_minutes","priority","location","time_hint","all_day","reminder","remind_minutes","reminder_mode","reminder_offsets","recurrence","recurrence_until","weekdays","parent_id","type_id","pinned","workdays_only","checkin_goal","base_kind","icon","color"));
    private static void patch(JSONObject r,JSONObject values,boolean strict)throws Exception{
        for(Iterator<String> it=values.keys();it.hasNext();){String key=it.next();if(!fields.contains(key)){if(strict)throw new IOException("不能修改字段："+key);continue;}Object value=values.get(key);if(value==JSONObject.NULL)r.remove(key);else r.put(key,value);}
        if(r.optString("due_date").isEmpty()&&!values.has("due_time"))r.remove("due_time");
        if(values.has("end_at"))r.put("time_mode",r.optString("end_at").isEmpty()?"start":"end");
        if(!r.optString("end_at").isEmpty()&&(r.optString("start_at").isEmpty()||!LocalDateTime.parse(r.getString("end_at")).isAfter(LocalDateTime.parse(r.getString("start_at")))))throw new IOException("结束时间必须晚于开始时间");
        if(r.optString("title").trim().isEmpty())throw new IOException("请填写标题");
        if(r.optBoolean("reminder"))PlanOptions.offsets(r);Data.validate(r);
    }
    private static JSONObject create(Context c,JSONObject a,Access x)throws Exception{
        String wanted=a.optString("kind","task"),kind=wanted;if(x.review&&!kind.equals("inbox"))kind="inbox";
        if(!Arrays.asList("task","event","course","project","note","inbox","type").contains(kind))throw new IOException("未知事项类型");
        if(a.has("file")&&x.ai)throw new IOException("AI 无法读取终端上传文件");
        JSONObject r=Data.make(kind,a.getString("title"));patch(r,a,false);
        if(kind.equals("note"))r.put("content_format","markdown");
        if(kind.equals("inbox"))r.put("confirmation_status","pending").put("proposed_kind",Arrays.asList("task","event","course","project").contains(wanted)?wanted:"task");
        Data.get(c).save(r.put("_undo_record",true).put("_undo_source",x.source));changed(c);return summary(r).put("created",true);
    }
    private static JSONObject mutate(Context c,JSONObject a,Access x,String op)throws Exception{
        Data db=Data.get(c);synchronized(db){JSONObject r=resolve(c,a);if(!r.optString("revision").equals(a.getString("revision")))throw new IOException("事项已变化，请先查询新版本");
            boolean inbox="inbox".equals(r.optString("kind"));if(x.review&&(!inbox||op.equals("delete")||op.equals("complete")))throw new IOException("资料整理会话不能修改已有日程");
            if(op.equals("complete")){if(!Arrays.asList("task","event","course","project").contains(r.optString("kind")))throw new IOException("此事项不能标记完成");if(r.optString("completed").isEmpty()==a.getBoolean("completed")){TaskActions.complete(c,r);r=resolve(c,a);}}
            else {if(op.equals("update"))patch(r,a.getJSONObject("changes"),true);
                else if(op.equals("delete"))r.put("deleted",true);
                else {if(!inbox)throw new IOException("只能批准收集箱候选");String kind=a.optString("kind",r.optString("proposed_kind","task"));if(!Arrays.asList("task","event","course","project").contains(kind))throw new IOException("无效的日程类型");r.put("kind",kind).put("confirmation_status","confirmed");patch(r,a.optJSONObject("changes")==null?new JSONObject():a.getJSONObject("changes"),true);}
                db.save(r.put("_undo_record",true).put("_undo_source",x.source));}
            changed(c);return summary(r).put("changed",true);
        }
    }
    private static void changed(Context c){AgendaWidget.refresh(c);ReminderReceiver.refresh(c);}
    private static File upload(Context c,JSONObject a)throws Exception{return CommandProvider.staged(c,a.getString("file"));}
    private static JSONObject calendarImport(Context c,JSONObject a,Access x)throws Exception{
        if(x.review)throw new IOException("资料整理请用 course.stage 生成草稿，再在聊天批准");String text=a.optString("content");if(a.has("file")){if(x.ai)throw new IOException("AI 无法读取终端上传文件");try(InputStream in=new FileInputStream(upload(c,a))){text=Network.read(in,4*1024*1024);}}
        JSONArray items;switch(a.getString("format")){case "csv":items=Imports.timetable(text);break;case "ics":items=Imports.ics(text);break;case "html":items=WebImports.html(text);break;default:throw new IOException("格式需为 csv、ics 或 html");}
        Data db=Data.get(c);int count=0;for(int i=0;i<items.length();i++)Data.validate(items.getJSONObject(i));synchronized(db){for(int i=0;i<items.length();i++){JSONObject r=items.getJSONObject(i);if(db.find(r.getString("id"))!=null)continue;db.save(r.put("_undo_record",true).put("_undo_source",x.source));count++;}}changed(c);return new JSONObject().put("imported",count);
    }
    private static JSONObject checkin(Context c,JSONObject a,Access x)throws Exception{if(x.review)throw new IOException("资料整理会话不能打卡");LocalDate day=date(a);if(day.isAfter(LocalDate.now()))throw new IOException("不能为未来日期打卡");Data db=Data.get(c);synchronized(db){JSONObject p=resolve(c,a);if(!p.optString("kind").equals("project"))throw new IOException("请指定长任务");String id=CheckIns.id(p.getString("id"),day);JSONObject r=db.find(id);if(r==null)r=Data.make("note",p.optString("title")+" · "+day+" 打卡");r.put("id",id).put("deleted",false).put("parent_id",p.getString("id")).put("checkin_for",p.getString("id")).put("checkin_date",day.toString()).put("content_format","markdown").put("content",a.optString("content"));Data.validate(r);db.save(r.put("_undo_record",true).put("_undo_source",x.source));return summary(r);}}
    private static final Set<String> CAPTURE_BOOL=new HashSet<>(Arrays.asList("capture","capture_all_messages","capture_all_apps","capture_sms","capture_group_filter"));
    private static final Set<String> CAPTURE_TEXT=new HashSet<>(Arrays.asList("packages","capture_groups","capture_regex_include","capture_regex_exclude"));
    private static JSONObject capture(Context c)throws Exception{JSONObject result=new JSONObject();Map<String,?> values=prefs(c).getAll();for(String key:CAPTURE_BOOL)if(values.containsKey(key))result.put(key,values.get(key));for(String key:CAPTURE_TEXT)if(values.containsKey(key))result.put(key,values.get(key));return result;}
    private static JSONObject setCapture(Context c,JSONObject a)throws Exception{
        android.content.SharedPreferences.Editor edit=prefs(c).edit();for(Iterator<String> it=a.keys();it.hasNext();){String key=it.next();Object value=a.get(key);if(CAPTURE_BOOL.contains(key)&&value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(CAPTURE_TEXT.contains(key)&&value instanceof String){if(key.startsWith("capture_regex"))CaptureRules.validate((String)value);edit.putString(key,(String)value);}else throw new IOException("无效的收集规则："+key);}if(!edit.commit())throw new IOException("无法保存收集规则");return msg("已保存；系统通知读取授权需在 Android 设置中确认");
    }
    private static final Set<String> BOOL=new HashSet<>(Arrays.asList("compact_ui","reduce_motion","dynamic_color","motion","blur","widget_inbox","sync_auto","updates_auto","hot_enabled","markdown_auto_open","swipe","assistant_auto_approve","plugin_enabled_app.planaloud.assistant","plugin_enabled_app.planaloud.markdown","plugin_enabled_app.planaloud.almanac","plugin_enabled_app.planaloud.voice"));
    private static final Map<String,List<String>> CHOICES=new LinkedHashMap<>();static{CHOICES.put("theme",Arrays.asList("system","light","dark"));CHOICES.put("widget_layout",Arrays.asList("list","grid"));CHOICES.put("widget_background",Arrays.asList("glass","solid","image","wallpaper"));CHOICES.put("updates_source",Arrays.asList("github","server"));CHOICES.put("sync_backend",Arrays.asList("dayflow","taskflow"));}
    private static JSONObject settings(Context c)throws Exception{JSONObject out=new JSONObject();Map<String,?> values=prefs(c).getAll();for(String k:BOOL)if(values.containsKey(k))out.put(k,values.get(k));for(String k:CHOICES.keySet())if(values.containsKey(k))out.put(k,values.get(k));out.put("widget_tint",prefs(c).getInt("widget_tint",73));return out;}
    private static JSONObject setting(Context c,JSONObject a,Access x)throws Exception{
        String key=a.getString("key");Object value=a.get("value");if(key.equals("assistant_auto_approve")&&x.ai)throw new IOException("AI 不能改变自己的审批权限");
        android.content.SharedPreferences.Editor edit=prefs(c).edit();
        if(BOOL.contains(key)&&value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(CHOICES.containsKey(key)&&CHOICES.get(key).contains(value))edit.putString(key,(String)value);else if(key.equals("widget_tint")&&value instanceof Integer&&(Integer)value>=48&&(Integer)value<=94)edit.putInt(key,(Integer)value);else throw new IOException("不支持的设置或值");
        if(!edit.commit())throw new IOException("设置保存失败");if(key.equals("theme"))new android.os.Handler(android.os.Looper.getMainLooper()).post(()->DayFlowApp.applyTheme((String)value));if(key.equals("sync_auto")||key.equals("sync_backend"))SyncJob.configure(c);if(key.equals("updates_auto"))Updates.configureJob(c);changed(c);return msg("已保存 "+key);
    }
}
