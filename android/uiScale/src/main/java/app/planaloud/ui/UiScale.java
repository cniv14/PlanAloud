package app.planaloud.ui;

import android.content.Context;
import android.content.res.Configuration;

/** App-local display settings. System density and accessibility preferences are never modified. */
public final class UiScale {
    private UiScale(){}
    public static int interfacePercent(int value){return Math.max(70,Math.min(130,value));}
    public static int fontPercent(int value){return Math.max(80,Math.min(160,value));}
    public static Configuration configuration(Configuration source,int ui,int font){
        Configuration out=new Configuration(source);float scale=interfacePercent(ui)/100f;
        if(source.densityDpi>0)out.densityDpi=Math.max(1,Math.round(source.densityDpi*scale));
        if(source.screenWidthDp>0)out.screenWidthDp=Math.round(source.screenWidthDp/scale);
        if(source.screenHeightDp>0)out.screenHeightDp=Math.round(source.screenHeightDp/scale);
        if(source.smallestScreenWidthDp>0)out.smallestScreenWidthDp=Math.round(source.smallestScreenWidthDp/scale);
        out.fontScale=(source.fontScale>0?source.fontScale:1)*fontPercent(font)/100f;return out;
    }
    public static Context wrap(Context base){DisplayTrial.recoverIfExpired(base);android.content.SharedPreferences p=base.getSharedPreferences("settings",0);int ui=interfacePercent(p.getInt("ui_scale",100)),font=fontPercent(p.getInt("font_scale",100));return ui==100&&font==100?base:base.createConfigurationContext(configuration(base.getResources().getConfiguration(),ui,font));}
}
