package app.planaloud;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.time.LocalTime;
import java.util.*;

/** Hand-drawn style diagonal leaders and short name underlines around the clock. */
final class DayTimeline extends View {
    private java.util.List<DayEvents.Segment> events=Collections.emptyList();
    private final TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Deque<Integer> previous=new ArrayDeque<>();
    private final java.util.function.Supplier<LocalTime> clock;
    private boolean expanded;
    private int start;private float downY;private boolean paging,dragging;
    private final Runnable tick=new Runnable(){public void run(){setEvents(DayEvents.load(getContext(),java.time.LocalDate.now()));removeCallbacks(this);if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    DayTimeline(Context c){this(c,LocalTime::now);}
    DayTimeline(Context c,java.util.function.Supplier<LocalTime> clock){super(c);this.clock=java.util.Objects.requireNonNull(clock);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setVerticalScrollBarEnabled(true);setOnClickListener(v->setExpanded(!expanded));setOnLongClickListener(v->{details();return true;});}
    boolean isExpanded(){return expanded;}
    void setExpanded(boolean value){expanded=value;start=0;previous.clear();requestLayout();invalidate();}
    private java.util.List<DayEvents.Segment> annotations(){return expanded?events:DayEvents.at(events,clock.get());}

    private float dp(float value){return Ui.dp(getContext(),value);}
    private float sp(float value){return android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,value,getResources().getDisplayMetrics());}
    private float rowHeight(){paint.setTextSize(sp(12));return Math.max(dp(24),paint.getFontMetrics().bottom-paint.getFontMetrics().top+dp(12));}
    private TimelineCallouts.Layout plan(float width,int index){paint.setTextSize(sp(12));paint.setTypeface(Typeface.DEFAULT);return TimelineCallouts.layout(annotations(),index,width,paint,dp(8),dp(6),dp(132));}
    void setEvents(java.util.List<DayEvents.Segment> next){events=next;start=Math.max(0,Math.min(start,Math.max(0,annotations().size()-1)));previous.clear();requestLayout();invalidate();}
    java.util.List<String> visibleNames(){java.util.List<String> names=new ArrayList<>();for(TimelineCallouts.Label label:plan(getWidth(),start).labels)names.add(label.event.title);return names;}
    private boolean multiple(float width){return start>0||plan(width,0).next<annotations().size();}
    private float clockBottom(){paint.setTextSize(sp(12));return Math.max(dp(20),-paint.ascent()+paint.descent()+dp(6));}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);TimelineCallouts.Layout layout=plan(width,start);float row=rowHeight();int above=multiple(width)?2:layout.above,below=multiple(width)?2:layout.below;float chart=dp(Math.max(4,DayEvents.lanes(events)*2.5f));int wanted=Math.round(Math.max(dp(64),clockBottom()+above*row+dp(6)+chart+below*row+sp(10)+dp(24)));setMeasuredDimension(resolveSize(width,w),resolveSize(wanted,h));}
    private int nameColor(int color){int background=androidx.core.graphics.ColorUtils.setAlphaComponent(Ui.surface(getContext()),255);return androidx.core.graphics.ColorUtils.calculateContrast(color,background)>=4.5?color:androidx.core.graphics.ColorUtils.blendARGB(color,Ui.ink(getContext()),.5f);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);LocalTime now=clock.get();TimelineCallouts.Layout layout=plan(getWidth(),start);float pad=dp(8),width=Math.max(1,getWidth()-2*pad),row=rowHeight(),chart=dp(Math.max(4,DayEvents.lanes(events)*2.5f));int above=multiple(getWidth())?2:layout.above,below=multiple(getWidth())?2:layout.below;float line=clockBottom()+above*row+dp(6);
        setContentDescription(DayTime.label(now)+(expanded?"，已展开全部事件标注，点击折叠":"，已折叠，仅标注当前时间段事件，点击展开")+"，长按查看完整详情"+(multiple(getWidth())?"，上下滑动可切换更多标注":""));
        paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(sp(12));paint.setColor(Ui.muted(getContext()));canvas.drawText(TextUtils.ellipsize(DayTime.label(now),paint,Math.max(1,width-dp(24)),TextUtils.TruncateAt.END).toString(),pad,Math.max(dp(15),-paint.ascent()),paint);float cx=getWidth()-pad-dp(6),cy=clockBottom()/2;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));Path arrow=new Path();arrow.moveTo(cx-dp(2.5f),cy+(expanded?dp(2):-dp(2)));arrow.lineTo(cx,cy+(expanded?-dp(2):dp(2)));arrow.lineTo(cx+dp(2.5f),cy+(expanded?dp(2):-dp(2)));canvas.drawPath(arrow,paint);paint.setStyle(Paint.Style.FILL);
        // Leaders are drawn first; labels never cross each other in the same row.
        for(TimelineCallouts.Label label:layout.labels){
            float y=label.side<0?line-dp(12)-label.row*row:line+chart+dp(12)+label.row*row;
            float elbow=label.anchor<label.left?label.left:label.anchor>label.right?label.right:label.left;
            paint.setColor(nameColor(label.event.color));paint.setAlpha(180);paint.setStrokeWidth(Math.max(1,dp(1)));
            canvas.drawLine(label.anchor,line+label.event.lane*dp(2.5f)+dp(1),elbow,y,paint);canvas.drawLine(label.left,y,label.right,y,paint);
        }
        paint.setAlpha(255);paint.setColor(LiquidGlass.tint(Ui.muted(getContext()),42));canvas.drawRoundRect(pad,line,pad+width,line+dp(2.5f),dp(2),dp(2),paint);
        float marker=pad+width*DayTime.minute(now)/DayTime.MINUTES;paint.setColor(Ui.primary(getContext()));canvas.drawRoundRect(pad,line,marker,line+dp(2.5f),dp(2),dp(2),paint);DayEvents.draw(canvas,paint,events,pad,line,width,dp(2.5f),Ui.ink(getContext()));paint.setColor(Ui.primary(getContext()));paint.setStrokeWidth(dp(2));canvas.drawLine(marker,line-dp(3),marker,line+chart+dp(2),paint);canvas.drawCircle(marker,line-dp(3),dp(3),paint);
        paint.setTextSize(sp(12));for(TimelineCallouts.Label label:layout.labels){float y=label.side<0?line-dp(12)-label.row*row:line+chart+dp(12)+label.row*row;paint.setColor(nameColor(label.event.color));float baseline=label.side<0?y-dp(3):y-paint.ascent()+dp(3);canvas.drawText(label.text,label.left+dp(2),baseline,paint);}
        paint.setColor(Ui.muted(getContext()));paint.setTextSize(sp(10));float axis=line+chart+below*row+dp(17);for(int i=0;i<5;i++){paint.setTextAlign(i==0?Paint.Align.LEFT:i==4?Paint.Align.RIGHT:Paint.Align.CENTER);canvas.drawText(new String[]{"00","06","12","18","24"}[i],pad+width*i/4,axis,paint);}
    }
    private boolean advance(boolean forward){TimelineCallouts.Layout layout=plan(getWidth(),start);if(forward){if(layout.next>=annotations().size())return false;previous.push(start);start=layout.next;}else{if(start==0)return false;start=previous.isEmpty()?Math.max(0,start-Math.max(1,layout.labels.size())):previous.pop();}requestLayout();invalidate();awakenScrollBars();return true;}
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:paging=multiple(getWidth());dragging=false;downY=event.getY();if(paging&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);break;
            case MotionEvent.ACTION_MOVE:if(paging&&Math.abs(event.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){dragging=true;cancelLongPress();return true;}break;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);paging=false;if(dragging){dragging=false;setPressed(false);if(event.getActionMasked()==MotionEvent.ACTION_UP)advance(event.getY()<downY);return true;}break;
            default:break;
        }return super.onTouchEvent(event);
    }
    @Override protected int computeVerticalScrollRange(){return multiple(Math.max(1,getWidth()))?getHeight()+events.size()*Ui.dp(getContext(),12):getHeight();}
    @Override protected int computeVerticalScrollOffset(){return start*Ui.dp(getContext(),12);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);TimelineCallouts.Layout layout=plan(getWidth(),start);info.addAction(expanded?AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE:AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);if(start>0){info.setScrollable(true);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}if(layout.next<annotations().size()){info.setScrollable(true);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==AccessibilityNodeInfo.ACTION_EXPAND){setExpanded(true);return true;}if(action==AccessibilityNodeInfo.ACTION_COLLAPSE){setExpanded(false);return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return advance(true);if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)return advance(false);return super.performAccessibilityAction(action,args);}
    private void details(){StringBuilder lines=new StringBuilder();for(DayEvents.Segment event:events)lines.append("● ").append(event.label()).append("\n");SpannableString text=new SpannableString(lines.toString());int at=0;for(DayEvents.Segment event:events){text.setSpan(new android.text.style.ForegroundColorSpan(event.color),at,at+1,0);at+=event.label().length()+3;}new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext()).setTitle("今日事件时间线").setMessage(events.isEmpty()?"今天还没有定时事件":text).setPositiveButton("知道了",null).show();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();tick.run();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&isAttachedToWindow())tick.run();else removeCallbacks(tick);}
}
