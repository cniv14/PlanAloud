package app.planaloud;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.*;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import org.json.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Native Material navigation. All editing and settings use full pages, not action dialogs. */
public final class MainActivity extends AppCompatActivity {
 @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(app.planaloud.ui.UiScale.wrap(base));}

    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    public SharedPreferences prefs;public EditorPanel editorState;public NoteEditor noteState;public LocationWeather locationWeather;
    private com.google.android.material.button.MaterialButton agentBar;private LinearLayout shell,agentRow;private FrameLayout content;private BottomNavigationView navigationBar;private LinearProgressIndicator progress;private Backdrop backdrop;private boolean selectingTab,handlingNavigationTap,rootPage;private int navigationMask=-1;private PageSwipe pageSwipe;Onboarding onboarding;DisplaySettings.TrialPrompt displayTrial;private String screenName="";private long screenGeneration;
    private String tab="agenda";private boolean dayView,busy,weekPreview;private int transitionDirection,scheduleScrollY;private ScrollView scheduleScroll;private LocalDate selected=LocalDate.now(),weekStart=LocalDate.now();private Runnable backAction;
    private String moreFeaturesTab, moreFeaturesQuery="";
    private int moreFeaturesScroll;
    private LocalDate moreFeaturesDate, moreFeaturesWeekStart;
    private boolean moreFeaturesWeekPreview;
    private int moreFeaturesScheduleScroll;

    void rememberMoreFeatures(String query,int scroll){
        if(moreFeaturesTab==null){
            moreFeaturesTab=tab;
            moreFeaturesDate=selected;
            moreFeaturesWeekStart=weekStart;
            moreFeaturesWeekPreview=weekPreview;
            moreFeaturesScheduleScroll=scheduleScroll==null?scheduleScrollY:scheduleScroll.getScrollY();
        }
        moreFeaturesQuery=query;
        moreFeaturesScroll=scroll;
    }

    void leaveMoreFeatures(){
        if(moreFeaturesTab!=null){
            tab=moreFeaturesTab;
            selected=moreFeaturesDate;
            weekStart=moreFeaturesWeekStart;
            weekPreview=moreFeaturesWeekPreview;
            scheduleScroll=null;
            scheduleScrollY=moreFeaturesScheduleScroll;
        }
        moreFeaturesTab=null;
        moreFeaturesDate=null;
        moreFeaturesWeekStart=null;
        moreFeaturesQuery="";
        moreFeaturesScroll=0;
        renderHome();
    }
    public void onCreate(Bundle state){super.onCreate(state);ThemeColors.apply(this);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);prefs=getSharedPreferences("settings",0);weekStart=WeekOptions.first(this,weekStart);tab=HomeNavigation.first(visibleNavigationMask());Onboarding.prepare(prefs,!prefs.contains("device_id"));ProviderAppearance.syncLauncher(this,prefs);if(!prefs.contains("device_id"))prefs.edit().putString("device_id",UUID.randomUUID().toString()).apply();locationWeather=new LocationWeather(this);
        NotificationManager channels=getSystemService(NotificationManager.class);NotificationChannel schedule=new NotificationChannel("schedule","日程提醒",NotificationManager.IMPORTANCE_HIGH);schedule.setDescription("日程到点提醒；闹钟响铃使用独立前台服务");schedule.enableVibration(true);schedule.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());channels.createNotificationChannel(schedule);
        FrameLayout base=new FrameLayout(this);backdrop=new Backdrop(this);base.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));shell=Ui.column(this);base.addView(shell,new FrameLayout.LayoutParams(-1,-1));setContentView(base);
        ViewCompat.setOnApplyWindowInsetsListener(base,(v,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());androidx.core.graphics.Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());shell.setPadding(bars.left,bars.top,bars.right,bars.bottom);if(content!=null)content.setPadding(0,0,0,Math.max(0,ime.bottom-bars.bottom-(rootPage&&navigationBar!=null?navigationBar.getHeight():0)));return WindowInsetsCompat.CONSUMED;});
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){if(backAction!=null)backAction.run();else if(!"agenda".equals(tab)){tab="agenda";home();}else moveTaskToBack(true);}});
        backdrop.load();home();Updates.configureJob(this);HotUpdateJob.configure(this);
        if(state!=null){scheduleScrollY=state.getInt("schedule_scroll");tab=state.getString("tab","agenda");dayView=state.getBoolean("day",false);weekPreview=state.getBoolean("week_preview",false);if(weekPreview&&tab.equals("agenda"))tab="week";selected=LocalDate.parse(state.getString("date",LocalDate.now().toString()));weekStart=LocalDate.parse(state.getString("week_start",LocalDate.now().toString()));
        moreFeaturesTab=state.getString("more_features_tab");
        if(moreFeaturesTab!=null){
            moreFeaturesQuery=state.getString("more_features_query","");
            moreFeaturesScroll=state.getInt("more_features_scroll");
            moreFeaturesDate=LocalDate.parse(state.getString("more_features_date",selected.toString()));
            moreFeaturesWeekStart=LocalDate.parse(state.getString("more_features_week",weekStart.toString()));
            moreFeaturesWeekPreview=state.getBoolean("more_features_week_preview");
            moreFeaturesScheduleScroll=state.getInt("more_features_schedule_scroll");
        }
        home();if(state.containsKey("editor")||state.containsKey("editor_file"))try{edit(state.containsKey("editor_file")?EditorSnapshot.read(this,state.getString("editor_file")):new JSONObject(state.getString("editor")));if(noteState!=null&&state.containsKey("md_uri"))noteState.bindExternal(Uri.parse(state.getString("md_uri")),state.getBoolean("md_writable"));}catch(Exception ignored){}else if(moreFeaturesTab==null&&"更多功能".equals(state.getString("screen")))CommandScreen.show(this);else if("外观与主题".equals(state.getString("screen")))new SettingsPanels(this).appearance();else if("界面风格".equals(state.getString("screen")))UiStyle.show(this);else if("界面与字体大小".equals(state.getString("screen")))DisplaySettings.show(this);else if("应用图标".equals(state.getString("screen")))ProviderAppearance.show(this);else if("AI 配置".equals(state.getString("screen")))AiSettings.show(this);}else{String initial=prefs.getString("start_page","today");if(initial.equals("manage"))PlanHub.show(this);else if(!initial.equals("today")){tab=initial;home();}if(!Onboarding.pending(prefs)&&!Onboarding.restoring(state))handleIntent(getIntent());}
        if(Onboarding.pending(prefs)||Onboarding.restoring(state))Onboarding.open(this,state!=null&&Onboarding.restoring(state)?state.getBoolean("guide_first",false):Onboarding.pending(prefs),state);
    }
    void onboardingFinished(boolean firstLaunch,boolean configureCloud){
        onboarding=null;tab=HomeNavigation.first(visibleNavigationMask());weekPreview=false;home();
        if(firstLaunch)handleIntent(getIntent());
        if(configureCloud)new SettingsPanels(this).account();
    }
    protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(onboarding==null)handleIntent(i);}
    private void handleIntent(Intent i){try{String command=i.getStringExtra("dayflow_screen_ticket");if(command!=null){CommandScreen.launch(this,command);return;}if((Intent.ACTION_VIEW.equals(i.getAction())||Intent.ACTION_EDIT.equals(i.getAction()))&&i.getData()!=null){importMarkdown(i.getData(),true,i.getFlags());return;}if(Intent.ACTION_SEND.equals(i.getAction())&&i.hasExtra(Intent.EXTRA_STREAM)){Uri file=i.getParcelableExtra(Intent.EXTRA_STREAM);if(file!=null&&MarkdownDocuments.isMarkdown(this,file)){importMarkdown(file,true,i.getFlags());return;}}if(i.hasExtra("record_id")){JSONObject r=Data.get(this).resolve(i.getStringExtra("record_id"),i.getStringExtra("series_id"),i.getStringExtra("occurrence_date"));if(r!=null&&!r.optBoolean("deleted")){if(!openSource(r))detail(r);}}else if(Intent.ACTION_SEND.equals(i.getAction())){JSONObject r=Data.make("inbox","通知 / 邮件").put("content",i.getStringExtra(Intent.EXTRA_TEXT));Data.get(this).save(r);changed();detail(r);}else if(i.getBooleanExtra("quick",false))edit(Data.make(i.getStringExtra("quick_kind")==null?"note":i.getStringExtra("quick_kind"),""));else if(i.getBooleanExtra("check_update",false))new SettingsPanels(this).updates();}catch(Exception e){message(e.getMessage());}}
    protected void onSaveInstanceState(Bundle state){if(onboarding!=null)onboarding.save(state);state.putInt("schedule_scroll",scheduleScroll==null?scheduleScrollY:scheduleScroll.getScrollY());state.putString("tab",tab);state.putString("date",selected.toString());state.putString("week_start",weekStart.toString());state.putBoolean("day",dayView);state.putBoolean("week_preview",weekPreview);state.putString("screen",screenName);
        if(moreFeaturesTab!=null){
            state.putString("more_features_tab",moreFeaturesTab);
            state.putString("more_features_query",moreFeaturesQuery);
            state.putInt("more_features_scroll",moreFeaturesScroll);
            state.putString("more_features_date",moreFeaturesDate.toString());
            state.putString("more_features_week",moreFeaturesWeekStart.toString());
            state.putBoolean("more_features_week_preview",moreFeaturesWeekPreview);
            state.putInt("more_features_schedule_scroll",moreFeaturesScheduleScroll);
        }
        if(noteState!=null){state.putString("editor_file",EditorSnapshot.write(this,noteState.draft()));if(noteState.externalUri()!=null){state.putString("md_uri",noteState.externalUri().toString());state.putBoolean("md_writable",noteState.externalWritable());}}else if(editorState!=null)state.putString("editor_file",EditorSnapshot.write(this,editorState.draft()));super.onSaveInstanceState(state);}
    protected void onResume(){super.onResume();if(DisplaySettings.resumeTrial(this))return;if(Updates.resumePending(this))return;SyncJob.configure(this);FocusTimer.restore(this);if(editorState==null&&noteState==null&&backAction==null&&"agenda".equals(tab))home();backdrop.active(true);ReminderReceiver.refresh(this);if(prefs.getBoolean("auto_location",false)&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED&&System.currentTimeMillis()-prefs.getLong("weather_updated",0)>6L*3600000)locationWeather.request();}
    protected void onPause(){if(noteState!=null)noteState.keepDraft();backdrop.active(false);locationWeather.cancel();super.onPause();}
    protected void onDestroy(){DisplaySettings.dismissTrial(this);worker.shutdown();backdrop.release();super.onDestroy();}
    public void message(String value){if(value!=null&&!isDestroyed())Snackbar.make(shell,value,Snackbar.LENGTH_LONG).show();}
    public interface Work{String run()throws Exception;}
    public void job(Work work,Runnable after){job(work,after,null);}
    public void job(Work work,Runnable after,java.util.function.Consumer<String> failed){if(busy){message("正在处理，请稍候");return;}busy=true;long origin=screenGeneration;if(progress!=null)progress.setVisibility(View.VISIBLE);worker.execute(()->{String result;boolean success;try{result=work.run();success=true;}catch(Exception e){Diagnostics.record(this,"background-operation",e);result=e.getMessage()==null?"操作失败":e.getMessage();success=false;}String value=result;boolean ok=success;runOnUiThread(()->{busy=false;if(isDestroyed())return;if(progress!=null)progress.setVisibility(View.GONE);if(ok){changed();if(after!=null&&origin==screenGeneration)after.run();}else if(failed!=null&&origin==screenGeneration)failed.accept(value);message(value);});});}
    public void changed(){AgendaWidget.refresh(this);ReminderReceiver.refresh(this);}
    private void ensureScaffold(){if(content!=null)return;
        FrameLayout body=new FrameLayout(this);shell.addView(body,new LinearLayout.LayoutParams(-1,0,1));content=new FrameLayout(this);body.addView(content,new FrameLayout.LayoutParams(-1,-1));progress=new LinearProgressIndicator(this);progress.setIndeterminate(true);progress.setVisibility(View.GONE);body.addView(progress,new FrameLayout.LayoutParams(-1,Ui.dp(this,3),Gravity.TOP));
        agentRow=Ui.row(this);
        LinearLayout.LayoutParams assistantRowLayout=new LinearLayout.LayoutParams(-1,Ui.dp(this,52));
        assistantRowLayout.setMargins(Ui.dp(this,16),Ui.dp(this,4),Ui.dp(this,16),0);
        shell.addView(agentRow,assistantRowLayout);
        agentBar=Ui.button(this,"问序言 AI",true,()->AssistantPlugin.open(this));
        FrameLayout aiContainer=new FrameLayout(this);aiContainer.addView(agentBar,new FrameLayout.LayoutParams(-1,-1));agentBar.setPadding(Ui.dp(this,48),0,Ui.dp(this,14),0);android.widget.ImageButton mic=new android.widget.ImageButton(this);mic.setImageResource(android.R.drawable.ic_btn_speak_now);mic.setImageTintList(android.content.res.ColorStateList.valueOf(Ui.color(this,com.google.android.material.R.attr.colorOnPrimary)));mic.setBackgroundColor(Color.TRANSPARENT);aiContainer.addView(mic,new FrameLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48),Gravity.START|Gravity.CENTER_VERTICAL));VoiceBridge.bind(this,mic,text->AssistantPlugin.draft(this,text),value->agentBar.setText(value));
        agentRow.addView(aiContainer,new LinearLayout.LayoutParams(0,-1,1));
        com.google.android.material.button.MaterialButton floating=Ui.iconButton(this,"",R.drawable.ic_floating_ai,false,()->FloatingAssistant.toggle(this));
        floating.setStrokeWidth(0);floating.setContentDescription("悬浮助手");floating.setTooltipText("悬浮助手");
        agentBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                LiquidGlass.tint(Ui.primary(this),238)));
        floating.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                LiquidGlass.tint(Ui.surface(this),202)));
        LinearLayout.LayoutParams floatingLayout=new LinearLayout.LayoutParams(Ui.dp(this,52),-1);
        floatingLayout.setMarginStart(Ui.dp(this,8));agentRow.addView(floating,floatingLayout);
        navigationBar=new BottomNavigationView(this);navigationBar.setItemHorizontalTranslationEnabled(false);navigationBar.setItemActiveIndicatorEnabled(true);navigationBar.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);String[] labels={UiStyle.neo(this)?"今日":"今天","笔记","日程","收集箱","设置"};int[] icons={R.drawable.ic_calendar,R.drawable.ic_note,R.drawable.ic_schedule,R.drawable.ic_inbox,R.drawable.ic_settings};refreshNavigation();navigationBar.setPadding(0,0,0,0);navigationBar.setMinimumHeight(Ui.dp(this,64));navigationBar.setBackgroundColor(Color.TRANSPARENT);navigationBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));navigationBar.setElevation(0);navigationBar.setItemIconSize(Ui.dp(this,27));navigationBar.setItemIconTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.primary(this),Ui.muted(this)}));getWindow().setNavigationBarColor(Color.TRANSPARENT);if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);LinearLayout.LayoutParams navLayout=new LinearLayout.LayoutParams(-1,-2);
        navLayout.setMargins(Ui.dp(this,12),Ui.dp(this,5),Ui.dp(this,12),Ui.dp(this,4));
        navigationBar.setBackgroundTintList(null);
        navigationBar.setBackground(LiquidGlass.panel(this,26));
        navigationBar.setItemActiveIndicatorEnabled(false);
        navigationBar.setItemBackground((android.graphics.drawable.Drawable)null);
        navigationBar.setItemTextColor(navigationBar.getItemIconTintList());
        shell.addView(navigationBar,navLayout);
        navigationBar.setOnItemSelectedListener(item->{if(selectingTab)return true;if(item.getItemId()==106){showNavigationOverflow();return false;}int index=item.getItemId()-100;if(index<0||index>=HomeNavigation.TABS.length)return false;String next=HomeNavigation.TABS[index];if(next.equals(tab))return true;int old=HomeNavigation.pages(visibleNavigationMask()).indexOf(tab);transitionDirection=HomeNavigation.pages(visibleNavigationMask()).indexOf(next)>=old?1:-1;weekPreview=next.equals("week");View focus=getCurrentFocus();if(focus!=null){focus.clearFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);}tab=next;handlingNavigationTap=true;try{renderHome();}finally{handlingNavigationTap=false;}return true;});navigationBar.setOnItemReselectedListener(item->{if(item.getItemId()==106)showNavigationOverflow();});

    }
    private void showNavigationOverflow(){android.widget.PopupMenu menu=new android.widget.PopupMenu(this,navigationBar);if(prefs.getBoolean("home_notes",false))menu.getMenu().add("笔记").setOnMenuItemClickListener(i->{openPage("note");return true;});if(prefs.getBoolean("home_inbox",false))menu.getMenu().add("收集箱").setOnMenuItemClickListener(i->{openPage("inbox");return true;});menu.show();}
    LinearLayout neoPage(){scaffold("Neo",null,true);return fixedWeekForm();}
    LinearLayout timetablePage(){scaffold("今天",null,true);return fixedWeekForm();}
    void showTimetableDay(LocalDate date){selected=date;tab="agenda";weekPreview=false;renderHome();}
    private int visibleNavigationMask(){if(UiStyle.neo(this))return HomeNavigation.WEEK|1;return HomeNavigation.mask(prefs.getBoolean("home_today",true),prefs.getBoolean("home_notes",false),prefs.getBoolean("home_schedule",true),prefs.getBoolean("home_inbox",false));}
    private boolean homeTabVisible(int index){return (visibleNavigationMask()&(1<<index))!=0;}
    void refreshNavigation(){
        if(navigationBar==null)return;
        int mask=visibleNavigationMask();
        if(mask==navigationMask)return;
        navigationMask=mask;selectingTab=true;
        try{
            navigationBar.getMenu().clear();
            String[] labels={UiStyle.neo(this)?"今日":"今天","笔记","日程","收集箱","设置","周预览"};boolean overflow=HomeNavigation.pages(mask).size()>5;
            for(String page:HomeNavigation.pages(mask)){int n=HomeNavigation.itemId(page)-100;if(overflow&&(n==1||n==3))continue;if(overflow&&n==4)navigationBar.getMenu().add(0,106,4,"更多").setIcon(R.drawable.ic_more).setCheckable(true);navigationBar.getMenu().add(0,100+n,HomeNavigation.pages(mask).indexOf(page),labels[n]).setIcon(NavigationIcons.icon(this,n)).setCheckable(true);}
            if(!handlingNavigationTap)syncNavigationSelection();
        }finally{selectingTab=false;}
    }
    private void syncNavigationSelection(){
        if(navigationBar==null||handlingNavigationTap)return;
        int id=HomeNavigation.itemId(tab);if(navigationBar.getMenu().findItem(id)==null&&(tab.equals("note")||tab.equals("inbox"))&&navigationBar.getMenu().findItem(106)!=null)id=106;
        if(navigationBar.getMenu().findItem(id)!=null&&navigationBar.getSelectedItemId()!=id){
            boolean previous=selectingTab;selectingTab=true;
            try{navigationBar.setSelectedItemId(id);}finally{selectingTab=previous;}
        }
    }

    private void scaffold(String name,Runnable back,boolean navigation){ensureScaffold();int index=java.util.Arrays.asList(HomeNavigation.TABS).indexOf(tab);if(navigation&&index>=0&&!homeTabVisible(index)){navigation=false;if(back==null)back=()->{tab="agenda";if(!homeTabVisible(0))tab="settings";home();};}if(scheduleScroll!=null){scheduleScrollY=scheduleScroll.getScrollY();scheduleScroll=null;}screenGeneration++;screenName=name;backAction=back==null&&navigation&&moreFeaturesTab!=null?this::home:back;rootPage=navigation;content.removeAllViews();navigationBar.setVisibility(navigation?View.VISIBLE:View.GONE);agentRow.setVisibility(navigation&&!UiStyle.neo(this)?View.VISIBLE:View.GONE);if(navigation)syncNavigationSelection();ViewCompat.requestApplyInsets(shell);content.animate().cancel();if(android.animation.ValueAnimator.areAnimatorsEnabled()&&!prefs.getBoolean("reduce_motion",false)){content.setAlpha(.48f);content.setTranslationX(Ui.dp(this,transitionDirection==0?0:30*transitionDirection));content.setTranslationY(Ui.dp(this,transitionDirection==0?10:0));content.setScaleX(.978f);content.setScaleY(.978f);content.animate().setStartDelay(0).alpha(1).translationX(0).translationY(0).scaleX(1).scaleY(1).setInterpolator(new android.view.animation.PathInterpolator(.16f,1f,.3f,1f)).setDuration(320).start();}else{content.setAlpha(1);content.setTranslationX(0);content.setTranslationY(0);content.setScaleX(1);content.setScaleY(1);}transitionDirection=0;}
    public LinearLayout fullPage(String title,Runnable back){scaffold(title,back,false);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout form=Ui.padded(this);scroll.addView(form);content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));return form;}
    public LinearLayout editorPage(String title,Runnable back){scaffold(title,back,false);LinearLayout page=Ui.column(this);content.addView(page,new FrameLayout.LayoutParams(-1,-1));return page;}
    public void exportMarkdown(JSONObject record){try{record=Data.get(this).expanded(record);java.nio.file.Files.write(new File(getCacheDir(),"note-export.md").toPath(),MarkdownFiles.markdown(record).getBytes(java.nio.charset.StandardCharsets.UTF_8));startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/markdown").putExtra(Intent.EXTRA_TITLE,MarkdownFiles.filename(record)),107);}catch(Exception e){message(e.getMessage());}}
    public void home(){
        if(moreFeaturesTab!=null){
            editorState=null;
            noteState=null;
            CommandScreen.show(this,moreFeaturesQuery,moreFeaturesScroll);
        }else renderHome();
    }
    private void renderHome(){if(navigationBar!=null)refreshNavigation();editorState=null;noteState=null;if(UiStyle.neo(this)&&tab.equals("agenda")){NeoHome.show(this,weekStart);return;}if(UiStyle.timetable(this)&&tab.equals("agenda")){TimetableHome.show(this,selected);return;}String name=tab.equals("week")?"周预览":tab.equals("agenda")?"今天":tab.equals("note")?"笔记":tab.equals("inbox")?"收集箱":tab.equals("course")?"日程":"设置";scaffold(name,null,true);try{LinearLayout hot=Ui.column(this);if(HotUpdates.render(this,tab,hot)){homeForm().addView(hot);return;}if(tab.equals("agenda")||tab.equals("course")||tab.equals("week"))schedule();else if(tab.equals("settings"))new SettingsPanels(this).root(homeForm());else recordList();}catch(Exception e){message("读取失败："+e.getMessage());}}
    private void schedule()throws Exception{
        LinearLayout page=tab.equals("week")?fixedWeekForm():homeForm();
        if(tab.equals("course")){
            ScheduleScreen.show(this,page,selected);
            ScrollView scroll=(ScrollView)page.getParent();scheduleScroll=scroll;
            int position=scheduleScrollY;
            if(position>0)scroll.post(()->{if(scheduleScroll==scroll)scroll.scrollTo(0,position);});
            return;
        }
        if(tab.equals("week")){if(UiStyle.neo(this))page.setPadding(page.getPaddingLeft(),page.getPaddingTop(),page.getPaddingRight(),Ui.dp(this,64));WeekPreview.show(this,page,weekStart,date->{weekStart=WeekOptions.first(this,date);transitionDirection=1;renderHome();},this::renderHome);if(UiStyle.neo(this))NeoHome.attachAI(this,page);return;}
        LocalDate today=LocalDate.now();LinearLayout hero=Ui.padded(this);
        TextView date=Ui.text(this,today.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA)),27,true);
        hero.addView(date);
        String weather=OpenWeatherSources.cached(this,"");
        hero.addView(Ui.help(this,ChineseDates.description(today)+(weather.isEmpty()?"":" · "+weather)));
        hero.addView(new DayTimeline(this),new LinearLayout.LayoutParams(-1,-2));
        page.addView(hero);
        LinearLayout actions=Ui.row(this);page.addView(actions);Ui.cell(this,actions,Ui.button(this,"＋ 新建待办",true,()->newRecord("task")));Ui.cell(this,actions,Ui.button(this,"周预览",false,()->showWeek(today)));
        if(!prefs.getBoolean("reduce_motion",false)){Ui.enter(actions,1);}DailyTasks.today(this,page);
    }
    void changeUiStyle(String style){if(!app.planaloud.ui.DisplayTrial.begin(this,prefs.getInt("ui_scale",100),prefs.getInt("font_scale",100),style)){message("界面风格未改变");return;}tab="agenda";weekPreview=false;moreFeaturesTab=null;screenName="";recreate();}
    void neoViewChanged(){home();}
    void weekOptionsChanged(LocalDate anchor){weekStart=WeekOptions.first(this,anchor);home();}
    void moveDisplayedWeek(LocalDate date){weekStart=WeekOptions.first(this,date);home();}
    void showWeek(LocalDate start){weekStart=WeekOptions.first(this,start);weekPreview=true;tab="week";transitionDirection=1;renderHome();}
    void openPage(String name){tab=name;weekPreview=false;renderHome();}
    void exportAllNotes(){startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip").putExtra(Intent.EXTRA_TITLE,"DayFlow-notes-"+LocalDate.now()+".zip"),109);}
    public void tasks(){taskBoard(0);}
    private void taskBoard(int mode){try{LinearLayout form=fullPage("待办事项",this::home);ChipGroup filter=new ChipGroup(this);filter.setSingleSelection(true);filter.setSelectionRequired(true);String[] names={"待完成","时间待定","已完成"};for(int n=0;n<3;n++){int index=n;Chip chip=new Chip(this);chip.setText(names[n]);chip.setCheckable(true);chip.setChecked(n==mode);chip.setOnClickListener(v->taskBoard(index));filter.addView(chip);}form.addView(filter);form.addView(Ui.button(this,"＋ 新建待办",true,()->newRecord("task")));JSONArray rows=Data.get(this).taskBoard(mode);for(int n=0;n<rows.length();n++){JSONObject r=rows.getJSONObject(n);MaterialCardView card=Ui.card(this);Ui.priority(card,r);LinearLayout row=Ui.row(this);com.google.android.material.checkbox.MaterialCheckBox check=new com.google.android.material.checkbox.MaterialCheckBox(this);check.setChecked(!r.optString("completed").isEmpty());check.setContentDescription("完成或恢复「"+r.optString("title")+"」");check.setOnClickListener(v->{try{TaskActions.complete(this,r);changed();taskBoard(mode);}catch(Exception e){check.setChecked(!r.optString("completed").isEmpty());message(e.getMessage());}});row.addView(check);LinearLayout text=recordContents(r);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));text.setOnClickListener(v->detail(r));text.setOnLongClickListener(v->{TaskQuickActions.menu(this,text,r,()->taskBoard(mode));return true;});card.addView(row);form.addView(card);}if(rows.length()==0)form.addView(Ui.help(this,"这里还没有事项。未确定日期的事情也可以先记下。"));}catch(Exception e){message(e.getMessage());}}
    public void newRecord(String kind){if(kind.equals("inbox")){captureText();return;}try{edit(Data.make(kind,""));}catch(Exception e){message(e.getMessage());}}
    private void captureText(){LinearLayout form=fullPage("粘贴通知",this::home);form.addView(Ui.help(this,"粘贴微信、QQ 或邮件正文。先放入收集箱，再确认日程或提取多个事项。"));TextInputEditText title=Ui.field(this,form,"标题","新通知",false),body=Ui.field(this,form,"通知正文","",true);body.setMinLines(8);Runnable save=()->{try{if(body.getText().toString().trim().isEmpty()){body.setError("请粘贴通知内容");return;}JSONObject r=Data.make("inbox",title.getText().toString().trim().isEmpty()?"新通知":title.getText().toString().trim()).put("content",body.getText().toString()).put("source","粘贴通知");Data.validate(r);Data.get(this).save(r);changed();detail(r);}catch(Exception e){message(e.getMessage());}};form.addView(Ui.button(this,"存入收集箱",true,save));}
    private void recordList()throws Exception{
        boolean notes=tab.equals("note");LinearLayout page=Ui.column(this);page.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);content.addView(page);LinearLayout actions=Ui.row(this);actions.addView(Ui.button(this,notes?"＋ 文字笔记":"＋ 粘贴通知",true,()->newRecord(tab)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(Ui.button(this,notes?"图片":"截图",false,()->pickFile(notes?105:106,"image/*")),new LinearLayout.LayoutParams(Ui.dp(this,90),-2));page.addView(actions);
        if(!notes){LinearLayout collect=Ui.row(this);Ui.cell(this,collect,Ui.button(this,"交给 AI 整理",false,()->InboxAgent.quick(this)));Ui.cell(this,collect,Ui.button(this,"指令",false,()->CommandScreen.show(this)));page.addView(collect);
            LinearLayout review=Ui.row(this);page.addView(review);
            Ui.cell(this,review,Ui.iconButton(this,"待批准",android.R.drawable.ic_menu_agenda,false,()->InboxReview.pending(this)));
            Ui.cell(this,review,Ui.iconButton(this,"回收站",android.R.drawable.ic_menu_delete,false,()->InboxReview.trash(this,0)));}
        final JSONArray[] rows={notes?Data.get(this).notes(""):Data.get(this).list(tab,200)};TextView status=Ui.help(this,notes?"支持 Markdown · 置顶笔记优先 · 文字与附图一起同步":"");
        TextInputEditText search=notes?Ui.field(this,page,"搜索笔记标题或正文","",false):null;page.addView(status);
        RecyclerView list=new RecyclerView(this);list.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this,Ui.columns(this)));RecyclerView.Adapter<RecordHolder> adapter=new RecyclerView.Adapter<RecordHolder>(){public RecordHolder onCreateViewHolder(ViewGroup p,int type){MaterialCardView card=Ui.card(MainActivity.this);RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(Ui.dp(MainActivity.this,4),Ui.dp(MainActivity.this,5),Ui.dp(MainActivity.this,4),Ui.dp(MainActivity.this,5));card.setLayoutParams(lp);return new RecordHolder(card);}public void onBindViewHolder(RecordHolder h,int position){try{JSONObject r=rows[0].getJSONObject(position);h.card.removeAllViews();Ui.priority(h.card,r);LinearLayout body=recordContents(r);if(r.optBoolean("pinned"))body.addView(Ui.help(MainActivity.this,"已置顶"));h.card.addView(body);h.card.setOnClickListener(v->{if(notes||!openSource(r))detail(r);});h.card.setOnLongClickListener(v->{recordMenu(h.card,r);return true;});}catch(Exception ignored){}}public int getItemCount(){return rows[0].length();}};list.setAdapter(adapter);page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        if(search!=null)search.addTextChangedListener(new android.text.TextWatcher(){final Handler handler=new Handler(Looper.getMainLooper());Runnable pending;public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence value,int start,int before,int count){if(pending!=null)handler.removeCallbacks(pending);String query=value.toString();pending=()->{if(isDestroyed()||!search.isAttachedToWindow())return;try{rows[0]=Data.get(MainActivity.this).notes(query);adapter.notifyDataSetChanged();status.setText(rows[0].length()==0?"没有找到笔记":"显示 "+rows[0].length()+" 条 · 置顶优先 · 正文与附图一起同步");}catch(Exception e){message(e.getMessage());}};handler.postDelayed(pending,200);}public void afterTextChanged(android.text.Editable value){}});
    }
    private static final class RecordHolder extends RecyclerView.ViewHolder{final MaterialCardView card;RecordHolder(MaterialCardView c){super(c);card=c;}}
    public LinearLayout recordContents(JSONObject r){LinearLayout l=Ui.padded(this);TextView title=Ui.text(this,r.optString("title"),16,true);title.setMaxLines(2);l.addView(title);if(!r.optString("type_id").isEmpty()){TextView badge=TaskTypes.badge(this,r.optString("type_id"));if(badge!=null)l.addView(badge);}String detail=r.optString("kind").equals("note")?r.optString("source","笔记"):PlanTime.label(r);String countdown=PlanTime.countdown(r,LocalDateTime.now());if(!countdown.isEmpty())detail=countdown+(r.optString("start_at").isEmpty()?"":" · "+detail);if(detail.isEmpty())detail=r.optString("time_hint");if(detail.isEmpty())detail=r.optString("source",r.optString("kind").equals("note")?"笔记":"时间待定");l.addView(Ui.help(this,detail));String planning=PlanTime.planning(r);if(!planning.isEmpty())l.addView(Ui.help(this,planning));if(!r.optString("content").isEmpty()){TextView body=Ui.text(this,r.optString("content"),14,false);if("note".equals(r.optString("kind")))NoteTools.renderSnippet(body,r.optString("content"));body.setMaxLines(3);body.setEllipsize(android.text.TextUtils.TruncateAt.END);l.addView(body);}if(r.optString("kind").equals("inbox"))l.addView(Ui.help(this,"待确认"+(r.optString("source_group").isEmpty()?"":" · "+r.optString("source_group"))));if(!r.optString("image_base64").isEmpty())l.addView(Ui.help(this,"附有图片"));return l;}
    private boolean openSource(JSONObject r){
        String pkg=r.optString("source");
        if(!"inbox".equals(r.optString("kind"))||!pkg.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+"))return false;
        try{
            Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);
            if(intent!=null){startActivity(intent);return true;}
        }catch(android.content.ActivityNotFoundException|SecurityException ignored){}
        return false;
    }
    private void recordMenu(View anchor,JSONObject r){
        PopupMenu menu=new PopupMenu(this,anchor);
        boolean note="note".equals(r.optString("kind"));
        menu.getMenu().add(note?"预览":"查看通知").setOnMenuItemClickListener(item->{detail(r);return true;});
        if(note){
            menu.getMenu().add("编辑").setOnMenuItemClickListener(item->{edit(r);return true;});
            menu.getMenu().add(r.optBoolean("pinned")?"取消置顶":"置顶").setOnMenuItemClickListener(item->{pinNote(r,updated->home());return true;});
            menu.getMenu().add("导出 .md").setOnMenuItemClickListener(item->{exportMarkdown(r);return true;});
        }
        menu.getMenu().add("删除").setOnMenuItemClickListener(item->{confirmDelete(r,this::home);return true;});
        menu.show();
    }
    private void pinNote(JSONObject r,java.util.function.Consumer<JSONObject> after){
        try{JSONObject note=Data.get(this).expanded(r);note.put("pinned",!note.optBoolean("pinned"));Data.get(this).save(note);changed();after.accept(note);}
        catch(Exception e){message(e.getMessage());}
    }
    void confirmDelete(JSONObject r,Runnable after){
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("删除「"+r.optString("title")+"」？")
            .setMessage(Recurrences.series(r)?"将删除整个重复系列。":r.optString("series_id").isEmpty()?"":"仅删除本次安排。")
            .setNegativeButton("取消",null).setPositiveButton("删除",(dialog,which)->{
                try{
                    Data db=Data.get(this);
                    JSONObject current=db.resolve(r.getString("id"),r.optString("series_id"),r.optString("occurrence_date"));
                    if(current==null||current.optBoolean("deleted")){after.run();return;}
                    JSONObject deleted=new JSONObject(current.toString()).put("deleted",true);
                    db.save(deleted);changed();after.run();
                    Snackbar.make(shell,"已删除",Snackbar.LENGTH_LONG).setAction("撤销",v->{
                        try{UndoHistory.restoreRecord(this,deleted.getString("id"),deleted.getString("revision"));changed();after.run();}
                        catch(Exception e){message(e.getMessage());}
                    }).show();
                }catch(Exception e){message(e.getMessage());}
            }).show();
    }
    private void notePreview(JSONObject r){notePreview(r,null,false);}
    void notePreview(JSONObject r,Uri external,boolean writable){
        noteState=null;editorState=null;
        LinearLayout form=fullPage("笔记预览",this::home);
        form.addView(Ui.text(this,r.optString("title"),26,true));
        addRecordImage(form,r);
        TextView body=Ui.text(this,"",prefs.getInt("note_font_size",17),false);
        body.setTextIsSelectable(true);
        if(DocumentPlugins.available(this,DocumentPlugins.MD))
            app.planaloud.markdown.NoteTools.render(body,r.optString("content"),false);
        else NoteTools.render(body,r.optString("content"));
        form.addView(body);
        LinearLayout actions=Ui.row(this);form.addView(actions);
        Ui.cell(this,actions,Ui.iconButton(this,"编辑",android.R.drawable.ic_menu_edit,true,()->{edit(r);if(external!=null&&noteState!=null)noteState.bindExternal(external,writable);}));
        Ui.cell(this,actions,Ui.iconButton(this,"删除",android.R.drawable.ic_menu_delete,false,()->confirmDelete(r,this::home)));
        LinearLayout options=Ui.row(this);form.addView(options);
        Ui.cell(this,options,Ui.iconButton(this,"导出 .md",android.R.drawable.ic_menu_save,false,()->exportMarkdown(r)));
        Ui.cell(this,options,Ui.iconButton(this,r.optBoolean("pinned")?"取消置顶":"置顶",android.R.drawable.ic_menu_set_as,false,()->pinNote(r,updated->notePreview(updated,external,writable))));
        LinearLayout convert=Ui.row(this);form.addView(convert);
        Ui.cell(this,convert,Ui.iconButton(this,"转为待办",android.R.drawable.ic_menu_agenda,false,()->{
            try{edit(new JSONObject(r.toString()).put("kind","task"));}catch(Exception e){message(e.getMessage());}
        }));
        Ui.cell(this,convert,new View(this));
        NoteTasks.entry(this,form,r);
    }
    private void addRecordImage(LinearLayout form,JSONObject r){
        if(r.optString("image_base64").isEmpty())return;
        byte[] bytes=android.util.Base64.decode(r.optString("image_base64"),0);
        ImageView image=new ImageView(this);image.setAdjustViewBounds(true);
        image.setImageBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length));
        form.addView(image,new LinearLayout.LayoutParams(-1,-2));
    }
    private void addRecord(LinearLayout parent,JSONObject r){MaterialCardView card=Ui.card(this);Ui.priority(card,r);card.addView(recordContents(r));card.setOnClickListener(v->detail(r));parent.addView(card);}
    public void editBasic(JSONObject r){editorState=null;noteState=new NoteEditor(this,r,false);}
    public void edit(JSONObject r){if(r.optBoolean("_dayflow_preview")){try{edit(Data.get(this).expanded(r));}catch(Exception e){message(e.getMessage());}return;}if("note".equals(r.optString("kind"))){editorState=null;noteState=new NoteEditor(this,r);}else{noteState=null;editorState=new EditorPanel(this,r);}}
    public void detail(JSONObject r){if(UiStyle.neo(this)){WeekActions.open(this,r);return;}if(r.optBoolean("_dayflow_preview")){try{detail(Data.get(this).expanded(r));}catch(Exception e){message(e.getMessage());}return;}editorState=null;noteState=null;if("note".equals(r.optString("kind"))){notePreview(r);return;}LinearLayout form=fullPage(r.optString("kind").equals("inbox")?"待确认通知":"事项详情",this::home);form.addView(Ui.text(this,r.optString("title"),26,true));form.addView(Ui.help(this,PlanTime.label(r)+" · "+TaskTypes.label(this,r.optString("type_id"))+(PlanTime.cancelled(r)?" · 已取消":"")));String countdown=PlanTime.countdown(r,LocalDateTime.now());if(!countdown.isEmpty())form.addView(Ui.help(this,countdown));String planning=PlanTime.planning(r);if(!planning.isEmpty())form.addView(Ui.help(this,planning));if(Recurrences.series(r))form.addView(Ui.help(this,"这是重复系列，编辑会影响尚未单独修改的各次安排。"));String date=r.optString("start_at");form.addView(Ui.help(this,date.isEmpty()?r.optString("time_hint","时间待定"):date.replace("T"," ")+(!r.optString("end_at").isEmpty()?" → "+r.optString("end_at").replace("T"," "):"")));if(!r.optString("location").isEmpty())form.addView(Ui.button(this,r.optString("location")+" · 高德地图",false,()->MapLinks.open(this,r.optString("location"))));
        addRecordImage(form,r);
        TextView body=Ui.text(this,r.optString("content"),16,false);
        body.setTextIsSelectable(true);body.setPadding(0,Ui.dp(this,16),0,Ui.dp(this,16));form.addView(body);
        boolean inbox="inbox".equals(r.optString("kind"));
        LinearLayout actions=Ui.row(this);form.addView(actions);
        Ui.cell(this,actions,Ui.iconButton(this,inbox?"确认并安排":"编辑",android.R.drawable.ic_menu_edit,true,()->edit(r)));
        Ui.cell(this,actions,Ui.iconButton(this,"删除",android.R.drawable.ic_menu_delete,false,()->confirmDelete(r,this::home)));
        LinearLayout more=Ui.row(this);form.addView(more);
        if(inbox){
            Ui.cell(this,more,Ui.iconButton(this,"AI 整理",android.R.drawable.ic_menu_manage,false,()->aiScreen(r)));
            Ui.cell(this,more,Ui.iconButton(this,"打开来源",android.R.drawable.ic_menu_view,false,()->{if(!openSource(r))message("未找到可打开的来源应用");}));
        }else{
            Ui.cell(this,more,Ui.iconButton(this,r.optString("completed").isEmpty()?"标记完成":"恢复待办",android.R.drawable.checkbox_on_background,false,()->{try{TaskActions.complete(this,r);changed();home();message("已更新");}catch(Exception e){message(e.getMessage());}}));
            Ui.cell(this,more,Ui.iconButton(this,"后续任务",android.R.drawable.ic_input_add,false,()->followup(r)));
        }
        if(r.optString("kind").equals("project"))CheckIns.panel(this,form,r);
        FocusTimer.entry(this,form,r);NoteTasks.entry(this,form,r);SmsCapture.details(this,form,r);PlanHub.relations(this,form,r);
        if(!date.isEmpty()){
            LinearLayout system=Ui.row(this);form.addView(system);
            Ui.cell(this,system,Ui.iconButton(this,"系统日历",R.drawable.ic_calendar,false,()->calendar(r)));
            Ui.cell(this,system,Ui.iconButton(this,"系统闹钟",android.R.drawable.ic_lock_idle_alarm,false,()->alarm(r)));
            form.addView(Ui.help(this,"系统时钟仅接收时分，请自行检查闹钟日期与重复设置。"));
        }
    }
    private void followup(JSONObject r){LinearLayout form=fullPage("添加后续任务",()->detail(r));form.addView(Ui.help(this,"实际完成「"+r.optString("title")+"」后，再开始下一阶段。"));TextInputEditText title=Ui.field(this,form,"标题","",false),days=Ui.field(this,form,"延迟天数（0为当天）","0",false);days.setInputType(2);form.addView(Ui.button(this,"添加后续任务",true,()->{try{int offset=Integer.parseInt(days.getText().toString());if(title.getText().toString().trim().isEmpty()||offset<0||offset>3650)throw new Exception("请填写标题，天数需为0–3650");if(!r.optString("completed").isEmpty())throw new Exception("请为尚未完成的事项添加后续任务");Data.get(this).save(Data.make("task",title.getText().toString().trim()).put("trigger_after",r.getString("id")).put("offset_days",offset).put("activation","waiting").put("time_hint","完成「"+r.optString("title")+"」后 "+offset+" 天"));changed();tasks();}catch(Exception e){message(e.getMessage());}}));}
    public void aiScreen(JSONObject r){AssistantPlugin.review(this,r,"总结这份通知并提取独立日程，先放入待确认收集箱，最后提醒我逐项核验。");}
    private void calendar(JSONObject r){try{long start=LocalDateTime.parse(r.getString("start_at")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),end=r.optString("end_at").isEmpty()?start+3600000:LocalDateTime.parse(r.getString("end_at")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();startActivity(new Intent(Intent.ACTION_INSERT,CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE,r.optString("title")).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,start).putExtra(CalendarContract.EXTRA_EVENT_END_TIME,end).putExtra(CalendarContract.Events.EVENT_LOCATION,r.optString("location")).putExtra(CalendarContract.Events.DESCRIPTION,r.optString("content")));}catch(Exception e){message("未能打开系统日历："+e.getMessage());}}
    private void alarm(JSONObject r){try{LocalDateTime date=LocalDateTime.parse(r.getString("start_at"));startActivity(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,date.getHour()).putExtra(AlarmClock.EXTRA_MINUTES,date.getMinute()).putExtra(AlarmClock.EXTRA_MESSAGE,r.optString("title")).putExtra(AlarmClock.EXTRA_SKIP_UI,false));}catch(Exception e){message("未找到兼容的系统时钟");}}
    private boolean pendingExactAlarm;
    public void notificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},200);}
    public void reminderPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){pendingExactAlarm=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},200);}else exactPermission();}
    public void exactPermission(){if(Build.VERSION.SDK_INT>=31&&!AlarmSound.exact(this))try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){message("请在系统设置中允许闹钟与提醒，否则提醒可能延迟");}}
    private LinearLayout fixedWeekForm(){LinearLayout page=Ui.column(this);page.setPadding(Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8),0);page.setTag("classic-week-page");content.addView(page,new FrameLayout.LayoutParams(-1,-1));return page;}
    public LinearLayout homeForm(){ScrollView scroll=new ScrollView(this);LinearLayout form=Ui.padded(this);scroll.addView(form);content.addView(scroll);if(UiStyle.neo(this))NeoHome.returnBar(this,form);return form;}
    public void checkUpdates(){if(busy)return;busy=true;worker.execute(()->{try{JSONObject info=Updates.check(this);runOnUiThread(()->{busy=false;if(isDestroyed())return;if(info==null)message("当前已是最新版本");else new SettingsPanels(this).showUpdate(info);});}catch(Exception e){runOnUiThread(()->{busy=false;message(e.getMessage());});}});}
    public void pickFile(int code,String mime){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime),code);}
    public void refreshBackdrop(){backdrop.load();}
    public void refreshWeatherUi(){if(editorState!=null||noteState!=null)return;if("位置与天气".equals(screenName))new SettingsPanels(this).weather();else if(backAction==null&&"agenda".equals(tab))home();}
    public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==201){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)locationWeather.request();else{prefs.edit().putBoolean("auto_location",false).apply();message("未授权位置，可继续使用手动位置与离线功能");}}else if(request==202){message(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED?"已授权新短信识别":"未授权直接接收短信，可使用通知访问或手动粘贴");}else if(request==200){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){if(pendingExactAlarm){pendingExactAlarm=false;exactPermission();}}else{pendingExactAlarm=false;message("通知权限未开启，事项仍已保存");}}}
    protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(code==AssistantPlugin.REQUEST){boolean timetable=prefs.getBoolean("assistant_timetable_staged",false);prefs.edit().remove("assistant_timetable_staged").apply();AssistantPlugin.close(this);changed();if(data!=null&&data.hasExtra("dayflow_open_command_ticket"))CommandScreen.launch(this,data.getStringExtra("dayflow_open_command_ticket"));else if(data!=null&&data.hasExtra("dayflow_open_command_page"))CommandScreen.open(this,data.getStringExtra("dayflow_open_command_page"));else if(timetable)new TimetableCapture(this,null);else if(data!=null&&data.getBooleanExtra("dayflow_open_inbox",false))showInbox();else if(data!=null&&data.getBooleanExtra("dayflow_open_schedule",false)){LocalDate day=LocalDate.now();try{day=LocalDate.parse(data.getStringExtra("dayflow_open_date"));}catch(Exception ignored){}openSchedule(day);}else home();if(prefs.getBoolean("assistant_needs_notification",false)){boolean exact=prefs.getBoolean("assistant_needs_exact_alarm",false);prefs.edit().remove("assistant_needs_notification").remove("assistant_needs_exact_alarm").apply();if(exact)reminderPermission();else notificationPermission();}return;}if(code==DocumentPlugins.REQUEST){DocumentPlugins.result(this,result,data);return;}if(code==124){WeekPng.result(this,result,data);return;}if(code==114){if(result==RESULT_OK&&data!=null)ExtensionHub.ocrResult(this,data);else ExtensionHub.cleanupOcr(this);return;}if(code==CardBackgrounds.PICK){if(result==RESULT_OK&&data!=null&&data.getData()!=null)CardBackgrounds.imported(this,data.getData());else CardBackgrounds.cancelImport(this);return;}if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(code==123)job(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("无法打开目标文件");out.write(Diagnostics.export(this).getBytes(java.nio.charset.StandardCharsets.UTF_8));}return "已导出诊断日志";},null);
        else if(code==101)job(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){Data.get(this).writeExport(out);}return "已导出同步文件";},null);
        else if(code==102)job(()->{try(InputStream in=getContentResolver().openInputStream(uri)){return Data.get(this).importStream(in);}},this::home);
        else if(code==103){if(busy)return;busy=true;worker.execute(()->{try{JSONArray items;try(InputStream in=getContentResolver().openInputStream(uri)){items=Imports.timetable(Network.read(in,4*1024*1024));}runOnUiThread(()->{busy=false;if(!isDestroyed())importPreview(items);});}catch(Exception e){runOnUiThread(()->{busy=false;message(e.getMessage());});}});}
        else if(code==110)new TimetableCapture(this,uri);
        else if(code==112)ImageTools.crop(this,uri,cropped->ExtensionHub.runOcr(this,cropped));
        else if(code==107)job(()->{try(InputStream input=new FileInputStream(new File(getCacheDir(),"note-export.md"));OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("无法打开目标文件");byte[] buffer=new byte[8192];int length;while((length=input.read(buffer))!=-1)output.write(buffer,0,length);}return "已导出 .md 文件";},null);
        else if(code==108)importMarkdown(uri,false,data.getFlags());
        else if(code==109)job(()->{try(OutputStream output=getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("无法打开目标文件");return "已导出 "+Data.get(this).exportMarkdownZip(output)+" 篇 .md 笔记";}},null);
        else if(code==121)job(()->{try(InputStream in=getContentResolver().openInputStream(uri)){WallpaperRules.save(this,new JSONObject(Network.read(in,256*1024)));return "已导入壁纸规则";}},()->{refreshBackdrop();WallpaperScreen.show(this);});
        else if(code==122)job(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("无法打开目标文件");out.write(WallpaperRules.read(this).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));return "已导出壁纸规则";}},null);
        else if(code==119||code==120)ImageTools.crop(this,uri,"裁切壁纸",cropped->job(()->{
            if(code==119){WallpaperImages.importRule(this,cropped);return "已加入壁纸图库";}
            WallpaperImages.importWidget(this,cropped);return "小组件背景已更新";
        },()->{if(code==119){refreshBackdrop();WallpaperScreen.show(this);}else new SettingsPanels(this).widget(this::home);}));
        else if(code==104)ImageTools.crop(this,uri,"裁切壁纸",cropped->job(()->saveImage(cropped,code),this::home));
        else if(code==105||code==106)job(()->saveImage(uri,code),this::home);
    }
    private void importMarkdown(Uri uri,boolean external,int flags){if(noteState!=null&&!noteState.persistForNavigation())return;final JSONObject[] imported={null};boolean writable=external&&(flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)!=0;if((flags&Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)!=0)try{getContentResolver().takePersistableUriPermission(uri,flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(SecurityException ignored){}
        job(()->{if(external)imported[0]=MarkdownDocuments.open(this,uri);else{String name=MarkdownDocuments.name(this,uri);if(!name.toLowerCase(Locale.ROOT).endsWith(".md"))throw new IOException("请选择 .md 文件");String text;try(InputStream input=getContentResolver().openInputStream(uri)){text=MarkdownFiles.read(input);}if(text.startsWith("\uFEFF"))text=text.substring(1);JSONObject note=Data.make("note",name.substring(0,name.length()-3)).put("content",text).put("content_format","markdown");Data.validate(note);Data.get(this).save(note);imported[0]=note;}return external?"已打开 Markdown 文件":"已导入 Markdown 笔记";},()->{if(imported[0]!=null){edit(imported[0]);if(external&&noteState!=null)noteState.bindExternal(uri,writable);}});
    }
    private String saveImage(Uri uri,int code)throws Exception{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,bounds);}if(bounds.outWidth<=0)throw new Exception("无法读取图片");BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/opts.inSampleSize>1400)opts.inSampleSize*=2;Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,opts);}if(bitmap==null)throw new Exception("无法读取图片");if(code==104){try(OutputStream out=openFileOutput("wallpaper.jpg",0)){bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);ThemeColors.extract(this,bitmap);}finally{bitmap.recycle();}runOnUiThread(()->{AgendaWidget.refresh(this);recreate();});return "壁纸与配色已更新";}
        ByteArrayOutputStream image=new ByteArrayOutputStream();try{bitmap.compress(Bitmap.CompressFormat.JPEG,75,image);while(image.size()>36000){Bitmap next=Bitmap.createScaledBitmap(bitmap,Math.max(1,bitmap.getWidth()*3/4),Math.max(1,bitmap.getHeight()*3/4),true);bitmap.recycle();bitmap=next;image.reset();bitmap.compress(Bitmap.CompressFormat.JPEG,70,image);}}finally{bitmap.recycle();}JSONObject record=Data.make(code==105?"note":"inbox",code==105?"图片笔记":"截图 · 待识别").put("image_base64",android.util.Base64.encodeToString(image.toByteArray(),2));Data.validate(record);Data.get(this).save(record);return "已保存压缩图片";
    }
    public void importPreview(JSONArray items){LinearLayout form=fullPage("导入预览 · "+items.length()+" 条",this::home);form.addView(Ui.help(this,"检查课程、日期、单双周与教室。已有事项不会重复导入。"));form.addView(Ui.button(this,"确认导入",true,()->job(()->{int count=0;for(int i=0;i<items.length();i++){JSONObject r=items.getJSONObject(i);if(Data.get(this).find(r.getString("id"))==null){Data.get(this).save(r);count++;}}return "已导入 "+count+" 条";},this::home)));for(int i=0;i<Math.min(20,items.length());i++)try{form.addView(recordContents(items.getJSONObject(i)));}catch(Exception ignored){}if(items.length()>20)form.addView(Ui.help(this,"另有 "+(items.length()-20)+" 条，导入后可在周课表查看。"));}
    public void setScheduleDate(LocalDate date){selected=date;}
    public void openSchedule(LocalDate date){if(!date.equals(selected)){scheduleScroll=null;scheduleScrollY=0;}selected=date;tab="course";weekPreview=false;renderHome();}
    public void showInbox(){tab="inbox";weekPreview=false;renderHome();}
    public void showCaptureText(String text){try{JSONObject r=Data.make("inbox","识别文字 · 待确认").put("content",text).put("confirmation_status","pending");Data.validate(r);Data.get(this).save(r);changed();detail(r);}catch(Exception e){message(e.getMessage());}}
    private boolean interactive(View view,float x,float y){android.graphics.Rect bounds=new android.graphics.Rect();if(!view.getGlobalVisibleRect(bounds)||!bounds.contains((int)x,(int)y))return false;if(view instanceof DayTimeline||view instanceof TwoAxisScroll||view.canScrollHorizontally(-1)||view.canScrollHorizontally(1)||view instanceof com.google.android.material.slider.Slider||view instanceof com.google.android.material.slider.RangeSlider||view instanceof EditText||view instanceof SeekBar||view instanceof NumberPicker||view instanceof HorizontalScrollView||view.getClass().getName().contains("CropImageView"))return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=group.getChildCount()-1;i>=0;i--)if(interactive(group.getChildAt(i),x,y))return true;}return false;}
    private String swipeDestination(boolean forward){return HomeNavigation.adjacent(visibleNavigationMask(),tab,forward);}
    private void swipePage(boolean forward){
        String next=swipeDestination(forward);if(next==null)return;
        weekPreview=next.equals("week");if(weekPreview)weekStart=WeekOptions.first(this,LocalDate.now());
        tab=next;transitionDirection=forward?1:-1;renderHome();
    }
    private boolean swipeStartAllowed(MotionEvent event){
        if(UiStyle.neo(this)||!rootPage||onboarding!=null||editorState!=null||noteState!=null||content==null||prefs==null||!prefs.getBoolean("swipe",true))return false;
        Rect bounds=new Rect();if(!content.getGlobalVisibleRect(bounds)||!bounds.contains((int)event.getRawX(),(int)event.getRawY()))return false;
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(shell);
        androidx.core.graphics.Insets gestures=insets==null?androidx.core.graphics.Insets.NONE:insets.getInsets(WindowInsetsCompat.Type.systemGestures());
        Rect window=new Rect();shell.getGlobalVisibleRect(window);
        int left=Math.max(Ui.dp(this,24),gestures.left),right=Math.max(Ui.dp(this,24),gestures.right);
        return event.getRawX()>window.left+left&&event.getRawX()<window.right-right&&!interactive(content,event.getRawX(),event.getRawY());
    }
    public boolean dispatchTouchEvent(MotionEvent event){
        if(pageSwipe==null)pageSwipe=new PageSwipe(getResources().getDisplayMetrics().density,ViewConfiguration.get(this).getScaledTouchSlop());
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                pageSwipe.begin(event.getRawX(),event.getRawY(),swipeStartAllowed(event),swipeDestination(true)!=null,swipeDestination(false)!=null);break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pageSwipe.move(event.getRawX(),event.getRawY(),event.getPointerCount());
                if(pageSwipe.captured()){content.setTranslationX(0);return true;}break;
            case MotionEvent.ACTION_POINTER_UP:
                if(pageSwipe.captured())return true;break;
            case MotionEvent.ACTION_MOVE:
                boolean captured=pageSwipe.captured();
                if(pageSwipe.move(event.getRawX(),event.getRawY(),event.getPointerCount())){
                    if(!captured){MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();content.animate().cancel();}
                    content.setTranslationX(Math.max(-Ui.dp(this,72),Math.min(Ui.dp(this,72),pageSwipe.delta(event.getRawX())*.25f)));return true;
                }break;
            case MotionEvent.ACTION_UP:
                boolean owned=pageSwipe.captured();int direction=pageSwipe.finish(event.getRawX(),event.getRawY());
                if(owned){content.setTranslationX(0);if(direction!=0)swipePage(direction>0);return true;}break;
            case MotionEvent.ACTION_CANCEL:
                boolean cancelled=pageSwipe.captured();pageSwipe.reset();if(content!=null)content.setTranslationX(0);if(cancelled)return true;break;
        }
        return super.dispatchTouchEvent(event);
    }
}
