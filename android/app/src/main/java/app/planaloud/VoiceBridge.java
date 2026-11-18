package app.planaloud;
import android.app.Activity;import android.view.View;import java.util.function.Consumer;
/** Stable host bridge used by separately updated assistant and voice plugins. */
public final class VoiceBridge {
 public static AutoCloseable bind(Activity a,View button,Consumer<String> text,Consumer<String> status){
  if(!a.getSharedPreferences("settings",0).getBoolean("plugin_enabled_app.planaloud.voice",true)){button.setOnClickListener(v->status.accept("语音插件已停用，请在扩展管理中开启"));return ()->{};}
  try{String name="app.planaloud.voice.VoiceInput";ClassLoader loader=ModuleUpdates.loader(name);Class<?> type=Class.forName(name,true,loader==null?a.getApplication().getClassLoader():loader);AutoCloseable capture=(AutoCloseable)type.getMethod("bind",Activity.class,View.class,Consumer.class,Consumer.class).invoke(null,a,button,text,status);ModuleUpdates.voiceReady();return capture;}
  catch(Exception error){ModuleUpdates.disable("voice","语音插件未能启动，已回到内置版本");Diagnostics.record(a,"voice-bind",error);return app.planaloud.voice.VoiceInput.bind(a,button,text,status);}
 }
 private VoiceBridge(){}
}
