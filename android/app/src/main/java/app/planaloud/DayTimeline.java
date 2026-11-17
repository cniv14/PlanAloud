package app.planaloud;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.LocalTime;

/** Minute-updated local day clock. The view owns no background service or continuous animation. */
final class DayTimeline extends View {
    private java.util.List<DayEvents.Segment> events=java.util.Collections.emptyList();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable tick=new Runnable(){public void run(){events=DayEvents.load(getContext(),java.time.LocalDate.now());invalidate();removeCallbacks(this);if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postDelayed(this,60000-System.currentTimeMillis()%60000);}};
    DayTimeline(Context c){super(c);setContentDescription(DayTime.label(LocalTime.now()));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setOnClickListener(v->{StringBuilder lines=new StringBuilder();for(DayEvents.Segment event:events)lines.append("● ").append(event.label()).append("\n");android.text.SpannableString text=new android.text.SpannableString(lines.toString());int at=0;for(DayEvents.Segment event:events){text.setSpan(new android.text.style.ForegroundColorSpan(event.color),at,at+1,0);at+=event.label().length()+3;}new com.google.android.material.dialog.MaterialAlertDialogBuilder(c).setTitle("今日事件时间线").setMessage(events.isEmpty()?"今天还没有定时事件":text).setPositiveButton("知道了",null).show();});}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);LocalTime now=LocalTime.now();setContentDescription(DayTime.label(now)+"，"+events.size()+"个事件，点击查看颜色对应的事件");float pad=Ui.dp(getContext(),8),width=Math.max(0,getWidth()-2*pad),line=Ui.dp(getContext(),31);
        paint.setTextSize(Ui.dp(getContext(),12));paint.setTypeface(Typeface.DEFAULT);paint.setColor(Ui.muted(getContext()));paint.setTextAlign(Paint.Align.LEFT);canvas.drawText(DayTime.label(now),pad,Ui.dp(getContext(),15),paint);
        paint.setColor(LiquidGlass.tint(Ui.muted(getContext()),42));canvas.drawRoundRect(pad,line,pad+width,line+Ui.dp(getContext(),4),Ui.dp(getContext(),2),Ui.dp(getContext(),2),paint);
        float marker=pad+width*DayTime.minute(now)/DayTime.MINUTES;paint.setColor(Ui.primary(getContext()));canvas.drawRoundRect(pad,line,marker,line+Ui.dp(getContext(),4),Ui.dp(getContext(),2),Ui.dp(getContext(),2),paint);
        DayEvents.draw(canvas,paint,events,pad,line,width,Ui.dp(getContext(),4));paint.setColor(Ui.primary(getContext()));paint.setStrokeWidth(Ui.dp(getContext(),2));canvas.drawLine(marker,line-Ui.dp(getContext(),3),marker,line+Ui.dp(getContext(),Math.max(8,DayEvents.lanes(events)*4)),paint);canvas.drawCircle(marker,line-Ui.dp(getContext(),3),Ui.dp(getContext(),3),paint);
        paint.setColor(Ui.muted(getContext()));paint.setTextSize(Ui.dp(getContext(),10));for(int i=0;i<5;i++){paint.setTextAlign(i==0?Paint.Align.LEFT:i==4?Paint.Align.RIGHT:Paint.Align.CENTER);canvas.drawText(new String[]{"00:00","06:00","12:00","18:00","24:00"}[i],pad+width*i/4,line+Ui.dp(getContext(),44),paint);}
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();tick.run();}
    @Override protected void onDetachedFromWindow(){removeCallbacks(tick);super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&isAttachedToWindow())tick.run();else removeCallbacks(tick);}
}
