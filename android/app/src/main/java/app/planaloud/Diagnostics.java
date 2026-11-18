package app.planaloud;

import android.content.*;
import android.os.Build;
import android.widget.LinearLayout;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.*;

/** Local diagnostic events contain operation names and stack locations, never request bodies or secrets. */
final class Diagnostics {
    private static File file(Context c){return new File(c.getFilesDir(),"diagnostics.txt");}
    static synchronized void record(Context c,String operation,Throwable failure){
        try{
            StringBuilder line=new StringBuilder(java.time.Instant.now()+" "+operation+" "+failure.getClass().getName()+"\n");
            for(StackTraceElement frame:failure.getStackTrace())if(frame.getClassName().startsWith("app.planaloud"))line.append("  ").append(frame).append('\n');
            File target=file(c);String previous=target.isFile()?new String(Files.readAllBytes(target.toPath()),StandardCharsets.UTF_8):"";
            if(previous.length()>32000){int cut=previous.indexOf('\n',previous.length()-16000);previous=cut<0?"":previous.substring(cut+1);}
            Files.write(target.toPath(),(previous+line).getBytes(StandardCharsets.UTF_8));
        }catch(Exception ignored){}
    }
    static synchronized String export(Context c)throws Exception{
        android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);
        JSONObject state=new JSONObject().put("time",java.time.Instant.now().toString()).put("app_version",info.versionName).put("android_sdk",Build.VERSION.SDK_INT).put("device",Build.MANUFACTURER+" "+Build.MODEL);
        android.content.SharedPreferences p=c.getSharedPreferences("settings",0);
        for(String key:new String[]{"theme","color_source","home_columns","voice_provider","ai_timeout","sync_backend"})if(p.contains(key))state.put(key,p.getAll().get(key));
        return state.toString(2)+"\n\n"+(file(c).isFile()?new String(Files.readAllBytes(file(c).toPath()),StandardCharsets.UTF_8):"暂无错误事件\n");
    }
    static void show(MainActivity a){
        LinearLayout page=a.fullPage("诊断日志",a::home);
        page.addView(Ui.text(a,"诊断日志",22,true));
        page.addView(Ui.help(a,"记录操作名称、错误类型和代码位置，用于反馈故障。不会记录密钥、聊天正文或附件内容。"));
        page.addView(Ui.button(a,"导出诊断日志",true,()->a.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"planaloud-diagnostics.txt"),123)));
        page.addView(Ui.button(a,"清空日志",false,()->{file(a).delete();a.message("已清空诊断日志");}));
    }
}
