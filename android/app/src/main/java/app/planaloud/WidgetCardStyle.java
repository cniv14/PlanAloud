package app.planaloud;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
/** Single-day widget textures; the native row outline clips static and animated fills equally. */
final class WidgetCardStyle {
    private static final int FRAMES=24;
    static void apply(Context c,RemoteViews row,JSONObject record){
        int image=R.id.widget_item_background,animation=R.id.widget_item_animation;
        boolean animated=(CardFinish.mode(c,record).equals("flow")||CardBackgrounds.animated(c,record))&&!CardBackgrounds.prefs(c).getBoolean("reduce_motion",false);
        row.removeAllViews(animation);row.setViewVisibility(animation,animated?View.VISIBLE:View.GONE);row.setViewVisibility(image,animated?View.GONE:View.VISIBLE);
        if(!animated){WidgetImages.set(row,image,EventColors.widget(c,record,.5f,300,110));return;}
        RemoteViews player=new RemoteViews(c.getPackageName(),R.layout.widget_card_animation);
        int first=(int)((SystemClock.uptimeMillis()%CardFinish.CYCLE_MS)*FRAMES/CardFinish.CYCLE_MS);
        for(int i=0;i<FRAMES;i++){RemoteViews frame=new RemoteViews(c.getPackageName(),R.layout.widget_animation_frame);WidgetImages.set(frame,R.id.widget_animation_frame,EventColors.widget(c,record,((first+i)%FRAMES)/(float)FRAMES,300,110));player.addView(R.id.widget_card_animation,frame);}
        player.setInt(R.id.widget_card_animation,"setFlipInterval",Math.round(CardFinish.CYCLE_MS/(float)FRAMES));player.setInt(R.id.widget_card_animation,"setDisplayedChild",0);row.addView(animation,player);
    }
}
