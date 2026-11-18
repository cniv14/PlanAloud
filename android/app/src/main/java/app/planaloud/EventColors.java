package app.planaloud;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import org.json.JSONObject;

/** Event identity controls the fill; explicit importance controls only the outside border. */
final class EventColors {
    static boolean important(JSONObject r){return !r.optString("priority").isEmpty();}
    static int border(Context c,JSONObject r){return border(r,0);}
    static int border(JSONObject r,int neutral){switch(r.optString("priority")){case "urgent":return 0xffff5722;case "high":return 0xffab47e8;case "normal":return 0xff268bf2;case "low":return 0xff27ae60;default:return 0;}}
    static int lightFill(JSONObject r){int base=DayEvents.color(r);return r.optString("color").matches("#[0-9a-fA-F]{6}")?androidx.core.graphics.ColorUtils.blendARGB(base,Color.WHITE,.58f):base;}
    static int fill(Context c,JSONObject r){int base=lightFill(r);int fill=AgendaWidget.dark(c)?androidx.core.graphics.ColorUtils.blendARGB(base,Ui.surface(c),.66f):base;return CardFinish.mode(c,r).equals("glass")?LiquidGlass.tint(fill,205):fill;}
    static android.graphics.drawable.Drawable shape(Context c,JSONObject r,float radius){return CardFinish.background(c,r,radius);}
    static Bitmap widget(Context c,JSONObject r,boolean twoDay){boolean dark=twoDay||AgendaWidget.dark(c);Bitmap image=Bitmap.createBitmap(300,110,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int base=DayEvents.color(r);if(r.optString("color").matches("#[0-9a-fA-F]{6}"))base=androidx.core.graphics.ColorUtils.blendARGB(base,Color.WHITE,.58f);p.setColor(dark?androidx.core.graphics.ColorUtils.blendARGB(base,0xff1b1b30,.66f):base);RectF area=new RectF(1,1,299,109);canvas.drawRoundRect(area,12,12,p);CardFinish.texture(canvas,area,12,CardFinish.mode(c,r),base,.5f);if(important(r)){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(border(c,r));canvas.drawRoundRect(2,2,298,108,12,12,p);}return image;}
}
