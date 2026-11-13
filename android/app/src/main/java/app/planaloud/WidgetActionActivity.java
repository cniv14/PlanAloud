package app.planaloud;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import org.json.JSONObject;

/** Collection templates route taps through an explicit, non-exported activity. */
public final class WidgetActionActivity extends Activity {
    protected void onCreate(Bundle state){
        super.onCreate(state);Intent i=getIntent();String id=i.getStringExtra("record_id"),series=i.getStringExtra("series_id"),date=i.getStringExtra("occurrence_date");
        if(id!=null&&id.matches("[a-f0-9-]{36}")){
            if(i.getBooleanExtra("complete",false)){Context c=getApplicationContext();new Thread(()->{try{JSONObject r=Data.get(c).resolve(id,series,date);if(r!=null&&!r.optBoolean("deleted")&&r.optString("completed").isEmpty())TaskActions.complete(c,r);AgendaWidget.refresh(c);ReminderReceiver.refresh(c);}catch(Exception ignored){}finally{runOnUiThread(this::finish);}},"widget-complete").start();overridePendingTransition(0,0);return;}
            else startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("record_id",id).putExtra("series_id",series).putExtra("occurrence_date",date));
        }
        finish();overridePendingTransition(0,0);
    }
}
