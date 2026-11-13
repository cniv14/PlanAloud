package app.planaloud;

import android.app.Activity;
import android.content.*;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Toast;

/** Hosts Android’s screen capture authorization and returns one frame to the overlay. */
public final class FloatingAssistantActivity extends Activity {
    private static final int CAPTURE = 81;
    private static final String RESULT_ACTION = "app.planaloud.CAPTURE_FRAME";
    private String captureId;
    private boolean listening;
    private final BroadcastReceiver result = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (captureId == null || !captureId.equals(intent.getStringExtra("id"))) return;
            String completed = captureId;
            captureId = null;
            if(intent.getBooleanExtra("success",false))
                FloatingAssistant.acceptCapture(ScreenFrame.file(context,completed));
            else Toast.makeText(context,"未取得截图，受保护的窗口可能无法截取",Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) captureId = state.getString("capture_id");
        androidx.core.content.ContextCompat.registerReceiver(this,result,new IntentFilter(RESULT_ACTION),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        listening = true;
        if(state!=null)return;
        try{
            MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);
            startActivityForResult(manager.createScreenCaptureIntent(),CAPTURE);
        }catch(RuntimeException error){finish();}
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("capture_id",captureId);
        super.onSaveInstanceState(state);
    }

    @Override protected void onActivityResult(int request, int code, Intent data) {
        super.onActivityResult(request,code,data);
        if (request == CAPTURE && code == RESULT_OK && data != null) {
            FloatingAssistant.hideHandle();
            captureId = java.util.UUID.randomUUID().toString();
            try {
                startForegroundService(new Intent(this,ScreenFrame.class).putExtra("consent",data).putExtra("id",captureId));
                String expected = captureId;
                new android.os.Handler(getMainLooper()).postDelayed(()->{
                    if (!isDestroyed() && expected.equals(captureId)) finish();
                },12000);
            } catch (RuntimeException error) { finish(); }
        } else finish();
    }

    @Override protected void onDestroy() {
        if (listening) unregisterReceiver(result);
        if (!isChangingConfigurations()) FloatingAssistant.restoreHandle();
        super.onDestroy();
    }
}
