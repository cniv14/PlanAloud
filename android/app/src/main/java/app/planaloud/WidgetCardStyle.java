package app.planaloud;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
/** Same renderer and loop duration as app cards, played by the launcher's ViewFlipper. */
final class WidgetCardStyle {
    private static final int FRAMES=24;
    static void apply(Context c,RemoteViews row,JSONObject record,boolean twoDay){
        int image=twoDay?R.id.two_day_item_background:R.id.widget_item_background,animation=twoDay?R.id.two_day_item_animation:R.id.widget_item_animation;
        boolean animated=(CardFinish.mode(c,record).equals("flow")||CardBackgrounds.animated(c,record))&&!CardBackgrounds.prefs(c).getBoolean("reduce_motion",false);
        row.removeAllViews(animation);row.setViewVisibility(animation,animated?View.VISIBLE:View.GONE);row.setViewVisibility(image,animated?View.GONE:View.VISIBLE);
        if(!animated){WidgetImages.set(row,image,EventColors.widget(c,record,twoDay));return;}
        int first=(int)((SystemClock.uptimeMillis()%CardFinish.CYCLE_MS)*FRAMES/CardFinish.CYCLE_MS);
        for(int i=0;i<FRAMES;i++){RemoteViews frame=new RemoteViews(c.getPackageName(),R.layout.widget_animation_frame);WidgetImages.set(frame,R.id.widget_animation_frame,EventColors.widget(c,record,((first+i)%FRAMES)/(float)FRAMES,96,36));row.addView(animation,frame);}
        row.setInt(animation,"setFlipInterval",Math.round(CardFinish.CYCLE_MS/(float)FRAMES));row.setInt(animation,"setDisplayedChild",0);
    }
}
