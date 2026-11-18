package app.planaloud;
import android.text.*;
import android.text.style.StrikethroughSpan;
import org.json.JSONObject;

/** Completion changes only the name decoration, never week geometry or ordering. */
final class WeekCompletion {
    static boolean done(JSONObject record){return !record.optString("completed").isEmpty();}
    static CharSequence label(JSONObject record,CharSequence value){
        if(!done(record))return value;String title=record.optString("title");int start=value.toString().indexOf(title);if(title.isEmpty()||start<0)return value;
        SpannableString result=new SpannableString(value);result.setSpan(new StrikethroughSpan(),start,start+title.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return result;
    }
    static CharSequence title(JSONObject record){return label(record,record.optString("title"));}
    static CharSequence titles(java.util.List<JSONObject> records){SpannableStringBuilder result=new SpannableStringBuilder();for(JSONObject record:records){if(result.length()>0)result.append("、");result.append(title(record));}return result;}
}
