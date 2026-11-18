package app.planaloud;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.time.LocalTime;

/** Compact clock with event-name callouts. Only the name column scrolls when it is full. */
final class DayTimeline extends View {
    private java.util.List<DayEvents.Segment> events=java.util.Collections.emptyList();
    private final TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private float labelOffset,downY,initialOffset;private boolean touchingNames,dragging;
    private final Runnable tick=new Runnable(){public void run(){setEvents(DayEvents.load(getContext(),java.time.LocalDate.now()));removeCallbacks(this);if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    DayTimeline(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setVerticalScrollBarEnabled(true);setOnClickListener(v->details());}
    private float dp(float value){return Ui.dp(getContext(),value);}
    private float sp(float value){return android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,value,getResources().getDisplayMetrics());}
    private float rowHeight(){paint.setTextSize(sp(12));return Math.max(dp(18),paint.getFontMetrics().bottom-paint.getFontMetrics().top+dp(6));}
    private float nameLeft(){return Math.max(dp(92),getWidth()-dp(8)-Math.min(dp(140),getWidth()*.43f));}
    private float nameHeight(){return Math.max(1,getHeight()-dp(32));}
    private float maxOffset(){return Math.max(0,events.size()*rowHeight()-nameHeight());}
    void setEvents(java.util.List<DayEvents.Segment> next){boolean sizeChanged=next.size()!=events.size();events=next;labelOffset=Math.max(0,Math.min(labelOffset,maxOffset()));if(sizeChanged)requestLayout();invalidate();}
    java.util.List<String> visibleNames(){java.util.List<String> names=new java.util.ArrayList<>();float row=rowHeight();for(int i=0;i<events.size();i++)if(i*row>=labelOffset&&i*row+row<=labelOffset+nameHeight())names.add(events.get(i).title);return names;}
    @Override protected void onMeasure(int w,int h){int wanted=Math.round(Math.max(dp(86),dp(32)+Math.min(4,events.size())*rowHeight()));setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(),w),resolveSize(wanted,h));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);LocalTime now=LocalTime.now();setContentDescription(DayTime.label(now)+"，"+events.size()+"个事件，右侧为事件名称，可上下滑动，点击查看完整内容");
        float pad=dp(8),label=nameLeft(),width=Math.max(0,label-pad-dp(16)),line=dp(32),row=rowHeight();
        paint.setTextSize(sp(12));paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);paint.setColor(Ui.muted(getContext()));canvas.drawText(DayTime.label(now),pad,Math.max(dp(15),-paint.ascent()),paint);
        // Draw all leaders behind the chart so they cannot obscure the colored intervals.
        for(int i=0;i<events.size();i++){
            float center=dp(28)+(i+.5f)*row-labelOffset;if(center-row/2<dp(28)||center+row/2>getHeight()-dp(4))continue;DayEvents.Segment event=events.get(i);
            paint.setColor(LiquidGlass.tint(event.color,135));paint.setStrokeWidth(Math.max(1,dp(1)));
            float anchor=pad+width*(event.start+event.end)/2880f,anchorY=line+event.lane*dp(4)+dp(1);
            canvas.drawLine(anchor,anchorY,label-dp(6),center,paint);canvas.drawLine(label-dp(6),center,label-dp(2),center,paint);
        }
        paint.setColor(LiquidGlass.tint(Ui.muted(getContext()),42));canvas.drawRoundRect(pad,line,pad+width,line+dp(4),dp(2),dp(2),paint);
        float marker=pad+width*DayTime.minute(now)/DayTime.MINUTES;paint.setColor(Ui.primary(getContext()));canvas.drawRoundRect(pad,line,marker,line+dp(4),dp(2),dp(2),paint);
        DayEvents.draw(canvas,paint,events,pad,line,width,dp(4));paint.setColor(Ui.primary(getContext()));paint.setStrokeWidth(dp(2));canvas.drawLine(marker,line-dp(3),marker,line+dp(Math.max(8,DayEvents.lanes(events)*4)),paint);canvas.drawCircle(marker,line-dp(3),dp(3),paint);
        paint.setColor(Ui.muted(getContext()));paint.setTextSize(sp(10));float axis=line-dp(6);for(int i=0;i<5;i++){paint.setTextAlign(i==0?Paint.Align.LEFT:i==4?Paint.Align.RIGHT:Paint.Align.CENTER);canvas.drawText(new String[]{"00","06","12","18","24"}[i],pad+width*i/4,axis,paint);}
        paint.setTextSize(sp(12));paint.setTextAlign(Paint.Align.LEFT);float textWidth=Math.max(1,getWidth()-label-pad);
        for(int i=0;i<events.size();i++){
            float center=dp(28)+(i+.5f)*row-labelOffset;if(center-row/2<dp(28)||center+row/2>getHeight()-dp(4))continue;DayEvents.Segment event=events.get(i);paint.setColor(nameColor(event.color));
            CharSequence title=TextUtils.ellipsize(event.title.replace('\n',' '),paint,textWidth,TextUtils.TruncateAt.END);canvas.drawText(title.toString(),label,center-(paint.ascent()+paint.descent())/2,paint);
        }
    }
    private int nameColor(int color){int background=androidx.core.graphics.ColorUtils.setAlphaComponent(Ui.surface(getContext()),255);if(androidx.core.graphics.ColorUtils.calculateContrast(color,background)>=4.5)return color;return androidx.core.graphics.ColorUtils.blendARGB(color,Ui.ink(getContext()),.5f);}
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:touchingNames=event.getX()>=nameLeft()&&maxOffset()>0;dragging=false;downY=event.getY();initialOffset=labelOffset;if(touchingNames&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);break;
            case MotionEvent.ACTION_MOVE:if(touchingNames){if(Math.abs(event.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())dragging=true;if(dragging){labelOffset=Math.max(0,Math.min(maxOffset(),initialOffset+downY-event.getY()));invalidate();awakenScrollBars();return true;}}break;
            case MotionEvent.ACTION_CANCEL:case MotionEvent.ACTION_UP:if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);touchingNames=false;if(dragging){dragging=false;setPressed(false);return true;}break;
            default:break;
        }return super.onTouchEvent(event);
    }
    @Override protected int computeVerticalScrollRange(){return Math.round(getHeight()+maxOffset());}
    @Override protected int computeVerticalScrollOffset(){return Math.round(labelOffset);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(maxOffset()>0){info.setScrollable(true);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}}
    @Override public boolean performAccessibilityAction(int action,Bundle arguments){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){labelOffset=Math.max(0,Math.min(maxOffset(),labelOffset+(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1)*nameHeight()));invalidate();return true;}return super.performAccessibilityAction(action,arguments);}
    private void details(){StringBuilder lines=new StringBuilder();for(DayEvents.Segment event:events)lines.append("● ").append(event.label()).append("\n");SpannableString text=new SpannableString(lines.toString());int at=0;for(DayEvents.Segment event:events){text.setSpan(new android.text.style.ForegroundColorSpan(event.color),at,at+1,0);at+=event.label().length()+3;}new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext()).setTitle("今日事件时间线").setMessage(events.isEmpty()?"今天还没有定时事件":text).setPositiveButton("知道了",null).show();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();tick.run();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&isAttachedToWindow())tick.run();else removeCallbacks(tick);}
}
