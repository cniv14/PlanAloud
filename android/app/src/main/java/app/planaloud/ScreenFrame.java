package app.planaloud;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.view.WindowManager;
import java.io.*;

public final class ScreenFrame extends Service {
    private final Handler main = new Handler(Looper.getMainLooper());
    private HandlerThread thread;
    private MediaProjection projection;
    private volatile ImageReader reader;
    private VirtualDisplay display;
    private String id;
    private boolean finished;

    static File file(android.content.Context context,String id) {
        return new File(new File(context.getCacheDir(),"screen-frames"),id+".jpg");
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if (intent == null || "stop".equals(intent.getAction())) {
            finishCapture(false);
            return START_NOT_STICKY;
        }
        if (id != null) return START_NOT_STICKY;
        id=intent.getStringExtra("id");
        if(id==null){stopSelf();return START_NOT_STICKY;}
        try {
            startForeground(521,FloatingAssistant.notification(this,"screen_frame","正在截取屏幕",
                    new Intent(this,ScreenFrame.class).setAction("stop")));
            Intent consent=intent.getParcelableExtra("consent");
            projection=getSystemService(MediaProjectionManager.class).getMediaProjection(RESULT_OK,consent);
            if(projection==null){finishCapture(false);return START_NOT_STICKY;}
            projection.registerCallback(new MediaProjection.Callback(){
                @Override public void onStop(){finishCapture(false);}
                @Override public void onCapturedContentResize(int width,int height){
                    if(width<1||height<1||display==null||finished)return;
                    try {
                        android.util.Size size=bounded(width,height);
                        if(reader.getWidth()==size.getWidth()&&reader.getHeight()==size.getHeight())return;
                        ImageReader old=reader;
                        reader=newReader(size);
                        display.resize(size.getWidth(),size.getHeight(),getResources().getDisplayMetrics().densityDpi);
                        display.setSurface(reader.getSurface());
                        if(old!=null)new Handler(thread.getLooper()).post(old::close);
                    } catch(RuntimeException error){finishCapture(false);}
                }
            },main);
            thread=new HandlerThread("screen-frame");
            thread.start();
            WindowManager manager=getSystemService(WindowManager.class);
            android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Rect bounds=manager.getMaximumWindowMetrics().getBounds();
                metrics.widthPixels=bounds.width();metrics.heightPixels=bounds.height();
            }else manager.getDefaultDisplay().getRealMetrics(metrics);
            android.util.Size size=bounded(metrics.widthPixels,metrics.heightPixels);
            reader=newReader(size);
            display=projection.createVirtualDisplay("PlanAloud screenshot",size.getWidth(),size.getHeight(),
                    getResources().getDisplayMetrics().densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.getSurface(),null,main);
            main.postDelayed(()->finishCapture(false),8000);
        } catch(RuntimeException error){finishCapture(false);}
        return START_NOT_STICKY;
    }

    private static android.util.Size bounded(int width,int height){
        float ratio=Math.min(1f,1440f/Math.max(width,height));
        return new android.util.Size(Math.max(1,Math.round(width*ratio)),Math.max(1,Math.round(height*ratio)));
    }

    private ImageReader newReader(android.util.Size size){
        ImageReader source=ImageReader.newInstance(size.getWidth(),size.getHeight(),PixelFormat.RGBA_8888,2);
        long ready=SystemClock.uptimeMillis()+350;
        boolean[] written={false};
        ImageReader.OnImageAvailableListener capture=buffer->{
            if(written[0]||buffer!=reader)return;
            try(Image image=buffer.acquireLatestImage()){
                if(image==null)return;
                written[0]=true;
                buffer.setOnImageAvailableListener(null,null);
                File target=file(this,id);
                File dir=target.getParentFile();
                if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建截图缓存");
                File[] old=dir.listFiles();
                if(old!=null)for(File item:old)if(System.currentTimeMillis()-item.lastModified()>3600000L)item.delete();
                File output=File.createTempFile("frame-",".tmp",dir);
                Image.Plane plane=image.getPlanes()[0];
                int columns=plane.getRowStride()/plane.getPixelStride();
                Bitmap padded=Bitmap.createBitmap(columns,image.getHeight(),Bitmap.Config.ARGB_8888);
                Bitmap frame=null;
                try {
                    padded.copyPixelsFromBuffer(plane.getBuffer());
                    frame=Bitmap.createBitmap(padded,0,0,image.getWidth(),image.getHeight());
                    try(OutputStream stream=new FileOutputStream(output)){
                        if(!frame.compress(Bitmap.CompressFormat.JPEG,88,stream))throw new IOException("截图写入失败");
                    }
                } finally {
                    if(frame!=null&&frame!=padded)frame.recycle();
                    padded.recycle();
                }
                main.post(()->{
                    if(finished||buffer!=reader){output.delete();return;}
                    boolean saved=output.renameTo(target);
                    if(!saved)output.delete();
                    finishCapture(saved);
                });
            }catch(Exception error){main.post(()->{if(buffer==reader)finishCapture(false);});}
        };
        Handler worker=new Handler(thread.getLooper());
        worker.postAtTime(()->{
            try {
                source.setOnImageAvailableListener(capture,worker);
                capture.onImageAvailable(source);
            } catch (IllegalStateException ignored) {}
        },ready);
        return source;
    }

    private void finishCapture(boolean success){
        if(finished)return;
        finished=true;
        if(id!=null){
            if(!success)file(this,id).delete();
            sendBroadcast(new Intent("app.planaloud.CAPTURE_FRAME").setPackage(getPackageName())
                    .putExtra("id",id).putExtra("success",success));
        }
        stopSelf();
    }

    @Override public void onDestroy(){
        finished=true;
        main.removeCallbacksAndMessages(null);
        if(display!=null)display.release();
        if(projection!=null)projection.stop();
        if(thread!=null){
            ImageReader closing=reader;
            if(closing!=null)new Handler(thread.getLooper()).post(closing::close);
            thread.quitSafely();
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
    private static final int RESULT_OK=android.app.Activity.RESULT_OK;
}
