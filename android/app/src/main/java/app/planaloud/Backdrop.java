package app.planaloud;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.View;

/** Bounded bitmap and reusable paints. Animation only while foreground and visible. */
public final class Backdrop extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF target=new RectF();
    private Bitmap bitmap,previous;private boolean active,motion,powerSave;private long start,powerChecked,fadeAt;private int surface,accent,opacity;
    private String current="";private final SharedPreferences prefs;private final PowerManager power;
    private final java.util.concurrent.ExecutorService loader=java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean loading,released;private int generation;
    private final Runnable tick=()->refreshWallpaper();
    public Backdrop(Context c){super(c);prefs=c.getSharedPreferences("settings",0);power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);setLayerType(View.LAYER_TYPE_HARDWARE,null);}
    public void load(){
        surface=Ui.surface(getContext());accent=Ui.primary(getContext());
        motion=android.animation.ValueAnimator.areAnimatorsEnabled()&&prefs.getBoolean("motion",false)&&!prefs.getBoolean("reduce_motion",false);opacity=prefs.getInt("opacity",86);
        if(Build.VERSION.SDK_INT>=31)setRenderEffect(prefs.getBoolean("blur",true)?RenderEffect.createBlurEffect(18,18,Shader.TileMode.CLAMP):null);
        refreshWallpaper();invalidate();
    }
    private void refreshWallpaper(){
        removeCallbacks(tick);if(active)postDelayed(tick,30000);if(loading||released)return;
        loading=true;int token=generation;String old=current;Context context=getContext().getApplicationContext();
        loader.execute(()->{
            java.io.File file=WallpaperRules.selected(context);String key=file.isFile()?file.getPath()+":"+file.lastModified()+":"+file.length():"";
            Bitmap next=null;if(!key.equals(old)&&file.isFile())next=WallpaperImages.decode(file,1400);
            Bitmap result=next;new Handler(Looper.getMainLooper()).post(()->{loading=false;if(released||token!=generation){if(result!=null)result.recycle();return;}if(!key.equals(current)){
                if(previous!=null)previous.recycle();previous=null;
                if(active&&android.animation.ValueAnimator.areAnimatorsEnabled()&&!prefs.getBoolean("reduce_motion",false)){previous=bitmap;fadeAt=SystemClock.uptimeMillis();}
                else if(bitmap!=null)bitmap.recycle();bitmap=result;current=key;invalidate();if("wallpaper".equals(prefs.getString("widget_background","")))AgendaWidget.refresh(context);
            }});
        });
    }
    public void active(boolean value){active=value;start=SystemClock.uptimeMillis();removeCallbacks(tick);if(value){refreshWallpaper();invalidate();}}
    private void drawImage(Canvas c,Bitmap image,int w,int h,int alpha){if(image==null)return;float scale=Math.max((float)w/image.getWidth(),(float)h/image.getHeight());float bw=image.getWidth()*scale,bh=image.getHeight()*scale;target.set((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2);paint.setShader(null);paint.setAlpha(alpha);c.drawBitmap(image,null,target,paint);paint.setAlpha(255);}
    protected void onDraw(Canvas canvas){super.onDraw(canvas);int w=getWidth(),h=getHeight();canvas.drawColor(surface);
        int alpha=255;if(previous!=null){alpha=(int)Math.min(255,(SystemClock.uptimeMillis()-fadeAt)*255/480);if(alpha<255){drawImage(canvas,previous,w,h,255);if(active)postInvalidateOnAnimation();}else{previous.recycle();previous=null;}}
        drawImage(canvas,bitmap,w,h,alpha);
        boolean enabled=motion;if(bitmap!=null){paint.setColor(Color.argb(opacity*255/100,Color.red(surface),Color.green(surface),Color.blue(surface)));canvas.drawRect(0,0,w,h,paint);}if(!enabled)return;
        long now=SystemClock.uptimeMillis();if(active&&now-powerChecked>1000){powerSave=power.isPowerSaveMode();powerChecked=now;}boolean moving=active&&!powerSave;
        float t=moving?(SystemClock.uptimeMillis()-start)/12000f:0;
        paint.setColor(Color.argb(18,Color.red(accent),Color.green(accent),Color.blue(accent)));canvas.drawCircle(w*(.18f+.12f*(float)Math.sin(t)),h*.25f,w*.7f,paint);canvas.drawCircle(w*.95f,h*(.7f+.12f*(float)Math.cos(t)),w*.8f,paint);
        if(moving)postInvalidateDelayed(33);
    }
    protected void onDetachedFromWindow(){active=false;removeCallbacks(tick);super.onDetachedFromWindow();}
    public void release(){active=false;released=true;generation++;removeCallbacks(tick);loader.shutdown();if(previous!=null){previous.recycle();previous=null;}if(Build.VERSION.SDK_INT>=31)setRenderEffect(null);if(bitmap!=null){bitmap.recycle();bitmap=null;}}
}
