package app.planaloud;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.IBinder;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import app.planaloud.assistant.ChatEngine;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public final class FloatingAssistant extends Service {
    static final String MASK_COLOR = "floating_mask_color";
    private static final String CONFIRM_CLOSE = "app.planaloud.floating.CONFIRM_CLOSE";
    private static final String CHANNEL = "floating_assistant";
    private static FloatingAssistant instance;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private WindowManager windows;
    private WindowManager.LayoutParams layout;
    private LinearLayout panel, messages, controls, attachment;
    private AlertDialog closeDialog;
    private ScrollView scroll;
    private EditText input;
    private ImageView preview;
    private Context theme;
    private int accent;
    private boolean expanded, right, capturing, busy;
    private volatile boolean closed;
    private File screenshot;
    private volatile String token;
    private final SharedPreferences.OnSharedPreferenceChangeListener appearanceListener=(prefs,key)->{
        if(key!=null&&java.util.Arrays.asList("theme","color_source","dynamic_color","custom_seed","wallpaper_seed",
                "launcher_appearance","ai_provider","api_url","model",MASK_COLOR).contains(key)){
            main.post(()->{if(!closed&&panel!=null)updatePalette();});
        }
    };
    private ChatEngine engine;
    private int pendingApprovals;

    static void hideHandle(){if(instance!=null)instance.panel.setVisibility(View.INVISIBLE);}
    static void restoreHandle(){
        if(instance!=null){
            instance.capturing=false;
            instance.panel.setVisibility(View.VISIBLE);
        }
    }
    static void acceptCapture(File file){
        if(instance==null||!file.isFile()){file.delete();return;}
        instance.clearAttachment();
        instance.screenshot=file;
        instance.showExpanded(true);
        instance.renderAttachment();
    }

    static void toggle(MainActivity activity) {
        Intent service = new Intent(activity, FloatingAssistant.class);
        if (instance != null) {
            instance.showExpanded(true);
            return;
        }
        if (!Settings.canDrawOverlays(activity)) {
            activity.message("请允许序言显示在其他应用上层，返回后再点悬浮助手");
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + activity.getPackageName())));
            } catch (ActivityNotFoundException ignored) {}
            return;
        }
        activity.startForegroundService(service);
    }

    @Override public int onStartCommand(Intent intent,int flags,int id) {
        boolean confirm=intent!=null&&CONFIRM_CLOSE.equals(intent.getAction());
        startForeground(520,notification(this,CHANNEL,"悬浮助手已开启",
                new Intent(this,FloatingAssistant.class).setAction(CONFIRM_CLOSE)));
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        if(panel!=null){
            if(confirm)confirmClose();
            return START_NOT_STICKY;
        }
        windows=getSystemService(WindowManager.class);
        layout=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        layout.gravity=Gravity.TOP|Gravity.LEFT;
        layout.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        layout.y=Ui.dp(this,150);
        right=true;
        createPanel();
        showExpanded(false);
        try{
            windows.addView(panel,layout);
            instance=this;
            getSharedPreferences("settings",0).registerOnSharedPreferenceChangeListener(appearanceListener);
            if(confirm)confirmClose();
        }catch(RuntimeException error){stopSelf();}
        return START_NOT_STICKY;
    }

    static Notification notification(Service service,String channel,String label,Intent stop) {
        NotificationManager manager=service.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(channel,label,NotificationManager.IMPORTANCE_LOW));
        PendingIntent close=PendingIntent.getService(service,0,stop,PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(service,channel).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(label).setOngoing(true)
                .addAction(new Notification.Action.Builder(null,"关闭",close).build()).build();
    }

    private void resolvePalette(){
        boolean dark=AgendaWidget.dark(this);
        Configuration config=new Configuration();
        config.uiMode=dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO;
        theme=new ContextThemeWrapper(createConfigurationContext(config),R.style.Theme_DayFlow);
        if(ThemeColors.usesProvider(this))
            theme.getTheme().applyStyle(ProviderAppearance.current(getSharedPreferences("settings",0)).style,true);
        accent=ThemeColors.accent(this,dark);
    }

    private void updatePalette(){
        resolvePalette();
        applyPanelBackground(expanded);
        recolor(panel);
        recolor(controls);
        input.setHintTextColor(Ui.muted(theme));
    }

    private void recolor(View view){
        if("edge_handle".equals(view.getTag()))view.setBackground(edgeLine());
        else if(view instanceof ImageButton icon)icon.setImageTintList(ColorStateList.valueOf(accent));
        else if(view instanceof TextView text){
            text.setTextColor(Ui.ink(theme));
            if(text.getTag() instanceof Boolean own)text.setBackground(bubbleBackground(own));
        }else if(view instanceof ViewGroup group){
            for(int i=0;i<group.getChildCount();i++)recolor(group.getChildAt(i));
        }
    }

    @Override public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);
        if(panel!=null){updatePalette();showExpanded(expanded);}
    }

    private void createPanel(){
        resolvePalette();
        panel=Ui.column(theme);
        panel.setElevation(Ui.dp(theme,6));

        controls=Ui.column(theme);
        controls.setPadding(Ui.dp(theme,8),Ui.dp(theme,4),Ui.dp(theme,8),Ui.dp(theme,6));
        LinearLayout bar=Ui.row(theme);
        controls.addView(bar,new LinearLayout.LayoutParams(-1,-2));
        View move=addIcon(bar,R.drawable.ic_assistant_sparkle,"移动",null,true);
        draggable(move);
        addIcon(bar,R.drawable.ic_assistant_capture,"截屏",this::capture,true);
        addIcon(bar,R.drawable.ic_assistant_open,"打开序言",this::openApp,true);
        addIcon(bar,android.R.drawable.ic_menu_close_clear_cancel,"关闭",this::confirmClose,true);
        addIcon(bar,R.drawable.ic_assistant_fold,"折叠",()->showExpanded(false),true);

        scroll=new ScrollView(theme);
        scroll.setFillViewport(true);
        messages=Ui.column(theme);
        scroll.addView(messages);
        controls.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        attachment=Ui.row(theme);
        preview=new ImageView(theme);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setContentDescription("截图预览");
        attachment.addView(preview,new LinearLayout.LayoutParams(0,Ui.dp(theme,84),1));
        addIcon(attachment,android.R.drawable.ic_menu_close_clear_cancel,"移除截图",this::clearAttachment);
        controls.addView(attachment,new LinearLayout.LayoutParams(-1,-2));
        attachment.setVisibility(View.GONE);

        LinearLayout compose=Ui.row(theme);
        input=new EditText(theme);
        input.setHint("问序言…");
        input.setTextSize(14);
        input.setTextColor(Ui.ink(theme));
        input.setHintTextColor(Ui.muted(theme));
        input.setMaxLines(2);
        compose.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        addIcon(compose,android.R.drawable.ic_menu_send,"发送",this::send);
        controls.addView(compose,new LinearLayout.LayoutParams(-1,-2));
    }

    private ImageButton icon(int res,String description,Runnable action){
        ImageButton button=new ImageButton(theme);
        button.setImageResource(res);
        button.setImageTintList(ColorStateList.valueOf(accent));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(Ui.dp(theme,11),Ui.dp(theme,11),Ui.dp(theme,11),Ui.dp(theme,11));
        button.setContentDescription(description);
        button.setTooltipText(description);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(LiquidGlass.tint(accent,40)),null,
                new ColorDrawable(Color.WHITE)));
        if(action!=null)button.setOnClickListener(v->action.run());
        else button.setClickable(true);
        return button;
    }

    private View addIcon(LinearLayout row,int res,String label,Runnable action,boolean weighted){
        LinearLayout item=Ui.column(theme);
        item.setGravity(Gravity.CENTER);
        ImageButton button=icon(res,label,action);
        button.setPadding(Ui.dp(theme,14),Ui.dp(theme,14),Ui.dp(theme,14),Ui.dp(theme,14));
        item.addView(button,new LinearLayout.LayoutParams(Ui.dp(theme,48),Ui.dp(theme,48)));
        TextView caption=Ui.text(theme,label,10,false);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(2);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        item.addView(caption,new LinearLayout.LayoutParams(-1,-2));
        row.addView(item,new LinearLayout.LayoutParams(weighted?0:Ui.dp(theme,48),-2,weighted?1:0));
        return button;
    }

    private void addIcon(LinearLayout row,int res,String label,Runnable action){
        addIcon(row,res,label,action,false);
    }

    private void openApp(){
        try{
            startActivity(new Intent(this,MainActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }catch(RuntimeException error){message("无法打开序言");}
    }

    private void confirmClose(){
        if(closeDialog!=null&&closeDialog.isShowing())return;
        closeDialog=new AlertDialog.Builder(theme)
                .setTitle("关闭悬浮助手？")
                .setNegativeButton("取消",null)
                .setPositiveButton("关闭",(dialog,which)->stopSelf())
                .create();
        Window window=closeDialog.getWindow();
        if(window!=null)window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        try{closeDialog.show();}
        catch(WindowManager.BadTokenException error){closeDialog=null;message("暂时无法显示关闭确认");}
    }

    private void showExpanded(boolean open){
        if(expanded&&!open&&input.getWindowToken()!=null){
            getSystemService(InputMethodManager.class).hideSoftInputFromWindow(input.getWindowToken(),0);
            input.clearFocus();
        }
        expanded=open;
        if(!open&&!busy&&pendingApprovals==0)releaseSession();
        panel.removeAllViews();
        panel.setElevation(open?Ui.dp(theme,6):0);
        int screen=getResources().getDisplayMetrics().widthPixels;
        int width=open?Math.min(screen-Ui.dp(theme,24),Ui.dp(theme,320)):Ui.dp(theme,24);
        int height=open?Math.min(getResources().getDisplayMetrics().heightPixels*2/5,Ui.dp(theme,320)):Ui.dp(theme,56);
        applyPanelBackground(open);
        if(open){
            panel.setFocusableInTouchMode(true);
            panel.addView(controls,new LinearLayout.LayoutParams(-1,-1));
            panel.requestFocus();
        }else{
            FrameLayout handle=new FrameLayout(theme);
            handle.setContentDescription("展开侧边悬浮助手");
            handle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            handle.setOnClickListener(v->showExpanded(true));
            View line=new View(theme);
            line.setTag("edge_handle");
            line.setBackground(edgeLine());
            FrameLayout.LayoutParams grip=new FrameLayout.LayoutParams(Ui.dp(theme,4),Ui.dp(theme,32),
                    Gravity.CENTER_VERTICAL|(right?Gravity.RIGHT:Gravity.LEFT));
            if(right)grip.rightMargin=Ui.dp(theme,2);else grip.leftMargin=Ui.dp(theme,2);
            handle.addView(line,grip);
            panel.addView(handle,new LinearLayout.LayoutParams(-1,-1));
            draggable(handle);
        }
        layout.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|(open?0:WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        layout.width=width;
        layout.height=height;
        int inset=open?Ui.dp(theme,8):0;
        layout.x=right?Math.max(inset,screen-width-inset):inset;
        layout.y=Math.min(layout.y,Math.max(0,getResources().getDisplayMetrics().heightPixels-height-Ui.dp(theme,32)));
        updateWindow();
    }

    private GradientDrawable edgeLine(){
        GradientDrawable line=new GradientDrawable();
        line.setColor(LiquidGlass.tint(accent,230));
        line.setCornerRadius(Ui.dp(theme,2));
        return line;
    }

    private void applyPanelBackground(boolean open){
        if(!open){panel.setBackground(null);return;}
        boolean themeMask=!"neutral".equals(getSharedPreferences("settings",0).getString(MASK_COLOR,"theme"));
        int base=themeMask?Ui.surface(theme):AgendaWidget.dark(this)?0xff202124:0xfff6f7f9;
        int fill=themeMask?androidx.core.graphics.ColorUtils.blendARGB(base,accent,.22f):base;
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{LiquidGlass.tint(fill,233),LiquidGlass.tint(base,207)});
        shape.setCornerRadius(Ui.dp(theme,22));
        shape.setStroke(Ui.dp(theme,1),LiquidGlass.tint(accent,136));
        panel.setBackground(shape);
    }

    private void updateWindow(){
        if(panel.isAttachedToWindow())windows.updateViewLayout(panel,layout);
    }

    private void draggable(View view){
        view.setOnTouchListener(new View.OnTouchListener(){
            private float x,y;
            private int left,top;
            private boolean dragged;
            @Override public boolean onTouch(View v,MotionEvent event){
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        x=event.getRawX();y=event.getRawY();left=layout.x;top=layout.y;dragged=false;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        int dx=Math.round(event.getRawX()-x),dy=Math.round(event.getRawY()-y);
                        if(!dragged&&Math.abs(dx)+Math.abs(dy)<ViewConfiguration.get(theme).getScaledTouchSlop())return false;
                        dragged=true;
                        layout.x=Math.max(0,Math.min(left+dx,getResources().getDisplayMetrics().widthPixels-layout.width));
                        layout.y=Math.max(0,Math.min(top+dy,getResources().getDisplayMetrics().heightPixels-layout.height));
                        updateWindow();return true;
                    case MotionEvent.ACTION_UP:
                        if(!dragged)return false;
                        v.setPressed(false);
                        right=layout.x+layout.width/2>getResources().getDisplayMetrics().widthPixels/2;
                        if(!expanded)showExpanded(false);
                        return true;
                    default:return false;
                }
            }
        });
    }

    private void capture(){
        if(capturing||busy)return;
        capturing=true;
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(input.getWindowToken(),0);
        try{
            startActivity(new Intent(this,FloatingAssistantActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch(RuntimeException error){restoreHandle();message("无法启动截屏");}
    }

    private void renderAttachment(){
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize=4;
        preview.setImageBitmap(BitmapFactory.decodeFile(screenshot.getPath(),options));
        attachment.setVisibility(View.VISIBLE);
    }

    private void clearAttachment(){
        if(screenshot!=null)screenshot.delete();
        screenshot=null;
        preview.setImageDrawable(null);
        attachment.setVisibility(View.GONE);
    }

    private void send(){
        if(busy)return;
        String text=input.getText().toString().trim();
        File image=screenshot;
        if(text.isEmpty()&&image==null)return;
        input.setText("");
        addMessage(text.isEmpty()?"已发送截图":text,true);
        busy=true;
        screenshot=null;
        preview.setImageDrawable(null);
        attachment.setVisibility(View.GONE);
        worker.execute(()->{
            try{
                if(closed)return;
                String active=AssistantPlugin.floatingSession(this,token);
                token=active;
                if(engine==null)engine=new ChatEngine(this,active);
                else engine.renewSession(active);
                if(closed){AssistantPlugin.closeFloating(this,active);return;}
                String encoded=image==null?null:"data:image/jpeg;base64,"+
                        android.util.Base64.encodeToString(Files.readAllBytes(image.toPath()),android.util.Base64.NO_WRAP);
                engine.send(text,encoded,new ChatEngine.Listener(){
                    public boolean active(){return !closed&&!Thread.currentThread().isInterrupted();}
                    public void message(String value){main.post(()->{if(!closed)addMessage(value,false);});}
                    public void tool(String name,JSONObject result){main.post(()->{if(!closed)toolResult(result);});}
                });
            }catch(Exception error){main.post(()->{if(!closed)addMessage(error.getMessage()==null?"AI 请求失败":error.getMessage(),false);});}
            finally{
                if(image!=null)image.delete();
                main.post(()->{if(!closed){busy=false;if(!expanded&&pendingApprovals==0)releaseSession();AgendaWidget.refresh(this);ReminderReceiver.refresh(this);}});
            }
        });
    }

    private void toolResult(JSONObject result){
        if(result.optBoolean("requires_approval")){
            pendingApprovals++;
            addMessage(result.optString("message","待批准"),false);
            LinearLayout row=Ui.row(theme);
            addIcon(row,android.R.drawable.checkbox_on_background,"批准",()->approve(result,row,true));
            addIcon(row,android.R.drawable.ic_menu_close_clear_cancel,"拒绝",()->approve(result,row,false));
            messages.addView(row);
            scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
        }else if(result.has("error"))addMessage(result.optString("error"),false);
        else if(result.optBoolean("created")||result.optBoolean("changed"))
            addMessage((result.optBoolean("deleted")?"已删除 · ":"已保存 · ")+result.optString("title"),false);
    }

    private void approve(JSONObject result,View row,boolean yes){
        if(busy){message("请等待当前回复完成");return;}
        busy=true;
        row.setVisibility(View.GONE);
        worker.execute(()->{
            try{
                JSONObject reply=engine.call("assistant_command_approve",new JSONObject()
                        .put("token",result.optString("approval_token")).put("approved",yes));
                main.post(()->{if(!closed){messages.removeView(row);pendingApprovals--;addMessage(reply.optString("message",yes?"已批准":"已拒绝"),false);}});
            }catch(Exception error){
                main.post(()->{if(!closed){messages.removeView(row);pendingApprovals--;addMessage(error.getMessage(),false);}});
            }finally{
                main.post(()->{if(!closed){busy=false;if(!expanded&&pendingApprovals==0)releaseSession();AgendaWidget.refresh(this);ReminderReceiver.refresh(this);}});
            }
        });
    }

    private void addMessage(String text,boolean own){
        if(text==null||text.isEmpty())return;
        TextView bubble=Ui.text(theme,text,14,false);
        bubble.setTextIsSelectable(true);
        bubble.setPadding(Ui.dp(theme,9),Ui.dp(theme,6),Ui.dp(theme,9),Ui.dp(theme,6));
        bubble.setTag(own);
        bubble.setBackground(bubbleBackground(own));
        LinearLayout.LayoutParams position=new LinearLayout.LayoutParams(-2,-2);
        position.gravity=own?Gravity.END:Gravity.START;
        position.bottomMargin=Ui.dp(theme,5);
        messages.addView(bubble,position);
        if(messages.getChildCount()>48){
            for(int i=0;i<messages.getChildCount();i++){
                if(messages.getChildAt(i) instanceof TextView){messages.removeViewAt(i);break;}
            }
        }
        scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
    }

    private GradientDrawable bubbleBackground(boolean own){
        GradientDrawable fill=new GradientDrawable();
        fill.setColor(LiquidGlass.tint(own?androidx.core.graphics.ColorUtils.blendARGB(Ui.surface(theme),accent,.28f):Ui.surface(theme),217));
        fill.setCornerRadius(Ui.dp(theme,12));
        return fill;
    }

    private void releaseSession(){
        AssistantPlugin.closeFloating(this,token);
    }

    private void message(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}

    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){
        closed=true;
        if(closeDialog!=null)closeDialog.dismiss();
        getSharedPreferences("settings",0).unregisterOnSharedPreferenceChangeListener(appearanceListener);
        instance=null;
        worker.shutdownNow();
        releaseSession();
        if(panel!=null&&panel.isAttachedToWindow())windows.removeViewImmediate(panel);
        if(screenshot!=null)screenshot.delete();
        super.onDestroy();
    }
}
