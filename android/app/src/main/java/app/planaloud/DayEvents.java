package app.planaloud;

import android.content.Context;
import android.graphics.*;
import java.time.*;

import java.util.*;
import org.json.*;

/** Shared event intervals and colors for the home clock and RemoteViews bitmap. */
final class DayEvents {
    static final class Segment {
        final int start,end,color,lane,border;final String title;
        Segment(int s,int e,int color,int lane,String title){this(s,e,color,lane,title,0);}
        Segment(int s,int e,int color,int lane,String title,int border){start=s;end=e;this.color=color;this.lane=lane;this.title=title;this.border=border;}
        String label(){return (start==0&&end==1440?"全天":clock(start)+"–"+clock(end))+" · "+title;}
    }
    static String clock(int minute){return minute==1440?"24:00":String.format(Locale.ROOT,"%02d:%02d",minute/60,minute%60);}
    static List<Segment> load(Context c,LocalDate day){
        List<JSONObject> records=new ArrayList<>();try{JSONArray data=Data.get(c).scheduled(day,day.plusDays(1),false);Set<String> seen=new HashSet<>();for(int i=0;i<data.length();i++){JSONObject r=data.getJSONObject(i);if(!r.optString("start_at").isEmpty()&&!"waiting".equals(r.optString("activation"))){records.add(r);seen.add(r.optString("id")+"|"+r.optString("occurrence_date"));}}JSONArray projects=Data.get(c).list("project",500);for(int i=0;i<projects.length();i++){JSONObject r=projects.getJSONObject(i);if(!r.optString("start_at").isEmpty()&&r.optString("recurrence").isEmpty()&&!PlanTime.cancelled(r)&&!"waiting".equals(r.optString("activation"))&&seen.add(r.optString("id")+"|"+r.optString("occurrence_date")))records.add(r);}}catch(Exception e){Diagnostics.record(c,"day-events",e);}
        records.sort(Comparator.comparing(r->r.optString("start_at")));
        List<Segment> result=new ArrayList<>();List<Integer> lanes=new ArrayList<>();LocalDateTime first=day.atStartOfDay(),last=day.plusDays(1).atStartOfDay();
        for(JSONObject r:records){try{
            LocalDateTime start=PlanTime.start(r),end=WeekSpans.end(r);if(r.optBoolean("all_day")){start=start.toLocalDate().atStartOfDay();if(r.optString("end_at").isEmpty()||!end.isAfter(start))end=start.plusDays(1);}
            if(!end.isAfter(first)||!start.isBefore(last))continue;
            int from=(int)Math.max(0,Duration.between(first,start).toMinutes()),to=(int)Math.min(1440,Duration.between(first,end).toMinutes());if(to<=from)continue;
            int lane=0;while(lane<lanes.size()&&lanes.get(lane)>from)lane++;if(lane==lanes.size())lanes.add(to);else lanes.set(lane,to);
            int color=color(r);
            result.add(new Segment(from,to,color,lane,r.optString("title"),EventColors.border(c,r)));
        }catch(Exception ignored){}}
        return result;
    }
    static int color(JSONObject r){return EventPalette.color(r);}
    static int lanes(List<Segment> events){int count=1;for(Segment s:events)count=Math.max(count,s.lane+1);return count;}
    static java.util.List<Segment> at(java.util.List<Segment> events,LocalTime now){int minute=DayTime.minute(now);java.util.List<Segment> active=new ArrayList<>();for(Segment event:events)if(event.start<=minute&&minute<event.end)active.add(event);return active;}
    static final class Tracks {
        final float main,outer,gap,top,height;final int count;
        Tracks(List<Segment> events,float main,float gap){this.main=main;this.outer=main*.75f;this.gap=gap;count=lanes(events);top=(count/2)*(outer+gap);height=top+main+((count-1)/2)*(outer+gap);}
        float y(Segment event){if(event.lane==0)return top;int row=(event.lane+1)/2;return event.lane%2==1?top-row*(outer+gap):top+main+gap+(row-1)*(outer+gap);}
        float thickness(Segment event){return event.lane==0?main:outer;}
    }
    static Tracks tracks(List<Segment> events,float main,float gap){return new Tracks(events,main,gap);}
    static void draw(Canvas canvas,Paint paint,List<Segment> events,float left,float top,float width,float thickness,int outline){draw(canvas,paint,events,left,top,width,tracks(events,thickness,thickness*.5f),outline);}
    static void draw(Canvas canvas,Paint paint,List<Segment> events,float left,float top,float width,Tracks tracks,int outline){
        for(Segment s:events){float x=left+width*s.start/1440f,right=Math.max(left+width*s.end/1440f,x+1),y=top+tracks.y(s),height=tracks.thickness(s),bottom=y+height,radius=height/2;paint.setStyle(Paint.Style.FILL);paint.setColor(s.color);canvas.drawRoundRect(x,y,right,bottom,radius,radius,paint);if(s.border==0)continue;float stroke=Math.min(Math.max(.6f,height*.12f),Math.min(right-x,height)/2);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(s.border);canvas.drawRoundRect(x+stroke/2,y+stroke/2,right-stroke/2,bottom-stroke/2,radius,radius,paint);paint.setStyle(Paint.Style.FILL);}
    }
    static Bitmap widget(Context c,LocalDate day){return widget(c,day,240);}
    static Bitmap widget(Context c,LocalDate day,int widgetWidth){
        List<Segment> events=load(c,day);float width=Math.max(100,widgetWidth-20),scale=900/width;Tracks tracks=tracks(events,6,2);Bitmap image=Bitmap.createBitmap(900,Math.max(1,(int)Math.ceil(tracks.height*scale)),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(scale,scale);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Path capsule=new Path();capsule.addRoundRect(new RectF(0,tracks.top,width,tracks.top+tracks.main),3,3,Path.Direction.CW);canvas.save();canvas.clipPath(capsule);paint.setColor(0x445f7188);canvas.drawRect(0,tracks.top,width,tracks.top+tracks.main,paint);paint.setColor(0x665298e5);float current=width*DayTime.minute(LocalTime.now())/1440f;canvas.drawRect(0,tracks.top,current,tracks.top+tracks.main,paint);canvas.restore();draw(canvas,paint,events,0,0,width,tracks,AgendaWidget.dark(c)?Color.WHITE:Color.BLACK);paint.setColor(ThemeColors.accent(c,AgendaWidget.dark(c)));paint.setStrokeWidth(1);canvas.drawLine(current,tracks.top,current,tracks.top+tracks.main,paint);return image;
    }
}
