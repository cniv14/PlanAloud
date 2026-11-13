package app.planaloud;

import android.view.View;
import android.widget.*;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.card.MaterialCardView;
import java.time.*;
import java.util.*;
import org.json.*;

public final class DailyTasks {
    public static void show(MainActivity a,LocalDate day){
        LinearLayout page=a.fullPage("每日待办",a::home);page.addView(Ui.text(a,day.toString(),24,true));
        page.addView(Ui.help(a,ChineseDates.description(day)));
        rows(a,page,day,()->show(a,day));
        page.addView(Ui.button(a,"＋ 新建待办",true,()->{try{JSONObject draft=Data.make("task","");if(!day.equals(LocalDate.now()))draft.put("due_date",day.toString());a.edit(draft);}catch(Exception e){a.message(e.getMessage());}}));
    }
    public static void today(MainActivity a,LinearLayout page){rows(a,page,LocalDate.now(),a::home);}
    static void rows(MainActivity a,LinearLayout page,LocalDate day,Runnable refresh){
        try{
            Data db=Data.get(a);JSONArray result=db.dayTasks(day),deadlines=db.deadlines(day,day.equals(LocalDate.now()));
            Map<String,JSONObject> due=new LinkedHashMap<>();
            List<JSONObject> timed=new ArrayList<>(),floating=new ArrayList<>(),projects=new ArrayList<>(),done=new ArrayList<>();
            for(int i=0;i<result.length();i++){
                JSONObject r=result.getJSONObject(i);
                if(!r.optString("completed").isEmpty())done.add(r);
                else if(r.optString("start_at").isEmpty()&&!r.optString("due_date").isEmpty()&&r.optString("due_date").compareTo(day.toString())<=0)due.put(r.getString("id"),r);
                else if("project".equals(r.optString("kind")))projects.add(r);
                else if(!r.optString("start_at").isEmpty())timed.add(r);
                else floating.add(r);
            }
            for(int i=0;i<deadlines.length();i++){JSONObject r=deadlines.getJSONObject(i);due.putIfAbsent(r.getString("id"),r);}
            page.addView(Ui.help(a,"已完成 "+done.size()+" 项 · "+timed.size()+" 项已安排 · "+due.size()+" 项需关注截止"));
            group(a,page,"已安排",timed,day,refresh,false);
            group(a,page,"到期与逾期",new ArrayList<>(due.values()),day,refresh,true);
            group(a,page,"待安排",floating,day,refresh,false);
            group(a,page,"进行中的长任务",projects,day,refresh,false);
            if(!done.isEmpty()){
                LinearLayout completed=Ui.column(a);completed.setVisibility(View.GONE);
                page.addView(Ui.button(a,"已完成 · "+done.size(),false,()->completed.setVisibility(completed.getVisibility()==View.GONE?View.VISIBLE:View.GONE)));page.addView(completed);
                append(a,completed,done,0,day,refresh,false);
            }
            if(result.length()==0&&due.isEmpty())page.addView(Ui.help(a,"今天还没有安排，先记下要做的事。"));
        }catch(Exception e){a.message(e.getMessage());}
    }
    private static void group(MainActivity a,LinearLayout page,String title,List<JSONObject> rows,LocalDate day,Runnable refresh,boolean due){
        if(rows.isEmpty())return;page.addView(Ui.text(a,title+" · "+rows.size(),18,true));LinearLayout tiles=Ui.column(a);page.addView(tiles);append(a,tiles,rows,0,day,refresh,due);
    }
    private static void append(MainActivity a,LinearLayout target,List<JSONObject> rows,int offset,LocalDate day,Runnable refresh,boolean due){
        int columns=a.getResources().getConfiguration().screenWidthDp>=340&&a.getResources().getConfiguration().fontScale<1.35f?2:1;
        int end=Math.min(rows.size(),offset+8);LinearLayout line=null;
        for(int i=offset;i<end;i++){
            if((i-offset)%columns==0){line=Ui.row(a);line.setGravity(android.view.Gravity.TOP);target.addView(line);}
            JSONObject r=rows.get(i);String label=PlanTime.taskLabel(r,day,due,LocalDateTime.now());
            String meta=PlanTime.planning(r);
            MaterialCardView card=Ui.card(a);Ui.priority(card,r);LinearLayout body=Ui.column(a);body.setPadding(Ui.dp(a,8),0,Ui.dp(a,8),Ui.dp(a,8));
            LinearLayout header=Ui.row(a);MaterialCheckBox check=new MaterialCheckBox(a);check.setChecked(!r.optString("completed").isEmpty());check.setContentDescription("完成或恢复 "+r.optString("title"));header.addView(check,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,48)));
            TextView name=Ui.text(a,r.optString("title"),15,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(name,new LinearLayout.LayoutParams(0,-2,1));body.addView(header);
            TextView info=Ui.help(a,label+(meta.isEmpty()?"":" · "+meta));info.setMaxLines(3);body.addView(info);card.addView(body);
            check.setOnClickListener(v->{try{TaskActions.complete(a,r);a.changed();refresh.run();}catch(Exception e){check.setChecked(!r.optString("completed").isEmpty());a.message(e.getMessage());}});
            card.setOnClickListener(v->a.detail(r));card.setOnLongClickListener(v->{TaskQuickActions.menu(a,card,r,a::home,refresh);return true;});
            Ui.cell(a,line,card);if(i+1==end&&(end-offset)%columns!=0)Ui.cell(a,line,new View(a));
            if(offset>0)Ui.enter(card,i-offset);
        }
        if(end<rows.size()){View more=Ui.button(a,"再显示 · 还有 "+(rows.size()-end)+" 项",false,()->{});more.setOnClickListener(v->{target.removeView(more);append(a,target,rows,end,day,refresh,due);});target.addView(more);}
    }
}
