package app.planaloud;

import android.widget.LinearLayout;
import org.json.JSONArray;
import org.json.JSONObject;

final class InboxReview {
    static void pending(MainActivity a) {
        LinearLayout page=a.fullPage("待批准",a::showInbox);
        page.addView(Ui.text(a,"待批准",24,true));
        try {
            JSONArray commands=DayCommands.pendingApprovals(a);
            for(int i=0;i<commands.length();i++){
                JSONObject command=commands.getJSONObject(i);
                page.addView(Ui.text(a,command.getString("message"),16,true));
                LinearLayout actions=Ui.row(a);page.addView(actions);
                Ui.cell(a,actions,Ui.button(a,"批准",true,()->decide(a,command,true)));
                Ui.cell(a,actions,Ui.button(a,"拒绝",false,()->decide(a,command,false)));
            }
            if(TimetableCapture.hasDraft(a))
                page.addView(Ui.button(a,"核对待导入课表",false,()->new TimetableCapture(a,null)));
            JSONArray records=Data.get(a).list("inbox",500);
            int count=0;
            for(int i=0;i<records.length()&&count<50;i++){
                JSONObject record=records.getJSONObject(i);
                if(!"pending".equals(record.optString("confirmation_status"))||!record.has("proposed_kind"))continue;
                page.addView(Ui.button(a,record.optString("title"),false,()->a.detail(record)));
                count++;
            }
            if(commands.length()==0&&count==0&&!TimetableCapture.hasDraft(a))
                page.addView(Ui.help(a,"暂无待批准事项"));
        }catch(Exception e){a.message(e.getMessage());}
    }

    private static void decide(MainActivity a,JSONObject command,boolean allow){
        a.job(()->{
            JSONObject result=DayCommands.approve(a,command.getString("token"),null,allow);
            return result.optString("message",allow?"已批准":"已拒绝");
        },()->pending(a),message->pending(a));
    }

    static void trash(MainActivity a,int offset){
        LinearLayout page=a.fullPage("回收站",a::showInbox);
        page.addView(Ui.text(a,"回收站",24,true));
        page.addView(Ui.button(a,"最近操作与撤销",false,()->UndoHistory.show(a,()->trash(a,offset))));
        try{
            JSONArray rows=Data.get(a).trashPage(offset);
            if(rows.length()==0)page.addView(Ui.help(a,"这里没有已删除的记录"));
            for(int i=0;i<rows.length();i++){
                JSONObject record=rows.getJSONObject(i);
                LinearLayout row=Ui.row(a);page.addView(row);
                row.addView(Ui.text(a,record.optString("title"),16,true),new LinearLayout.LayoutParams(0,-2,1));
                row.addView(Ui.button(a,"恢复",false,()->a.job(()->{
                    Data.get(a).restoreDeleted(record);
                    return "已恢复";
                },()->trash(a,offset))),new LinearLayout.LayoutParams(Ui.dp(a,88),-2));
            }
            LinearLayout nav=Ui.row(a);page.addView(nav);
            if(offset>0)Ui.cell(a,nav,Ui.button(a,"上一页",false,()->trash(a,Math.max(0,offset-50))));
            if(rows.length()==50)Ui.cell(a,nav,Ui.button(a,"下一页",false,()->trash(a,offset+50)));
        }catch(Exception e){a.message(e.getMessage());}
    }
}
