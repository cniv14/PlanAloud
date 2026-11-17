package app.planaloud;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.OverScroller;

/** One pan surface for a wide/tall weekly table; a drag can move along both axes. */
final class TwoAxisScroll extends FrameLayout {
    private final OverScroller fling;
    private final int slop;
    private VelocityTracker velocity;
    private float downX,downY,lastX,lastY;
    private boolean dragging;
    private java.util.function.BiConsumer<Integer,Integer> position;
    void onPosition(java.util.function.BiConsumer<Integer,Integer> listener){position=listener;}
    TwoAxisScroll(Context c){super(c);fling=new OverScroller(c);slop=ViewConfiguration.get(c).getScaledTouchSlop();setClipChildren(true);setHorizontalScrollBarEnabled(true);setVerticalScrollBarEnabled(true);setFocusable(true);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);
        if(getChildCount()>0){View child=getChildAt(0);int cw=child.getLayoutParams().width,ch=child.getLayoutParams().height;child.measure(MeasureSpec.makeMeasureSpec(Math.max(0,cw),cw>=0?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED),MeasureSpec.makeMeasureSpec(Math.max(0,ch),ch>=0?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED));}
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){if(getChildCount()>0){View child=getChildAt(0);child.layout(0,0,child.getMeasuredWidth(),child.getMeasuredHeight());}scrollTo(getScrollX(),getScrollY());}
    private int maxX(){return getChildCount()==0?0:Math.max(0,getChildAt(0).getMeasuredWidth()-getWidth());}
    private int maxY(){return getChildCount()==0?0:Math.max(0,getChildAt(0).getMeasuredHeight()-getHeight());}
    @Override public void scrollTo(int x,int y){int oldX=getScrollX(),oldY=getScrollY();super.scrollTo(Math.max(0,Math.min(maxX(),x)),Math.max(0,Math.min(maxY(),y)));awakenScrollBars();if(position!=null&&(oldX!=getScrollX()||oldY!=getScrollY()))position.accept(getScrollX(),getScrollY());}
    @Override public boolean canScrollHorizontally(int direction){return direction<0?getScrollX()>0:getScrollX()<maxX();}
    @Override public boolean canScrollVertically(int direction){return direction<0?getScrollY()>0:getScrollY()<maxY();}
    @Override protected int computeHorizontalScrollRange(){return getWidth()+maxX();}
    @Override protected int computeVerticalScrollRange(){return getHeight()+maxY();}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=lastX=event.getX();downY=lastY=event.getY();dragging=!fling.isFinished();fling.abortAnimation();getParent().requestDisallowInterceptTouchEvent(true);return dragging;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop)){dragging=true;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_CANCEL||event.getActionMasked()==MotionEvent.ACTION_UP)dragging=false;
        return dragging;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(velocity==null)velocity=VelocityTracker.obtain();velocity.addMovement(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:lastX=event.getX();lastY=event.getY();fling.abortAnimation();getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:
                scrollBy(Math.round(lastX-event.getX()),Math.round(lastY-event.getY()));lastX=event.getX();lastY=event.getY();return true;
            case MotionEvent.ACTION_UP:
                velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());
                fling.fling(getScrollX(),getScrollY(),-(int)velocity.getXVelocity(),-(int)velocity.getYVelocity(),0,maxX(),0,maxY());postInvalidateOnAnimation();
                velocity.recycle();velocity=null;dragging=false;getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;
            case MotionEvent.ACTION_CANCEL:
                velocity.recycle();velocity=null;dragging=false;getParent().requestDisallowInterceptTouchEvent(false);return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void computeScroll(){if(fling.computeScrollOffset()){scrollTo(fling.getCurrX(),fling.getCurrY());postInvalidateOnAnimation();}}
}
