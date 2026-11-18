package app.planaloud;
import android.content.Context;
import java.util.*;
import org.json.*;
/** One user-facing style; legacy fields remain readable for existing records and plugins. */
final class CardStyles {
    static List<String> ids(Context c){List<String> result=new ArrayList<>(Arrays.asList("color","texture:matte","texture:glass","texture:gradient","texture:flow"));List<String> backgrounds=CardBackgrounds.ids(c);result.addAll(backgrounds.subList(1,backgrounds.size()));return result;}
    static List<String> names(Context c){List<String> result=new ArrayList<>(Arrays.asList("纯色（默认）","哑光","玻璃","渐变","流光"));List<String> backgrounds=CardBackgrounds.names(c);result.addAll(backgrounds.subList(1,backgrounds.size()));return result;}
    static String current(Context c,JSONObject r){String bg=r==null?CardBackgrounds.prefs(c).getString("card_background","color"):r.optString("card_background","color"),finish=r==null?CardBackgrounds.prefs(c).getString("card_texture","none"):r.optString("card_texture","none");return bg.equals("color")&&!finish.equals("none")?"texture:"+finish:bg;}
    static String automatic(JSONObject r){if(r.has("card_background")||r.has("card_texture"))return null;return r.optString("priority").equals("urgent")?"motion:gold":r.optString("priority").equals("high")?"motion:prism":null;}
    static boolean valid(String value){return CardBackgrounds.valid(value)||Arrays.asList("texture:matte","texture:glass","texture:gradient","texture:flow").contains(value);}
    static void apply(JSONObject record,String value)throws JSONException{if(value.isEmpty()){record.remove("card_background");record.remove("card_texture");return;}record.put("card_background",value.startsWith("texture:")?"color":value).put("card_texture",value.startsWith("texture:")?value.substring(8):"none");}
    static void save(Context c,String value){CardBackgrounds.prefs(c).edit().putString("card_background",value.startsWith("texture:")?"color":value).putString("card_texture",value.startsWith("texture:")?value.substring(8):"none").apply();}
}
