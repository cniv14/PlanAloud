package app.planaloud;

import android.content.Context;
import android.graphics.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Device-local wallpaper library and deterministic rule selection. No executable expressions. */
final class WallpaperRules {
    private static File folder(Context c){return new File(c.getFilesDir(),"wallpapers");}
    private static AtomicFile config(Context c){return new AtomicFile(new File(folder(c),"rules.json"));}
    static File image(Context c,String id){if(!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("无效壁纸 ID");return new File(folder(c),id+".jpg");}
    static synchronized JSONObject read(Context c)throws Exception{
        try(InputStream in=config(c).openRead()){return new JSONObject(Network.read(in,256*1024));}
        catch(FileNotFoundException e){return new JSONObject().put("schema",1).put("rotate_minutes",0).put("images",new JSONArray()).put("rules",new JSONArray());}
    }
    static synchronized void save(Context c,JSONObject rules)throws Exception{
        if(rules.getInt("schema")!=1||rules.getInt("rotate_minutes")<0||rules.getInt("rotate_minutes")>10080)throw new IOException("轮播间隔应为 0–10080 分钟（0 关闭）");
        JSONArray images=rules.getJSONArray("images"),entries=rules.getJSONArray("rules");Set<String> ids=new HashSet<>();
        byte[] encoded=rules.toString(2).getBytes(StandardCharsets.UTF_8);
        if(images.length()>256||entries.length()>256||encoded.length>256*1024)throw new IOException("壁纸配置过大");
        for(int i=0;i<images.length();i++){JSONObject r=images.getJSONObject(i);String id=r.getString("id");if(!ids.add(id)||!image(c,id).isFile()||r.getString("name").trim().isEmpty())throw new IOException("壁纸不存在、重名 ID 或名称为空");}
        if(!rules.optString("fallback").isEmpty()&&!ids.contains(rules.optString("fallback")))throw new IOException("默认壁纸不在图库中");
        for(int i=0;i<entries.length();i++){JSONObject r=entries.getJSONObject(i);if(!ids.contains(r.getString("image")))throw new IOException("规则引用了未导入的壁纸");r.getInt("priority");new Expression(r.getString("when"),(k,v)->false).parse();}
        FileOutputStream out=null;try{out=config(c).startWrite();out.write(encoded);config(c).finishWrite(out);}catch(Exception e){if(out!=null)config(c).failWrite(out);throw e;}
    }
    static synchronized String add(Context c,String name,Bitmap bitmap)throws Exception{
        if(!folder(c).isDirectory()&&!folder(c).mkdirs())throw new IOException("无法创建壁纸图库");
        String id=UUID.randomUUID().toString();File file=image(c,id);
        try{try(OutputStream out=new FileOutputStream(file)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,85,out))throw new IOException("无法保存图片");}JSONObject rules=read(c);rules.getJSONArray("images").put(new JSONObject().put("id",id).put("name",name));save(c,rules);return id;}catch(Exception e){file.delete();throw e;}
    }
    static synchronized void remove(Context c,String id)throws Exception{
        JSONObject state=read(c);JSONArray old=state.getJSONArray("images"),images=new JSONArray(),rules=new JSONArray(),oldRules=state.getJSONArray("rules");
        for(int i=0;i<old.length();i++)if(!id.equals(old.getJSONObject(i).getString("id")))images.put(old.get(i));
        for(int i=0;i<oldRules.length();i++)if(!id.equals(oldRules.getJSONObject(i).getString("image")))rules.put(oldRules.get(i));
        if(id.equals(state.optString("fallback")))state.remove("fallback");save(c,state.put("images",images).put("rules",rules));image(c,id).delete();
    }
    static File selected(Context c){
        File legacy=new File(c.getFilesDir(),"wallpaper.jpg");
        try{JSONObject state=read(c);JSONArray rules=state.getJSONArray("rules"),images=state.getJSONArray("images");LocalDateTime now=LocalDateTime.now();Environment env=new Environment(c,now);int best=Integer.MIN_VALUE;List<String> matches=new ArrayList<>();
            for(int i=0;i<rules.length();i++){JSONObject r=rules.getJSONObject(i);int priority=r.getInt("priority");String id=r.getString("image");if(priority<best||!image(c,id).isFile()||!new Expression(r.getString("when"),env::matches).parse())continue;if(priority>best){matches.clear();best=priority;}if(!matches.contains(id))matches.add(id);}
            int minutes=state.optInt("rotate_minutes");if(matches.isEmpty()&&minutes>0)for(int i=0;i<images.length();i++){String id=images.getJSONObject(i).getString("id");if(image(c,id).isFile())matches.add(id);}
            if(!matches.isEmpty()){long slot=minutes<=0?0:Instant.now().getEpochSecond()/(60L*minutes);return image(c,matches.get((int)Math.floorMod(slot,(long)matches.size())));}
            String fallback=state.optString("fallback");if(!fallback.isEmpty()&&image(c,fallback).isFile())return image(c,fallback);
        }catch(Exception ignored){}return legacy;
    }
    private static final class Environment {
        final Context c;final LocalDateTime now;JSONArray events;String weather;
        Environment(Context c,LocalDateTime now){this.c=c;this.now=now;}
        boolean matches(String kind,String value)throws Exception{
            if(kind.equals("time")){int hour=now.getHour();switch(value){case "night":return hour>=19||hour<6;case "day":return hour>=6&&hour<19;case "morning":return hour>=6&&hour<12;case "afternoon":return hour>=12&&hour<19;case "weekday":return now.getDayOfWeek().getValue()<=5;case "weekend":return now.getDayOfWeek().getValue()>5;default:String[] range=value.split("-",-1);LocalTime start=LocalTime.parse(range[0]),end=LocalTime.parse(range[1]),t=now.toLocalTime();return start.isBefore(end)?!t.isBefore(start)&&t.isBefore(end):!t.isBefore(start)||t.isBefore(end);}}
            if(kind.equals("weather")){if(weather==null){android.content.SharedPreferences p=c.getSharedPreferences("settings",0);String cached=OpenWeatherSources.cached(c,"");if(System.currentTimeMillis()-p.getLong("weather_updated",0)>6*3600000L)cached="";if(cached.isEmpty()){String forecast=Forecast.summary(c,now.toLocalDate());cached=forecast.contains("缓存待刷新")?"":forecast;}weather=cached.matches(".*[晴云阴雨雪雾].*")?cached:"";}switch(value){case "sunny":return weather.contains("晴");case "cloudy":return weather.contains("云")||weather.contains("阴");case "rain":return weather.contains("雨");case "snow":return weather.contains("雪");case "fog":return weather.contains("雾");default:return weather.isEmpty();}}
            if(events==null)events=Data.get(c).range(now.toLocalDate(),now.toLocalDate().plusDays(1),false);
            boolean active=false;for(int i=0;i<events.length();i++){JSONObject r=events.getJSONObject(i);if(r.optString("start_at").isEmpty()||"waiting".equals(r.optString("activation")))continue;LocalDateTime start=PlanTime.start(r),end=PlanTime.end(r);if(now.isBefore(start)||!now.isBefore(end))continue;active=true;if(value.equals("active")||value.equals(r.optString("kind"))||value.startsWith("title:")&&r.optString("title").toLowerCase(Locale.ROOT).contains(value.substring(6).toLowerCase(Locale.ROOT)))return true;}return value.equals("none")&&!active;
        }
    }
    interface Predicate{boolean test(String kind,String value)throws Exception;}
    private static final class Expression {
        final String text;final Predicate test;int position,depth;
        Expression(String text,Predicate test)throws IOException{if(text.length()>2048)throw new IOException("条件表达式过长");this.text=text;this.test=test;}
        boolean parse()throws Exception{boolean result=or();space();if(position!=text.length())throw invalid();return result;}
        void space(){while(position<text.length()&&Character.isWhitespace(text.charAt(position)))position++;}
        boolean take(String token){space();if(!text.startsWith(token,position))return false;position+=token.length();return true;}
        boolean or()throws Exception{boolean value=and();while(take("||"))value|=and();return value;}
        boolean and()throws Exception{boolean value=atom();while(take("&&"))value&=atom();return value;}
        boolean atom()throws Exception{
            if(take("(")){if(++depth>16)throw invalid();boolean result=or();if(!take(")"))throw invalid();depth--;return result;}
            space();int begin=position;while(position<text.length()&&Character.isLetter(text.charAt(position)))position++;String kind=text.substring(begin,position);if(kind.equals("always"))return true;
            if(!take("["))throw invalid();int end=text.indexOf(']',position);if(end<0)throw invalid();String value=text.substring(position,end).trim();position=end+1;
            if(kind.equals("wether"))kind="weather";
            if(kind.equals("time")){if(!Arrays.asList("night","day","morning","afternoon","weekday","weekend").contains(value)){if(!value.matches("[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}"))throw invalid();String[] range=value.split("-");if(LocalTime.parse(range[0]).equals(LocalTime.parse(range[1])))throw invalid();}}
            else if(kind.equals("weather")){if(!Arrays.asList("sunny","cloudy","rain","snow","fog","unknown").contains(value))throw invalid();}
            else if(kind.equals("event")){if(!Arrays.asList("active","none","task","event","course","project").contains(value)&&!(value.startsWith("title:")&&value.length()>6))throw invalid();}
            else throw invalid();return test.test(kind,value);
        }
        IOException invalid(){return new IOException("条件格式有误，位置 "+(position+1)+"；例如 time[night] && weather[sunny]");}
    }
}
