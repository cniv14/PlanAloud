package app.planaloud;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import java.util.*;
import org.json.JSONObject;

/** Optional surface effects. No finish is enabled implicitly by choosing a theme. */
final class CardFinish {
    private static final Bitmap GRAIN=grain();
    private static Bitmap grain(){Bitmap b=Bitmap.createBitmap(14,12,Bitmap.Config.ARGB_8888);for(int y=0;y<12;y++)for(int x=0;x<14;x++){int v=(x*17+y*31)%11;b.setPixel(x,y,v<3?0x0fffffff:v<6?0x0b000000:0);}return b;}
    static final String[] VALUES={"none","matte","glass","flow"},LABELS={"无","哑光","玻璃","流光"};
    static String mode(Context c,JSONObject record){String value=record.optString("card_texture",c.getSharedPreferences("settings",0).getString("card_texture","none"));return Arrays.asList(VALUES).contains(value)?value:"none";}
    static Drawable background(Context c,JSONObject r,float radius){return new Surface(c,r,radius,false);}
    static Drawable effect(Context c,JSONObject r,float radius){return new Surface(c,r,radius,true);}
    static void texture(Canvas canvas,RectF area,float radius,String mode,int color,float phase){
        if(mode.equals("none"))return;canvas.save();Path clip=new Path();clip.addRoundRect(area,radius,radius,Path.Direction.CW);canvas.clipPath(clip);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        if(mode.equals("matte")){p.setColor(0x08000000);canvas.drawRect(area,p);p.setShader(new BitmapShader(GRAIN,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT));p.setAlpha(255);canvas.drawRect(area,p);}
        else if(mode.equals("glass")){p.setShader(new LinearGradient(area.left,area.top,area.right,area.bottom,new int[]{0x69ffffff,0x0cffffff,0x24ffffff},null,Shader.TileMode.CLAMP));canvas.drawRect(area,p);p.setShader(null);p.setColor(0x18ffffff);canvas.drawOval(area.left-area.width()*.2f,area.top-area.height()*.7f,area.right,area.top+area.height()*.42f,p);}
        else if(mode.equals("flow")){float beam=area.width()*.65f,center=area.left-area.width()+phase*(area.width()*3);p.setShader(new LinearGradient(center-beam,area.top,center+beam,area.bottom,new int[]{0x00ffffff,0x50ffffff,0x00ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP));canvas.drawRect(area,p);}
        canvas.restore();
    }
    private static final class Surface extends Drawable {
        final Context c;final JSONObject record;final float radius;final boolean effectOnly;final String mode;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);int alpha=255;ColorFilter filter;final Runnable tick=this::invalidateSelf;
        Surface(Context c,JSONObject r,float radius,boolean effect){this.c=c;record=r;this.radius=Ui.dp(c,radius);effectOnly=effect;mode=mode(c,r);}
        public void draw(Canvas canvas){if(effectOnly&&mode.equals("none"))return;RectF area=new RectF(getBounds());if(area.isEmpty())return;int save=alpha==255?canvas.save():canvas.saveLayerAlpha(area,alpha);if(!effectOnly){paint.setStyle(Paint.Style.FILL);paint.setShader(null);paint.setColorFilter(filter);paint.setColor(EventColors.fill(c,record));canvas.drawRoundRect(area,radius,radius,paint);}boolean animate=mode.equals("flow")&&!c.getSharedPreferences("settings",0).getBoolean("reduce_motion",false)&&getCallback()!=null&&isVisible();float phase=animate?(SystemClock.uptimeMillis()%3500)/3500f:.5f;texture(canvas,area,radius,mode,EventColors.fill(c,record),phase);if(!effectOnly&&EventColors.important(record)){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(c,2));paint.setColor(EventColors.border(c,record));RectF edge=new RectF(area);edge.inset(Ui.dp(c,1),Ui.dp(c,1));canvas.drawRoundRect(edge,radius,radius,paint);paint.setStyle(Paint.Style.FILL);}canvas.restoreToCount(save);if(animate)scheduleSelf(tick,SystemClock.uptimeMillis()+50);}
        public boolean setVisible(boolean visible,boolean restart){if(!visible)unscheduleSelf(tick);return super.setVisible(visible,restart);}
        public void setAlpha(int a){alpha=a;invalidateSelf();}public int getAlpha(){return alpha;}public void setColorFilter(ColorFilter f){filter=f;invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
