package app.planaloud;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.OverScroller;

/** One pan surface with focal-point-preserving pinch zoom and transformed child hit testing. */
final class TwoAxisScroll extends FrameLayout {
    private final OverScroller fling;private final ScaleGestureDetector detector;private final int slop;
    private VelocityTracker velocity;private float downX,downY,lastX,lastY,zoom=1;
    private boolean dragging,zoomEnabled,scalingSequence;
    private java.util.function.BiConsumer<Integer,Integer> position;
    private java.util.function.Consumer<Float> zoomChanged;private Runnable zoomFinished,gestureStart;
    void onPosition(java.util.function.BiConsumer<Integer,Integer> listener){position=listener;}
    void onGestureStart(Runnable listener){gestureStart=listener;}
    void stopFling(){fling.abortAnimation();}
    float zoom(){return zoom;}
    void zoomable(float initial,java.util.function.Consumer<Float> changed,Runnable finished){zoomEnabled=true;zoomChanged=changed;zoomFinished=finished;setZoom(initial,0,0);}
    TwoAxisScroll(Context c){
        super(c);fling=new OverScroller(c);slop=ViewConfiguration.get(c).getScaledTouchSlop();setClipChildren(true);setHorizontalScrollBarEnabled(true);setVerticalScrollBarEnabled(true);setFocusable(true);
        detector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector d){stopFling();scalingSequence=true;disallow(true);return true;}
            @Override public boolean onScale(ScaleGestureDetector d){setZoom(zoom*d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}
            @Override public void onScaleEnd(ScaleGestureDetector d){if(zoomFinished!=null)zoomFinished.run();}
        });
    }
    void setZoom(float value,float focusX,float focusY){
        if(!Float.isFinite(value))return;float next=Math.max(.3f,Math.min(3f,value));if(Math.abs(next-zoom)<.0001f)return;
        float x=(getScrollX()+focusX)/zoom,y=(getScrollY()+focusY)/zoom;zoom=next;
        if(getChildCount()>0){View child=getChildAt(0);child.setPivotX(0);child.setPivotY(0);child.setScaleX(zoom);child.setScaleY(zoom);}
        if(zoomChanged!=null)zoomChanged.accept(zoom);scrollTo(Math.round(x*zoom-focusX),Math.round(y*zoom-focusY));invalidate();
    }
    private void disallow(boolean value){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(value);}
    private void clearVelocity(){if(velocity!=null){velocity.recycle();velocity=null;}}
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN)scalingSequence=false;if(zoomEnabled){if(event.getPointerCount()>1)scalingSequence=true;detector.onTouchEvent(event);}return super.dispatchTouchEvent(event);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);if(getChildCount()>0){View child=getChildAt(0);int cw=child.getLayoutParams().width,ch=child.getLayoutParams().height;child.measure(MeasureSpec.makeMeasureSpec(Math.max(0,cw),cw>=0?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED),MeasureSpec.makeMeasureSpec(Math.max(0,ch),ch>=0?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED));}setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){if(getChildCount()>0){View child=getChildAt(0);child.layout(0,0,child.getMeasuredWidth(),child.getMeasuredHeight());child.setPivotX(0);child.setPivotY(0);child.setScaleX(zoom);child.setScaleY(zoom);}scrollTo(getScrollX(),getScrollY());}
    private int maxX(){return getChildCount()==0?0:Math.max(0,Math.round(getChildAt(0).getMeasuredWidth()*zoom)-getWidth());}
    private int maxY(){return getChildCount()==0?0:Math.max(0,Math.round(getChildAt(0).getMeasuredHeight()*zoom)-getHeight());}
    @Override public void scrollTo(int x,int y){int oldX=getScrollX(),oldY=getScrollY();super.scrollTo(Math.max(0,Math.min(maxX(),x)),Math.max(0,Math.min(maxY(),y)));awakenScrollBars();if(position!=null&&(oldX!=getScrollX()||oldY!=getScrollY()))position.accept(getScrollX(),getScrollY());}
    @Override public boolean canScrollHorizontally(int direction){return direction<0?getScrollX()>0:getScrollX()<maxX();}
    @Override public boolean canScrollVertically(int direction){return direction<0?getScrollY()>0:getScrollY()<maxY();}
    @Override protected int computeHorizontalScrollRange(){return getWidth()+maxX();}
    @Override protected int computeVerticalScrollRange(){return getHeight()+maxY();}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){if(gestureStart!=null)gestureStart.run();downX=lastX=event.getX();downY=lastY=event.getY();dragging=!fling.isFinished();stopFling();disallow(true);return dragging;}
        if(zoomEnabled&&scalingSequence){dragging=true;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop)){dragging=true;return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_CANCEL||event.getActionMasked()==MotionEvent.ACTION_UP)dragging=false;return dragging;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(zoomEnabled&&scalingSequence){if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){scalingSequence=false;dragging=false;clearVelocity();disallow(false);}return true;}
        if(velocity==null)velocity=VelocityTracker.obtain();velocity.addMovement(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:lastX=event.getX();lastY=event.getY();stopFling();disallow(true);return true;
            case MotionEvent.ACTION_MOVE:scrollBy(Math.round(lastX-event.getX()),Math.round(lastY-event.getY()));lastX=event.getX();lastY=event.getY();return true;
            case MotionEvent.ACTION_UP:velocity.computeCurrentVelocity(1000,ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity());fling.fling(getScrollX(),getScrollY(),-(int)velocity.getXVelocity(),-(int)velocity.getYVelocity(),0,maxX(),0,maxY());postInvalidateOnAnimation();clearVelocity();dragging=false;disallow(false);performClick();return true;
            case MotionEvent.ACTION_CANCEL:clearVelocity();dragging=false;disallow(false);return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void computeScroll(){if(fling.computeScrollOffset()){scrollTo(fling.getCurrX(),fling.getCurrY());postInvalidateOnAnimation();}}
    @Override protected void onDetachedFromWindow(){stopFling();clearVelocity();super.onDetachedFromWindow();}
}
