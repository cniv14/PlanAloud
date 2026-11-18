package app.planaloud.ui;

import android.content.*;
import android.os.SystemClock;

/** A persisted, fixed ten-second display trial; expiry is recovered before resources are wrapped. */
public final class DisplayTrial {
    private static final String PREFIX="display_trial_";
    private DisplayTrial(){}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("settings",0);}
    public static boolean pending(Context c){return prefs(c).getBoolean(PREFIX+"pending",false);}
    public static long remaining(Context c){SharedPreferences p=prefs(c);if(!p.getBoolean(PREFIX+"pending",false))return 0;long elapsed=SystemClock.elapsedRealtime();if(elapsed<p.getLong(PREFIX+"started",Long.MAX_VALUE))return 0;return Math.max(0,Math.min(p.getLong(PREFIX+"wall",0)-System.currentTimeMillis(),p.getLong(PREFIX+"elapsed",0)-elapsed));}
    public static boolean recoverIfExpired(Context c){return pending(c)&&remaining(c)<=0&&revert(c);}
    public static boolean begin(Context c,int ui,int font,String style){
        if(pending(c))revert(c);SharedPreferences p=prefs(c);ui=UiScale.interfacePercent(ui);font=UiScale.fontPercent(font);style="neo".equals(style)?"neo":"timetable".equals(style)?"timetable":"classic";
        if(ui==p.getInt("ui_scale",100)&&font==p.getInt("font_scale",100)&&style.equals(p.getString("ui_style","classic")))return false;
        long now=SystemClock.elapsedRealtime();return p.edit().putBoolean(PREFIX+"pending",true).putBoolean(PREFIX+"had_ui",p.contains("ui_scale")).putBoolean(PREFIX+"had_font",p.contains("font_scale")).putBoolean(PREFIX+"had_style",p.contains("ui_style")).putInt(PREFIX+"old_ui",p.getInt("ui_scale",100)).putInt(PREFIX+"old_font",p.getInt("font_scale",100)).putString(PREFIX+"old_style",p.getString("ui_style","classic")).putLong(PREFIX+"started",now).putLong(PREFIX+"elapsed",now+10000).putLong(PREFIX+"wall",System.currentTimeMillis()+10000).putInt("ui_scale",ui).putInt("font_scale",font).putString("ui_style",style).commit();
    }
    private static SharedPreferences.Editor clear(SharedPreferences p){SharedPreferences.Editor edit=p.edit();for(String key:p.getAll().keySet())if(key.startsWith(PREFIX))edit.remove(key);return edit;}
    public static boolean confirm(Context c){if(!pending(c))return false;if(remaining(c)<=0){revert(c);return false;}return clear(prefs(c)).commit();}
    public static boolean revert(Context c){SharedPreferences p=prefs(c);if(!p.getBoolean(PREFIX+"pending",false))return false;SharedPreferences.Editor edit=clear(p);if(p.getBoolean(PREFIX+"had_ui",false))edit.putInt("ui_scale",p.getInt(PREFIX+"old_ui",100));else edit.remove("ui_scale");if(p.getBoolean(PREFIX+"had_font",false))edit.putInt("font_scale",p.getInt(PREFIX+"old_font",100));else edit.remove("font_scale");if(p.getBoolean(PREFIX+"had_style",false))edit.putString("ui_style",p.getString(PREFIX+"old_style","classic"));else edit.remove("ui_style");return edit.commit();}
}
