package app.planaloud.voice;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import android.speech.*;
import android.view.*;
import android.widget.Toast;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Plugin entry point for in-place press-to-talk. It never launches an activity. */
public final class VoiceInput {
 public static AutoCloseable bind(Activity activity,View button,Consumer<String> text,Consumer<String> status){return new Capture(activity,button,text,status);}
 private VoiceInput(){}
 private static final class Capture implements View.OnTouchListener,AutoCloseable,Application.ActivityLifecycleCallbacks {
  final Activity a;final View button;final Consumer<String> text,status;final Handler main=new Handler(Looper.getMainLooper());final ExecutorService worker=Executors.newSingleThreadExecutor();
  volatile boolean closed,recording,accepted;volatile AudioRecord microphone;boolean busy,held,systemMode;long started;SpeechRecognizer recognizer;int generation;String systemResult;
  final Runnable limit=()->release(true);
  Capture(Activity a,View button,Consumer<String> text,Consumer<String> status){this.a=a;this.button=button;this.text=text;this.status=status;button.setOnTouchListener(this);button.setContentDescription("按住说话，松开识别，上滑取消");button.setOnClickListener(v->say("按住麦克风说话，松开识别，上滑取消"));a.getApplication().registerActivityLifecycleCallbacks(this);}
  void say(String value){if(!closed)status.accept(value);}
  public boolean onTouch(View v,MotionEvent e){
   switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:if(!v.isEnabled())return false;if(busy){say("正在识别，请稍候");return true;}if(a.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7301);say("授权麦克风后，再次按住说话");return true;}if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(true);start();return true;
    case MotionEvent.ACTION_MOVE:if(held&&e.getY()<-48*a.getResources().getDisplayMetrics().density)release(false);return true;
    case MotionEvent.ACTION_UP:if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(false);if(held)release(true);v.setPressed(false);return true;
    case MotionEvent.ACTION_CANCEL:if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(false);release(false);return true;
    default:return true;
   }
  }
  void start(){
   if(closed||busy)return;VoiceSettings.migrate(a);int mode=a.getSharedPreferences("settings",0).getInt("voice_provider",3);systemMode=mode==0;held=true;accepted=false;started=SystemClock.elapsedRealtime();button.setPressed(true);
   try{if(systemMode){startSystem();return;}int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IOException("设备不支持语音录音");AudioRecord recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(8192,min*2));if(recorder.getState()!=AudioRecord.STATE_INITIALIZED){recorder.release();throw new IOException("麦克风不可用");}try{recorder.startRecording();}catch(RuntimeException error){recorder.release();throw error;}microphone=recorder;recording=true;busy=true;say("正在录音 · 松开识别，上滑取消");main.postDelayed(limit,120000);worker.execute(()->record(recorder,mode));}
   catch(Exception error){if(systemMode)cancelSystem();held=false;busy=false;recording=false;button.setPressed(false);say(error.getMessage()==null?"无法开始录音":error.getMessage());}
  }
  void release(boolean submit){if(!held&&!recording)return;held=false;accepted=submit&&SystemClock.elapsedRealtime()-started>=300;main.removeCallbacks(limit);button.setPressed(false);if(systemMode){if(systemResult!=null){String value=systemResult;systemResult=null;if(accepted)deliver(value);else say(submit?"录音太短，请按住说话":"已取消录音");cancelSystem();return;}if(recognizer!=null){if(accepted){say("正在识别…");recognizer.stopListening();main.postDelayed(()->{if(busy&&systemMode){cancelSystem();say("识别超时，请重试");}},15000);}else{cancelSystem();say(submit?"录音太短，请按住说话":"已取消录音");}}return;}recording=false;AudioRecord recorder=microphone;if(recorder!=null)try{recorder.stop();}catch(RuntimeException ignored){}say(accepted?"正在识别…":submit?"录音太短，请按住说话":"已取消录音");}
  void record(AudioRecord recorder,int mode){File wav=null;try{
   wav=File.createTempFile("voice-input-",".wav",a.getCacheDir());try(RandomAccessFile out=new RandomAccessFile(wav,"rw")){out.write(new byte[44]);byte[] block=new byte[4096];int total=0;while(recording&&!closed&&total<3840000){int n=recorder.read(block,0,Math.min(block.length,3840000-total));if(n<0){if(!recording)break;throw new IOException("录音中断，请重试");}if(n>0){out.write(block,0,n);total+=n;}}if(total>=3840000&&!closed){accepted=true;main.post(()->{held=false;main.removeCallbacks(limit);say("正在识别…");});}header(out,total);}
   finally{try{recorder.stop();}catch(RuntimeException ignored){}recorder.release();microphone=null;recording=false;}
   if(accepted&&!closed){String value=new CloudSpeech(a.getApplicationContext()).transcribe(wav,mode);main.post(()->deliver(value));}
  }catch(Exception error){main.post(()->say(error.getMessage()==null?"语音识别失败，请重试":error.getMessage()));}
   finally{if(wav!=null)wav.delete();main.post(()->{busy=false;held=false;main.removeCallbacks(limit);button.setPressed(false);});}
  }
  void deliver(String value){if(closed||!accepted)return;if(value==null||value.trim().isEmpty()){say("未识别到文字，请重试");return;}text.accept(value.trim());say("已填入输入框，可编辑后发送");}
  void startSystem(){systemResult=null;if(!SpeechRecognizer.isRecognitionAvailable(a))throw new IllegalStateException("系统语音服务不可用，请在 AI → 语音识别中切换服务");cancelSystem();busy=true;held=true;int session=++generation;recognizer=SpeechRecognizer.createSpeechRecognizer(a);recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle p){}public void onBeginningOfSpeech(){}public void onRmsChanged(float v){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onPartialResults(Bundle b){}public void onEvent(int e,Bundle b){}public void onError(int e){if(session!=generation||closed)return;held=false;busy=false;button.setPressed(false);say("系统语音未完成（"+e+"），请重试");}public void onResults(Bundle b){if(session!=generation||closed)return;ArrayList<String> result=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(held){systemResult=result!=null&&!result.isEmpty()?result.get(0):"";return;}busy=false;button.setPressed(false);main.removeCallbacks(limit);if(result!=null&&!result.isEmpty())deliver(result.get(0));else say("未识别到文字，请重试");}});Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN").putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);say("正在录音 · 松开识别，上滑取消");recognizer.startListening(intent);main.postDelayed(limit,120000);}
  void cancelSystem(){generation++;systemResult=null;main.removeCallbacks(limit);if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}busy=false;}
  void cancel(){boolean wasBusy=busy;accepted=false;release(false);if(wasBusy)say("已取消录音");if(systemMode)cancelSystem();main.removeCallbacks(limit);}
  @Override public void close(){if(closed)return;cancel();closed=true;main.removeCallbacksAndMessages(null);button.setOnTouchListener(null);a.getApplication().unregisterActivityLifecycleCallbacks(this);worker.shutdownNow();}
  public void onActivityPaused(Activity activity){if(activity==a)cancel();}public void onActivityDestroyed(Activity activity){if(activity==a)close();}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityResumed(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}
 }
 private static void header(RandomAccessFile out,int size)throws IOException{ByteBuffer b=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(size+36).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put("data".getBytes(StandardCharsets.US_ASCII)).putInt(size);out.seek(0);out.write(b.array());}
}
