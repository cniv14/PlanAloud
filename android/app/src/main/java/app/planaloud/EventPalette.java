package app.planaloud;
import android.content.*;
import android.graphics.Color;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

/** Persistent allocation avoids palette collisions between different event groups. */
final class EventPalette {
    private static final int[] PASTELS={0xfff5bfd2,0xffbcdcf5,0xffbee7d3,0xffffdfb4,0xffd8c5f2,0xfff7c9b4,0xffbee7e7,0xffe3e8b7,0xfff0c4e8,0xffc9d2f4,0xfff4e3b9,0xffc7e7c1};
    static final String[] VALUES={"macaron","pink","crystal","forest","sunset"},LABELS={"马卡龙系（默认）","粉系","水晶系","森林系","落日系"};
    private static final int[][] PALETTES={PASTELS,
        {0xffffc1d5,0xfff7b4cf,0xffffd4df,0xffe9b8dc,0xffffc9c3,0xffe8c5ee,0xfff8d6ed,0xffefb8be,0xffffd9d1,0xffdcc5ed,0xfff5c4dd,0xffffe1ea},
        {0xffbceafa,0xffd3c5fc,0xffc2f2e7,0xffe6d5ff,0xffbcd9ff,0xffeac9ef,0xffd3f3fc,0xffc4d7f3,0xffd8f5e9,0xfff1dcf8,0xffc2eced,0xffdde5ff},
        {0xffbfe4c8,0xffdbe8b8,0xffb6ddd0,0xffd2e9d9,0xffc5dbb3,0xffe7e6bc,0xffb5d7bf,0xffd9edc5,0xffc5e3dc,0xffe9dfc4,0xffcddcbc,0xffc6e6cf},
        {0xffffc7ac,0xffffdca9,0xfff5b8c8,0xffffd0bf,0xffe8c2e7,0xffffe2c5,0xfff4c0a5,0xfff8d2d4,0xffe0c4ed,0xffffe8b6,0xfff2bcc0,0xffeed7c6}};
    private static SharedPreferences settings;
    private static SharedPreferences prefs;private static final Map<String,Integer> colors=new HashMap<>();private static int next;
    static synchronized void init(Context c){settings=c.getApplicationContext().getSharedPreferences("settings",0);SharedPreferences current=c.getApplicationContext().getSharedPreferences("event_palette_v2",0);if(prefs==current)return;prefs=current;colors.clear();next=prefs.getInt("next",0);for(Map.Entry<String,?> entry:prefs.getAll().entrySet())if(entry.getKey().startsWith("c.")&&entry.getValue() instanceof Integer){colors.put(entry.getKey(),(Integer)entry.getValue());}}
    static String identity(JSONObject r){String series=r.optString("series_id"),id=r.optString("id"),name=r.optString("title").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");String identity;if(!series.isEmpty())identity="series:"+series;else if(!r.optString("recurrence").isEmpty()&&!id.isEmpty())identity="series:"+id;else if("course".equals(r.optString("kind"))&&!name.isEmpty())identity="course:"+name;else if(!id.isEmpty())identity="record:"+id;else identity=r.optString("kind")+":"+name;return "c."+UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));}
    static String selected(){String value=settings==null?"macaron":settings.getString("event_palette","macaron");return Arrays.asList(VALUES).contains(value)?value:"macaron";}
    static int at(String palette,int index){int family=Math.max(0,Arrays.asList(VALUES).indexOf(palette));int[] colors=PALETTES[family];if(index<colors.length)return colors[index];float[] hsv=new float[3];Color.colorToHSV(colors[index%colors.length],hsv);hsv[0]=(hsv[0]+(index/colors.length)*7.3f)%360;hsv[1]=Math.min(.42f,hsv[1]+(index/colors.length%7)*.012f);return Color.HSVToColor(hsv);}
    static synchronized int color(JSONObject r){
        String custom=r.optString("color");if(custom.matches("#[0-9a-fA-F]{6}"))return Color.parseColor(custom);
        String key=identity(r),slotKey="i."+key.substring(2);Integer prior=colors.get(key);int slot=prefs==null?-1:prefs.getInt(slotKey,-1);
        if(slot<0){if(prior!=null){for(int i=0;i<PASTELS.length;i++)if(PASTELS[i]==prior){slot=i;break;}}if(slot<0)slot=next++;if(prefs!=null)prefs.edit().putInt(slotKey,slot).putInt("next",next).apply();}
        if(selected().equals("macaron")&&prior!=null)return prior;
        int value=at(selected(),slot);if(prior==null){colors.put(key,at("macaron",slot));if(prefs!=null)prefs.edit().putInt(key,at("macaron",slot)).apply();}return value;
    }
    private EventPalette(){}
}
