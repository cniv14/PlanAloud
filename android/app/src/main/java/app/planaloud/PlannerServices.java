package app.planaloud;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.*;

public final class PlannerServices {
    public static String login(Context c,String address,String username,String password,String invite,boolean register)throws Exception{
        SharedPreferences p=c.getSharedPreferences("settings",0);String base=Network.serverEndpoint(address),identity=base+"|"+username.trim().toLowerCase(Locale.ROOT),binding=p.getString("binding","");
        if(!binding.isEmpty()&&!binding.equals(identity))throw new Exception("当前本地数据已绑定其他账号，请先导出备份后使用独立数据目录。");
        JSONObject result=Network.request(base+"/v1/auth/"+(register?"register":"login"),new JSONObject().put("username",username).put("password",password).put("invite_code",invite).put("device_id",p.getString("device_id","")).put("device_name",Build.MANUFACTURER+" "+Build.MODEL),"");
        String old=p.getString("user_id","");if(!old.isEmpty()&&!old.equals(result.getString("user_id")))throw new Exception("服务器账号身份发生变化，请先备份数据");
        Secrets.put(c,"token",result.getString("token"));p.edit().putString("server",base).putString("username",username.trim().toLowerCase(Locale.ROOT)).putString("binding",identity).putString("user_id",result.getString("user_id")).putString("sync_backend","dayflow").putBoolean("sync_auto",true).commit();SyncJob.configure(c);return "登录成功，已开启全部内容自动同步";
    }
    public static String weather(Context c,double lat,double lon,String city)throws Exception{
        return Forecast.refresh(c,lat,lon,city,"");
    }
}
