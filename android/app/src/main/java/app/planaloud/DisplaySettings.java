package app.planaloud;

import android.widget.*;
import android.os.*;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.slider.Slider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import app.planaloud.ui.UiScale;
import app.planaloud.ui.DisplayTrial;

final class DisplaySettings {
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("界面与字体大小",()->new SettingsPanels(a).appearance());page.addView(Ui.text(a,"界面与字体大小",22,true));page.addView(Ui.help(a,"界面缩放控制控件与留白，字体大小单独控制文字；都以手机当前设置为基准，只影响序言。"));
        Slider ui=slider(a,page,"界面缩放",70,130,UiScale.interfacePercent(a.prefs.getInt("ui_scale",100))),font=slider(a,page,"字体大小",80,160,UiScale.fontPercent(a.prefs.getInt("font_scale",100)));
        TextView preview=Ui.text(a,"预览：今天 09:00 整理资料",16,true);page.addView(preview);font.addOnChangeListener((s,v,user)->preview.setTextSize(16*v/UiScale.fontPercent(a.prefs.getInt("font_scale",100))));
        page.addView(Ui.button(a,"应用",true,()->apply(a,Math.round(ui.getValue()),Math.round(font.getValue()))));page.addView(Ui.button(a,"恢复默认",false,()->apply(a,100,100)));page.addView(Ui.help(a,"应用后有 10 秒确认时间；点击保留才保存，超时或点击还原会恢复上一次设置。旋转、切后台和重新打开应用不会延长倒计时。系统字体和桌面小组件不会被改动。"));
    }
    private static void apply(MainActivity a,int ui,int font){if(DisplayTrial.begin(a,ui,font,a.prefs.getString("ui_style","classic")))a.recreate();else a.message("显示大小未改变");}
    static boolean resumeTrial(MainActivity a){
        if(!DisplayTrial.pending(a))return false;
        if(DisplayTrial.remaining(a)<=0){DisplayTrial.revert(a);dismissTrial(a);a.recreate();return true;}
        if(a.displayTrial==null&&!a.isFinishing()&&!a.isDestroyed()){a.displayTrial=new TrialPrompt(a);a.displayTrial.show();}return false;
    }
    static void dismissTrial(MainActivity a){if(a.displayTrial!=null)a.displayTrial.dispose();}
    static final class TrialPrompt {
        final MainActivity a;final Handler handler=new Handler(Looper.getMainLooper());AlertDialog dialog;boolean disposed;
        final Runnable tick=new Runnable(){public void run(){if(disposed)return;if(!DisplayTrial.pending(a)){dispose();return;}long left=DisplayTrial.remaining(a);if(left<=0){restore();return;}int seconds=(int)((left+999)/1000);showCountdown(seconds);handler.postDelayed(this,Math.min(1000,left));}};
        private void showCountdown(int seconds){String lead=seconds+" 秒后自动还原";android.text.SpannableString message=new android.text.SpannableString(lead+"\n\n界面 "+a.prefs.getInt("ui_scale",100)+"% · 字体 "+a.prefs.getInt("font_scale",100)+"% · "+UiStyle.name(a)+"\n\n请确认保留；无操作会恢复上一次设置。");message.setSpan(new android.text.style.ForegroundColorSpan(AgendaWidget.dark(a)?0xffff6b6b:0xffd32f2f),0,lead.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);message.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,lead.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);message.setSpan(new android.text.style.RelativeSizeSpan(1.35f),0,lead.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);dialog.setMessage(message);}
        TrialPrompt(MainActivity a){this.a=a;}
        void show(){dialog=new MaterialAlertDialogBuilder(a).setTitle("保留新的显示设置？").setPositiveButton("保留",(d,w)->{boolean kept=DisplayTrial.confirm(a);dispose();if(!kept)a.recreate();else a.message("已保留显示设置");}).setNegativeButton("还原",(d,w)->restore()).create();dialog.setCanceledOnTouchOutside(false);dialog.setOnCancelListener(d->restore());dialog.setOnDismissListener(d->dispose());dialog.show();tick.run();}
        void restore(){if(disposed)return;boolean pending=DisplayTrial.pending(a);DisplayTrial.revert(a);dispose();if(pending&&!a.isDestroyed())a.recreate();}
        void dispose(){if(disposed)return;disposed=true;handler.removeCallbacks(tick);if(a.displayTrial==this)a.displayTrial=null;if(dialog!=null&&dialog.isShowing())dialog.dismiss();}
    }
    private static Slider slider(MainActivity a,LinearLayout page,String name,int min,int max,int value){TextView label=Ui.text(a,name+" · "+value+"%",16,true);page.addView(label);Slider slider=new Slider(a);slider.setContentDescription(name);slider.setValueFrom(min);slider.setValueTo(max);slider.setStepSize(5);slider.setValue(Math.round(value/5f)*5);slider.addOnChangeListener((s,v,user)->label.setText(name+" · "+Math.round(v)+"%"));page.addView(slider);return slider;}
}
