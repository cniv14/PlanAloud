package app.planaloud;
import android.content.*;
import android.graphics.Color;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

/** Persistent allocation avoids palette collisions between different event groups. */
final class EventPalette {
    private static final int[] PASTELS={0xfff5bfd2,0xffbcdcf5,0xffbee7d3,0xffffdfb4,0xffd8c5f2,0xfff7c9b4,0xffbee7e7,0xffe3e8b7,0xfff0c4e8,0xffc9d2f4,0xfff4e3b9,0xffc7e7c1};
    private static SharedPreferences prefs;private static final Map<String,Integer> colors=new HashMap<>();private static final Set<Integer> used=new HashSet<>();private static int next;
    static synchronized void init(Context c){SharedPreferences current=c.getApplicationContext().getSharedPreferences("event_palette_v2",0);if(prefs==current)return;prefs=current;colors.clear();used.clear();next=prefs.getInt("next",0);for(Map.Entry<String,?> entry:prefs.getAll().entrySet())if(entry.getKey().startsWith("c.")&&entry.getValue() instanceof Integer){colors.put(entry.getKey(),(Integer)entry.getValue());used.add((Integer)entry.getValue());}}
    static String identity(JSONObject r){String series=r.optString("series_id"),id=r.optString("id"),name=r.optString("title").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");String identity;if(!series.isEmpty())identity="series:"+series;else if(!r.optString("recurrence").isEmpty()&&!id.isEmpty())identity="series:"+id;else if("course".equals(r.optString("kind"))&&!name.isEmpty())identity="course:"+name;else if(!id.isEmpty())identity="record:"+id;else identity=r.optString("kind")+":"+name;return "c."+UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));}
    static synchronized int color(JSONObject r){String custom=r.optString("color");if(custom.matches("#[0-9a-fA-F]{6}"))return Color.parseColor(custom);String key=identity(r);Integer prior=colors.get(key);if(prior!=null)return prior;int selected;do{int index=next++;selected=index<PASTELS.length?PASTELS[index]:Color.HSVToColor(new float[]{(float)(index*137.507764%360),.19f+(index/360%5)*.018f,.94f+(index/1800%3)*.02f});}while(used.contains(selected));colors.put(key,selected);used.add(selected);if(prefs!=null)prefs.edit().putInt(key,selected).putInt("next",next).apply();return selected;}
    private EventPalette(){}
}
