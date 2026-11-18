package app.planaloud;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONObject;

/** Paints the finish above the card surface and below all text and controls. */
final class TexturedCard extends MaterialCardView {
    private Drawable finish;private JSONObject record;
    TexturedCard(Context c){super(c);}
    void finish(JSONObject record){this.record=record;if(finish!=null){finish.setVisible(false,false);finish.setCallback(null);}if(CardFinish.mode(getContext(),record).equals("none")&&CardBackgrounds.selected(getContext(),record).equals("color")&&!PriorityPattern.present(record)){finish=null;invalidate();return;}finish=CardFinish.effect(getContext(),record,Ui.compact(getContext())?16:20);finish.setCallback(this);finish.setBounds(0,0,getWidth(),getHeight());finish.setVisible(isAttachedToWindow()&&getVisibility()==VISIBLE,false);invalidate();}
    protected void dispatchDraw(Canvas canvas){if(record!=null)EventColors.contrast(this,record);if(finish!=null)finish.draw(canvas);super.dispatchDraw(canvas);}
    protected boolean verifyDrawable(Drawable who){return who==finish||super.verifyDrawable(who);}
    protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(finish!=null)finish.setBounds(0,0,w,h);}
    protected void onAttachedToWindow(){super.onAttachedToWindow();if(finish!=null)finish.setVisible(getVisibility()==VISIBLE,false);}
    protected void onDetachedFromWindow(){if(finish!=null)finish.setVisible(false,false);super.onDetachedFromWindow();}
    protected void onVisibilityChanged(android.view.View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(finish!=null)finish.setVisible(visibility==VISIBLE&&isAttachedToWindow(),false);}
}
