package app.planaloud;
import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
public final class DayFlowApp extends Application implements app.planaloud.assistant.ConversationAppearance {
    public void onCreate(){super.onCreate();app.planaloud.voice.VoiceSettings.migrate(this);ModuleUpdates.init(this);HotUpdates.init(this);registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){public void onActivityResumed(android.app.Activity a){ModuleUpdates.resumed(a);}public void onActivityCreated(android.app.Activity a,android.os.Bundle b){}public void onActivityStarted(android.app.Activity a){}public void onActivityPaused(android.app.Activity a){}public void onActivityStopped(android.app.Activity a){}public void onActivitySaveInstanceState(android.app.Activity a,android.os.Bundle b){}public void onActivityDestroyed(android.app.Activity a){}});Holidays.init(this);SharedPreferences p=getSharedPreferences("settings",0);
        if(p.contains("qweather_host"))p.edit().remove("qweather_host").apply();
        SharedPreferences secrets=getSharedPreferences("secrets",0);
        if(secrets.contains("qweather_api_key"))secrets.edit().remove("qweather_api_key").apply();
        applyTheme(p.getString("theme","system"));if(!p.getBoolean("capture_unified_v1",false)){if(p.getBoolean("capture_sms",false))p.edit().putBoolean("capture",true).apply();p.edit().putBoolean("capture_unified_v1",true).apply();}if(!p.getBoolean("assistant_api_removed",false)){sendBroadcast(new android.content.Intent("app.planaloud.assistant.STOP").setPackage("app.planaloud.assistant"),"app.planaloud.permission.INTERNAL");p.edit().remove("assistant_api").putBoolean("assistant_api_removed",true).apply();getSharedPreferences("secrets",0).edit().remove("assistant_api").apply();}

        new Thread(()->{try{Data.get(this).expireTrash();}catch(Exception e){android.util.Log.w("Trash","Unable to expire trash",e);}},"trash-expiry").start();
        if(!p.getBoolean("md_files_ready",false))new Thread(()->{try{Data.get(this).materializeNotes();p.edit().putBoolean("md_files_ready",true).apply();}catch(Exception ignored){/* Retry migration next launch; never remove original records. */}},"markdown-migration").start();
    }
    @Override public android.view.View createConversationBackdrop(android.content.Context context){
        Backdrop backdrop=new Backdrop(context);
        backdrop.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(android.view.View view){backdrop.load();backdrop.active(true);}
            @Override public void onViewDetachedFromWindow(android.view.View view){backdrop.release();}
        });
        return backdrop;
    }
    @Override public void recordConversationError(String operation,Throwable failure){Diagnostics.record(this,operation,failure);}
    @Override public boolean applyConversationTheme(android.app.Activity activity){ThemeColors.apply(activity);return true;}
    @Override public void styleConversationSurface(android.view.View view,boolean own){
        android.content.Context c=view.getContext();float radius=Ui.compact(c)?16:20;
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setColor(LiquidGlass.tint(Ui.color(c,own?com.google.android.material.R.attr.colorSecondaryContainer:com.google.android.material.R.attr.colorSurfaceContainerLow),185));
        shape.setCornerRadius(Ui.dp(c,radius));shape.setStroke(Ui.dp(c,1),Ui.color(c,com.google.android.material.R.attr.colorOutlineVariant));
        view.setBackground(shape);LiquidGlass.outline(view,radius);
    }
    public static void applyTheme(String value){AppCompatDelegate.setDefaultNightMode("dark".equals(value)?AppCompatDelegate.MODE_NIGHT_YES:"light".equals(value)?AppCompatDelegate.MODE_NIGHT_NO:AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);}
}
