package app.planaloud;

import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.google.android.material.textfield.TextInputEditText;
import org.json.*;
import java.time.*;
import java.util.*;

/** Optional planning metadata. An estimate never creates a calendar time slot. */
final class PlanningFields {
    private static final String[] PRIORITY={"","low","normal","high","urgent"};
    private final TextInputEditText date,time,estimate;
    private final Spinner priority;
    PlanningFields(MainActivity a,LinearLayout form,JSONObject r){
        form.addView(Ui.text(a,"计划 · 均可留空",16,true));
        LinearLayout pair=Ui.row(a);form.addView(pair);
        LinearLayout left=Ui.column(a),right=Ui.column(a);
        Ui.cell(a,pair,left);Ui.cell(a,pair,right);
        date=Ui.field(a,left,"截止日期",r.optString("due_date"),false);
        date.setInputType(InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_DATE);
        time=Ui.field(a,right,"截止时刻",r.optString("due_time"),false);
        time.setInputType(InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_TIME);
        form.addView(Ui.button(a,"从日历选截止日期",false,()->{try{JSONObject draft=a.editorState.draft();LocalDate day;try{day=LocalDate.parse(draft.optString("due_date"));}catch(Exception e){day=LocalDate.now();}MonthBrowser.select(a,day,d->{try{a.edit(draft.put("due_date",d.toString()));}catch(Exception e){a.message(e.getMessage());}},()->a.edit(draft),false);}catch(Exception e){a.message(e.getMessage());}}));
        LinearLayout details=Ui.row(a),el=Ui.column(a),pr=Ui.column(a);form.addView(details);Ui.cell(a,details,el);Ui.cell(a,details,pr);
        estimate=Ui.field(a,el,"预计用时 · 分钟",r.has("estimate_minutes")?r.optString("estimate_minutes"):"",false);estimate.setInputType(InputType.TYPE_CLASS_NUMBER);
        priority=TaskTypes.spinner(a,pr,"重要程度",Arrays.asList("未设定","低优先","普通","重要","紧急"));priority.setSelection(Math.max(0,Arrays.asList(PRIORITY).indexOf(r.optString("priority"))));
    }
    void apply(JSONObject r)throws JSONException{
        r.put("due_date",date.getText().toString().trim()).put("due_time",time.getText().toString().trim());
        String raw=estimate.getText().toString().trim();if(raw.isEmpty())r.remove("estimate_minutes");else try{r.put("estimate_minutes",Integer.parseInt(raw));}catch(NumberFormatException e){throw new JSONException("预计用时请填写整数分钟");}
        if(priority.getSelectedItemPosition()<=0)r.remove("priority");else r.put("priority",PRIORITY[priority.getSelectedItemPosition()]);
    }
    static void validate(JSONObject r)throws Exception{
        String time=r.optString("due_time");if(!time.isEmpty()&&(r.optString("due_date").isEmpty()||!time.matches("[0-9]{2}:[0-9]{2}")))throw new Exception("截止时刻需同时填写日期，时刻格式为 HH:mm");
        if(!time.isEmpty())LocalTime.parse(time);
        if(r.has("estimate_minutes")&&!r.isNull("estimate_minutes")){Object n=r.get("estimate_minutes");if(!(n instanceof Integer||n instanceof Long)||r.getLong("estimate_minutes")<1||r.getLong("estimate_minutes")>525600)throw new Exception("预计用时需在 1–525600 分钟之间，或留空");}
        if(!Arrays.asList(PRIORITY).contains(r.optString("priority")))throw new Exception("无效优先级");
    }
    static String deadlineLabel(JSONObject r){String time=r.optString("due_time");return r.optString("due_date")+(time.isEmpty()?"":" "+time);}
}
