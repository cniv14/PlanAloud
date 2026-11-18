package app.planaloud;
import android.view.View;
import org.json.JSONObject;
final class WeekActions {
    static void open(MainActivity a,JSONObject r){if(!UiStyle.neo(a)){a.detail(r);return;}String description=PlanTime.label(r)+(r.optString("location").isEmpty()?"":"\n"+r.optString("location"))+(WeekCompletion.done(r)?"\n已完成":"");new com.google.android.material.dialog.MaterialAlertDialogBuilder(a).setTitle(r.optString("title")).setMessage(description).setPositiveButton("关闭",null).setNeutralButton("交给 AI",(d,n)->AssistantPlugin.draft(a,"关于事件「"+r.optString("title")+"」（ID："+r.optString("id")+"），请帮我：")).show();}
    static void bind(MainActivity a,View view,JSONObject r,Runnable refresh){EventColors.contrast(view,r);if(PriorityPattern.present(r)){CharSequence description=view.getContentDescription();view.setContentDescription((description==null?r.optString("title")+"，"+PlanTime.label(r):description.toString())+"，"+PriorityPattern.label(r));}view.setOnClickListener(v->open(a,r));if(!UiStyle.neo(a))view.setOnLongClickListener(v->{TaskQuickActions.menu(a,view,r,refresh);return true;});}
}
