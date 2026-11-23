package app.planaloud;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import org.json.JSONObject;

/** Event fills are independent of night mode; text follows the actual fill contrast. */
final class EventColors {
    static boolean important(JSONObject r){return !r.optString("priority").isEmpty();}
    static int border(Context c,JSONObject r){return 0;}
    static int border(JSONObject r,int neutral){return 0;}
    static int lightFill(JSONObject r){return DayEvents.color(r);}
    static int fill(Context c,JSONObject r){int fill=lightFill(r);return CardFinish.mode(c,r).equals("glass")?LiquidGlass.tint(fill,205):fill;}
    static int ink(JSONObject r){return androidx.core.graphics.ColorUtils.calculateLuminance(lightFill(r))>.40?0xff202632:Color.WHITE;}
    static int ink(Context c,JSONObject r){String style=CardBackgrounds.selected(c,r);return style.equals("color")||style.equals("motion:ripple")||style.equals("motion:stars")||style.equals("motion:meteors")?ink(r):0xff202632;}
    static void contrast(android.view.View view,JSONObject r){contrastColor(view,ink(view.getContext(),r));}
    private static void contrastColor(android.view.View view,int color){if(view instanceof android.widget.TextView text&&text.getCurrentTextColor()!=color)text.setTextColor(color);if(view instanceof android.widget.CompoundButton button){android.content.res.ColorStateList tint=button.getButtonTintList();if(tint==null||tint.getDefaultColor()!=color)button.setButtonTintList(android.content.res.ColorStateList.valueOf(color));}if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)contrastColor(group.getChildAt(i),color);}
    static android.graphics.drawable.Drawable shape(Context c,JSONObject r,float radius){return CardFinish.background(c,r,radius);}
    static Bitmap widget(Context c,JSONObject r,float phase,int width,int height){
        Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);image.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas=new Canvas(image);canvas.scale(width/300f,height/110f);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(fill(c,r));
        RectF area=new RectF(0,0,300,110);
        // XML clipToOutline is available from API 31; older launcher rows need a rounded bitmap.
        float radius=android.os.Build.VERSION.SDK_INT>=31?0:12;
        canvas.drawRoundRect(area,radius,radius,paint);
        CardBackgrounds.paint(c,canvas,area,radius,r,phase,true,null,fill(c,r));
        CardFinish.texture(canvas,area,radius,CardFinish.mode(c,r),lightFill(r),phase);
        return image;
    }
}
