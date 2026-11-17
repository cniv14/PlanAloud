package app.planaloud;

import android.content.*;
import org.json.JSONObject;

/** Widget completion never creates an activity or brings the app to the foreground. */
public final class WidgetActions extends BroadcastReceiver {
    static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
    @Override public void onReceive(Context context,Intent intent){
        String id=intent.getStringExtra("record_id");if(id==null||!id.matches("[a-f0-9-]{36}"))return;
        if(!intent.getBooleanExtra("complete",false)){
            context.startActivity(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("record_id",id).putExtra("series_id",intent.getStringExtra("series_id")).putExtra("occurrence_date",intent.getStringExtra("occurrence_date")));return;
        }
        PendingResult pending=goAsync();Context app=context.getApplicationContext();
        worker.execute(()->{try{complete(app,intent);}catch(Exception error){Diagnostics.record(app,"widget-complete",error);}finally{if(pending!=null)pending.finish();}});
    }
    static void complete(Context c,Intent intent)throws Exception{
        JSONObject record=Data.get(c).resolve(intent.getStringExtra("record_id"),intent.getStringExtra("series_id"),intent.getStringExtra("occurrence_date"));
        if(record!=null&&!record.optBoolean("deleted")&&record.optString("completed").isEmpty())TaskActions.complete(c,record);
        AgendaWidget.refresh(c);ReminderReceiver.refresh(c);
    }
}
