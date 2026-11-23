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
    private final TextInputEditText date,time,estimate,color;
    private final Spinner priority,background;
    private final java.util.List<String> backgroundIds;
    private final String originalBackground,originalTexture;private final int originalSelection;
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
        priority=TaskTypes.spinner(a,pr,"重要程度",Arrays.asList("未设定","● 低优先","◆ 普通","✦ 重要","▲ 紧急"));priority.setSelection(Math.max(0,Arrays.asList(PRIORITY).indexOf(r.optString("priority"))));
        color=Ui.field(a,form,"事件底色 · #RRGGBB，留空自动",r.optString("color"),false);
        form.addView(Ui.help(a,"不同事件自动分色，同一事件／重复系列保持同色，重要程度使用图案标记，不使用边框；重要默认淡棱彩，紧急默认柔金光，可在卡片样式中覆盖。"));
        originalBackground=r.has("card_background")?r.optString("card_background"):null;originalTexture=r.has("card_texture")?r.optString("card_texture"):null;
        backgroundIds=new java.util.ArrayList<>();backgroundIds.add("");backgroundIds.addAll(CardStyles.ids(a));java.util.List<String> backgrounds=new java.util.ArrayList<>();backgrounds.add("跟随默认卡片样式");backgrounds.addAll(CardStyles.names(a));String current=r.has("card_background")||r.has("card_texture")?CardStyles.current(a,r):"";int selected=backgroundIds.indexOf(current);if(selected<0){backgroundIds.add(current);backgrounds.add("本机缺少图片（保留引用）");selected=backgroundIds.size()-1;}background=TaskTypes.spinner(a,form,"本事件卡片样式",backgrounds);originalSelection=Math.max(0,selected);background.setSelection(originalSelection);form.addView(Ui.button(a,"命名导入并裁切新图片",false,()->CardBackgrounds.beginImport(a,true)));


    }
    void apply(JSONObject r)throws JSONException{
        if(background.getSelectedItemPosition()==originalSelection){if(originalBackground==null)r.remove("card_background");else r.put("card_background",originalBackground);if(originalTexture==null)r.remove("card_texture");else r.put("card_texture",originalTexture);}else CardStyles.apply(r,backgroundIds.get(background.getSelectedItemPosition()));
        String tint=color.getText().toString().trim();if(tint.isEmpty())r.remove("color");else r.put("color",tint);
        r.put("due_date",date.getText().toString().trim()).put("due_time",time.getText().toString().trim());
        String raw=estimate.getText().toString().trim();if(raw.isEmpty())r.remove("estimate_minutes");else try{r.put("estimate_minutes",Integer.parseInt(raw));}catch(NumberFormatException e){throw new JSONException("预计用时请填写整数分钟");}
        if(priority.getSelectedItemPosition()<=0)r.remove("priority");else r.put("priority",PRIORITY[priority.getSelectedItemPosition()]);
    }
    static void validate(JSONObject r)throws Exception{
        if(r.has("card_background")&&!CardBackgrounds.valid(r.optString("card_background")))throw new Exception("无效卡片背景引用");
        if(r.has("card_texture")&&!java.util.Arrays.asList(CardFinish.VALUES).contains(r.optString("card_texture")))throw new Exception("无效卡片质感");
        String tint=r.optString("color");if(!tint.isEmpty()&&!tint.matches("#[0-9a-fA-F]{6}"))throw new Exception("事件颜色格式为 #RRGGBB，或留空自动");
        String time=r.optString("due_time");if(!time.isEmpty()&&(r.optString("due_date").isEmpty()||!time.matches("[0-9]{2}:[0-9]{2}")))throw new Exception("截止时刻需同时填写日期，时刻格式为 HH:mm");
        if(!time.isEmpty())LocalTime.parse(time);
        if(r.has("estimate_minutes")&&!r.isNull("estimate_minutes")){Object n=r.get("estimate_minutes");if(!(n instanceof Integer||n instanceof Long)||r.getLong("estimate_minutes")<1||r.getLong("estimate_minutes")>525600)throw new Exception("预计用时需在 1–525600 分钟之间，或留空");}
        if(!Arrays.asList(PRIORITY).contains(r.optString("priority")))throw new Exception("无效优先级");
    }
    static String deadlineLabel(JSONObject r){String time=r.optString("due_time");return r.optString("due_date")+(time.isEmpty()?"":" "+time);}
}
