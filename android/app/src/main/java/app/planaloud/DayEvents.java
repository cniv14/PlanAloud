package app.planaloud;

import android.content.Context;
import android.graphics.*;
import java.time.*;

import java.util.*;
import org.json.*;

/** Shared event intervals and colors for the home clock and RemoteViews bitmap. */
final class DayEvents {
    private static final int[] COLORS={0xff5298e5,0xffe79c46,0xff8d78dc,0xff46b69e,0xffdd7896,0xff8eaa4c,0xffc17acc,0xff46adca};
    static final class Segment {
        final int start,end,color,lane;final String title;
        Segment(int s,int e,int color,int lane,String title){start=s;end=e;this.color=color;this.lane=lane;this.title=title;}
        String label(){return (start==0&&end==1440?"全天":clock(start)+"–"+clock(end))+" · "+title;}
    }
    static String clock(int minute){return minute==1440?"24:00":String.format(Locale.ROOT,"%02d:%02d",minute/60,minute%60);}
    static List<Segment> load(Context c,LocalDate day){
        List<JSONObject> records=new ArrayList<>();try{JSONArray data=Data.get(c).scheduled(day,day.plusDays(1),false);for(int i=0;i<data.length();i++){JSONObject r=data.getJSONObject(i);if(!r.optString("start_at").isEmpty()&&!"waiting".equals(r.optString("activation")))records.add(r);}}catch(Exception e){Diagnostics.record(c,"day-events",e);}
        records.sort(Comparator.comparing(r->r.optString("start_at")));
        List<Segment> result=new ArrayList<>();int[] lanes=new int[6];LocalDateTime first=day.atStartOfDay(),last=day.plusDays(1).atStartOfDay();
        for(JSONObject r:records){try{
            LocalDateTime start=PlanTime.start(r),end=PlanTime.end(r);if(r.optBoolean("all_day")){start=start.toLocalDate().atStartOfDay();if(r.optString("end_at").isEmpty()||!end.isAfter(start))end=start.plusDays(1);}
            if(!end.isAfter(first)||!start.isBefore(last))continue;
            int from=(int)Math.max(0,Duration.between(first,start).toMinutes()),to=(int)Math.min(1440,Duration.between(first,end).toMinutes());if(to<=from)continue;
            int lane=0;while(lane<5&&lanes[lane]>from)lane++;lanes[lane]=to;
            String identity=r.optString("series_id",r.optString("id",r.optString("title")));if(identity.isEmpty())identity=r.optString("id",r.optString("title"));int color=COLORS[Math.floorMod(identity.hashCode(),COLORS.length)];
            // Adjacent intervals use distinct colors even when their IDs hash to the same palette entry.
            if(!result.isEmpty()&&result.get(result.size()-1).color==color)color=COLORS[(Math.floorMod(identity.hashCode(),COLORS.length)+1)%COLORS.length];
            result.add(new Segment(from,to,color,lane,r.optString("title")));
        }catch(Exception ignored){}}
        return result;
    }
    static int lanes(List<Segment> events){int count=1;for(Segment s:events)count=Math.max(count,s.lane+1);return count;}
    static void draw(Canvas canvas,Paint paint,List<Segment> events,float left,float top,float width,float laneHeight){
        for(Segment s:events){float x=left+width*s.start/1440f,right=left+width*s.end/1440f,y=top+s.lane*laneHeight;paint.setColor(s.color);canvas.drawRoundRect(x,y,Math.max(right,x+2),y+Math.max(2,laneHeight-2),2,2,paint);}
    }
    static Bitmap widget(Context c,LocalDate day){List<Segment> events=load(c,day);Bitmap image=Bitmap.createBitmap(900,Math.max(12,lanes(events)*12),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0x445f7188);canvas.drawRect(0,0,900,image.getHeight(),paint);paint.setColor(0x665298e5);float current=900*DayTime.minute(LocalTime.now())/1440f;canvas.drawRect(0,0,current,image.getHeight(),paint);draw(canvas,paint,events,0,0,900,12);paint.setColor(ThemeColors.accent(c,AgendaWidget.dark(c)));paint.setStrokeWidth(4);canvas.drawLine(current,0,current,image.getHeight(),paint);return image;}
}
