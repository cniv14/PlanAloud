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
    private final java.util.function.Supplier<LocalTime> clock;
    private boolean expanded;
    private TimelineCallouts.Layout cachedPlan;private float cachedWidth,cachedFont;private int cachedMinute;
    private final Runnable tick=new Runnable(){public void run(){setEvents(DayEvents.load(getContext(),java.time.LocalDate.now()));removeCallbacks(this);if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    DayTimeline(Context c){this(c,LocalTime::now);}
    DayTimeline(Context c,java.util.function.Supplier<LocalTime> clock){super(c);this.clock=java.util.Objects.requireNonNull(clock);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setVerticalScrollBarEnabled(false);setOnClickListener(v->setExpanded(!expanded));setOnLongClickListener(v->{details();return true;});}
    boolean isExpanded(){return expanded;}
    void setExpanded(boolean value){expanded=value;cachedPlan=null;requestLayout();invalidate();}
    private java.util.List<DayEvents.Segment> annotations(){return expanded?events:DayEvents.at(events,clock.get());}

    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    private float sp(float value){return android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,value,getResources().getDisplayMetrics());}
    private float rowHeight(){paint.setTextSize(sp(12));return Math.max(dp(24),paint.getFontMetrics().bottom-paint.getFontMetrics().top+dp(12));}
    private TimelineCallouts.Layout plan(float width,int index){paint.setTextSize(sp(12));paint.setTypeface(Typeface.DEFAULT);int minute=DayTime.minute(clock.get());if(cachedPlan!=null&&cachedWidth==width&&cachedFont==sp(12)&&cachedMinute==minute)return cachedPlan;cachedWidth=width;cachedFont=sp(12);cachedMinute=minute;cachedPlan=TimelineCallouts.layout(annotations(),0,width,paint,dp(8),dp(6),dp(132));return cachedPlan;}
    void setEvents(java.util.List<DayEvents.Segment> next){events=next;cachedPlan=null;requestLayout();invalidate();}
    java.util.List<String> visibleNames(){java.util.List<String> names=new ArrayList<>();for(TimelineCallouts.Label label:plan(getWidth(),0).labels)names.add(label.event.title);return names;}
    private float clockBottom(){paint.setTextSize(sp(12));return Math.max(dp(20),-paint.ascent()+paint.descent()+dp(6));}
    @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);TimelineCallouts.Layout layout=plan(width,0);float row=rowHeight();int above=layout.above,below=layout.below;float chart=DayEvents.tracks(events,dp(4.5f),dp(.5f)).height;int wanted=Math.round(Math.max(dp(64),clockBottom()+above*row+dp(6)+chart+below*row+sp(10)+dp(24)));setMeasuredDimension(resolveSize(width,w),resolveSize(wanted,h));}
    private int nameColor(int color){int background=androidx.core.graphics.ColorUtils.setAlphaComponent(Ui.surface(getContext()),255);return androidx.core.graphics.ColorUtils.calculateContrast(color,background)>=4.5?color:Ui.ink(getContext());}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);LocalTime now=clock.get();TimelineCallouts.Layout layout=plan(getWidth(),0);float pad=dp(8),width=Math.max(1,getWidth()-2*pad),row=rowHeight(),chart=DayEvents.tracks(events,dp(4.5f),dp(.5f)).height;int above=layout.above,below=layout.below;float line=clockBottom()+above*row+dp(6);DayEvents.Tracks tracks=DayEvents.tracks(events,dp(4.5f),dp(.5f));float mainTop=line+tracks.top;
        setContentDescription(DayTime.label(now)+(expanded?"，已展开全部事件标注，点击折叠":"，已折叠，仅标注当前时间段事件，点击展开")+"，长按查看完整详情"+"，展开后所有事件名同屏，上下各最多两行");
        paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(sp(12));paint.setColor(Ui.muted(getContext()));canvas.drawText(TextUtils.ellipsize(DayTime.label(now),paint,Math.max(1,width-dp(24)),TextUtils.TruncateAt.END).toString(),pad,Math.max(dp(15),-paint.ascent()),paint);float cx=getWidth()-pad-dp(6),cy=clockBottom()/2;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));Path arrow=new Path();arrow.moveTo(cx-dp(2.5f),cy+(expanded?dp(2):-dp(2)));arrow.lineTo(cx,cy+(expanded?-dp(2):dp(2)));arrow.lineTo(cx+dp(2.5f),cy+(expanded?dp(2):-dp(2)));canvas.drawPath(arrow,paint);paint.setStyle(Paint.Style.FILL);
        // Leaders are drawn first; labels never cross each other in the same row.
        for(TimelineCallouts.Label label:layout.labels){
            float y=label.side<0?line-dp(12)-label.row*row:line+chart+dp(12)+label.row*row;
            float elbow=label.elbow,base=label.side<0?line:line+chart;
            paint.setColor(nameColor(label.event.color));paint.setAlpha(180);paint.setStrokeWidth(Math.max(1,dp(1)));
            canvas.drawLine(label.anchor,line+tracks.y(label.event)+tracks.thickness(label.event)/2,label.anchor,base,paint);canvas.drawLine(label.anchor,base,elbow,y,paint);canvas.drawLine(label.left,y,label.right,y,paint);
        }
        paint.setAlpha(255);paint.setColor(LiquidGlass.tint(Ui.muted(getContext()),42));canvas.drawRoundRect(pad,mainTop,pad+width,mainTop+tracks.main,tracks.main/2,tracks.main/2,paint);
        float marker=pad+width*DayTime.minute(now)/DayTime.MINUTES;paint.setColor(Ui.primary(getContext()));canvas.drawRoundRect(pad,mainTop,marker,mainTop+tracks.main,tracks.main/2,tracks.main/2,paint);DayEvents.draw(canvas,paint,events,pad,line,width,tracks,Ui.ink(getContext()));paint.setColor(Ui.primary(getContext()));paint.setStrokeWidth(dp(2));canvas.drawLine(marker,line-dp(8),marker,line+chart+dp(8),paint);canvas.drawCircle(marker,line-dp(8),dp(2.5f),paint);
        paint.setTextSize(sp(12));for(TimelineCallouts.Label label:layout.labels){float y=label.side<0?line-dp(12)-label.row*row:line+chart+dp(12)+label.row*row;paint.setColor(nameColor(label.event.color));float baseline=label.side<0?y-dp(3):y-paint.ascent()+dp(3);canvas.save();canvas.clipRect(label.left,baseline+paint.getFontMetrics().top-dp(1),label.right,baseline+paint.getFontMetrics().bottom+dp(1));canvas.drawText(label.text,label.left+dp(2),baseline,paint);canvas.restore();}
        paint.setColor(Ui.muted(getContext()));paint.setTextSize(sp(10));float axis=line+chart+below*row+dp(17);for(int i=0;i<5;i++){paint.setTextAlign(i==0?Paint.Align.LEFT:i==4?Paint.Align.RIGHT:Paint.Align.CENTER);canvas.drawText(new String[]{"00","06","12","18","24"}[i],pad+width*i/4,axis,paint);}
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.addAction(expanded?AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE:AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==AccessibilityNodeInfo.ACTION_EXPAND){setExpanded(true);return true;}if(action==AccessibilityNodeInfo.ACTION_COLLAPSE){setExpanded(false);return true;}return super.performAccessibilityAction(action,args);}
    private void details(){StringBuilder lines=new StringBuilder();for(DayEvents.Segment event:events)lines.append("● ").append(event.label()).append("\n");SpannableString text=new SpannableString(lines.toString());int at=0;for(DayEvents.Segment event:events){text.setSpan(new android.text.style.ForegroundColorSpan(event.color),at,at+1,0);at+=event.label().length()+3;}new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext()).setTitle("今日事件时间线").setMessage(events.isEmpty()?"今天还没有定时事件":text).setPositiveButton("知道了",null).show();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();tick.run();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&isAttachedToWindow())tick.run();else removeCallbacks(tick);}
}
