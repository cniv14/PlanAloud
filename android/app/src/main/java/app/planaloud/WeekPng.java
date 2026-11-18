package app.planaloud;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.text.*;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.JSONObject;

/** Renders the complete week, rather than taking a screenshot of the clipped viewport. */
final class WeekPng {
    private static final String PENDING="pending_week_png";
    private static final int WIDTH=1808,COLUMN=240,LEFT=104,MARGIN=12;
    static void request(MainActivity a,LocalDate first){
        a.prefs.edit().putString(PENDING,first.toString()).commit();
        try{a.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/png").putExtra(Intent.EXTRA_TITLE,"序言-周日程-"+first+".png"),124);}catch(RuntimeException error){a.prefs.edit().remove(PENDING).apply();a.message("无法打开保存位置："+error.getMessage());}
    }
    static void result(MainActivity a,int result,Intent data){
        String pending=a.prefs.getString(PENDING,"");a.prefs.edit().remove(PENDING).apply();if(result!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        if(pending.isEmpty()){a.message("导出日期已失效，请重新选择导出");return;}
        LocalDate first;try{first=LocalDate.parse(pending);}catch(RuntimeException error){a.message("无法读取导出日期，请重试");return;}
        Uri target=data.getData();a.job(()->{Bitmap image=null;try{image=render(WeekSchedule.load(a,first),false);try(OutputStream out=a.getContentResolver().openOutputStream(target,"wt")){if(out==null)throw new IOException("无法打开保存位置");if(!image.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG 编码失败");}return "已导出完整周日程 PNG";}catch(OutOfMemoryError memory){throw new IOException("周日程过大，无法生成图片，请减少单周事项后重试",memory);}finally{if(image!=null)image.recycle();}},null);
    }
    static void help(MainActivity a){new MaterialAlertDialogBuilder(a).setTitle("周预览怎么用")
        .setMessage("① 经典从今天点「周预览」，Neo 直接显示周表；用上一周／下一周选择日期。\n\n② 在表格中横向、纵向或斜向拖动；顶部星期行和左侧时段列固定对齐。点事项看详情，长按查看快捷操作。\n\n③ 周表按实际时间排布，默认 08:00–22:00，超出会自动延长。事件高度对应持续时间，重叠事件并排显示。全天事项作为当天的背景，侧栏可查看名称；只有截止日期的待办在底部细条中查看。\n\n④ 点保存图标（导出 PNG），在系统文件选择器中选择文件夹并保存。图片包含当前所选七天的完整日程，含未在屏幕展开的事项，不受滚动位置影响。取消保存不会导出。\n\n⑤ 首页进度条用斜线连接事件名称，名称较多时可上下滑动切换更多标注，点击可查看完整信息；桌面小组件可直接勾选完成，无需打开界面。")
        .setPositiveButton("知道了",null).setNeutralButton("完整使用引导",(d,w)->Onboarding.show(a)).show();}
    static void previewExample(MainActivity a){Bitmap[] image={null};a.job(()->{image[0]=render(WeekSchedule.example(LocalDate.now()),true);return "示例已生成";},()->{
        ImageView preview=new ImageView(a);preview.setImageBitmap(image[0]);preview.setAdjustViewBounds(true);preview.setContentDescription("七天完整周日程 PNG 示例，不包含真实数据");ScrollView scroll=new ScrollView(a);scroll.addView(preview);new MaterialAlertDialogBuilder(a).setTitle("导出样式示例").setView(scroll).setMessage("实际导出：今天 → 周预览 → 导出 PNG → 选择保存位置。示例不会保存文件或修改你的日程。")
            .setPositiveButton("明白了",null).show();});}
    private static StaticLayout text(String value,TextPaint paint,int width){return StaticLayout.Builder.obtain(value,0,value.length(),paint,width).setIncludePad(false).setLineSpacing(3,1).build();}
    private static String entry(JSONObject record,LocalDate day,int band){String when=band<0?"全天":band==3?"截止 "+PlanningFields.deadlineLabel(record):PlanTime.dailyLabel(record,day);return when+"\n"+record.optString("title")+(record.optString("location").isEmpty()?"":"\n"+record.optString("location"));}
    static Bitmap render(WeekSchedule week,boolean example)throws IOException{
        WeekGeometry geometry=new WeekGeometry(week);TextPaint ink=new TextPaint(Paint.ANTI_ALIAS_FLAG);ink.setColor(0xff202c3b);ink.setTextSize(22);
        java.util.List<java.util.List<StaticLayout>> appendix=new ArrayList<>();int appendixHeight=0;
        for(int d=0;d<7;d++){
            java.util.List<StaticLayout> items=new ArrayList<>();appendix.add(items);int total=0;
            for(JSONObject r:week.allDay.get(d)){StaticLayout block=text(entry(r,week.first.plusDays(d),-1),ink,COLUMN-24);items.add(block);total+=block.getHeight()+22;}
            for(WeekGeometry.Timed event:geometry.days.get(d)){StaticLayout block=text(entry(event.record,week.first.plusDays(d),0),ink,COLUMN-24);items.add(block);total+=block.getHeight()+22;}
            for(JSONObject r:geometry.deadlines.get(d)){StaticLayout block=text(entry(r,week.first.plusDays(d),3),ink,COLUMN-24);items.add(block);total+=block.getHeight()+22;}
            appendixHeight=Math.max(appendixHeight,total);
        }
        int top=104,header=80,chartTop=top+header,chartBottom=chartTop+geometry.minutes(),appendixTop=chartBottom+92,height=appendixTop+appendixHeight+32;
        if(height>10000)throw new IOException("周日程内容过多，暂时无法生成单张图片");
        Bitmap image=Bitmap.createBitmap(WIDTH,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff202c3b);paint.setTextSize(32);paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(example?"周日程导出示例（非真实数据）":"序言 · 周日程",MARGIN,44,paint);paint.setTextSize(23);paint.setTypeface(Typeface.DEFAULT);canvas.drawText(week.first+" — "+week.first.plusDays(6),MARGIN,80,paint);
        paint.setColor(0xffedf2f8);canvas.drawRect(MARGIN,top,WIDTH-MARGIN,chartTop,paint);paint.setColor(0xff202c3b);paint.setTextSize(23);canvas.drawText("时间",MARGIN+16,top+46,paint);
        for(int d=0;d<7;d++){
            int x=MARGIN+LEFT+d*COLUMN;LocalDate date=week.first.plusDays(d);paint.setColor(0xff202c3b);canvas.drawText(date.format(DateTimeFormatter.ofPattern("EEE",Locale.CHINA)),x+12,top+30,paint);canvas.drawText(date.format(DateTimeFormatter.ofPattern("M/d")),x+12,top+62,paint);
            boolean all=!week.allDay.get(d).isEmpty();int rail=all?22:0;
            if(all){paint.setColor(0xffedf4ea);canvas.drawRect(x,chartTop,x+COLUMN,chartBottom,paint);paint.setColor(0xff526648);paint.setTextSize(17);StringBuilder title=new StringBuilder("全天");for(JSONObject r:week.allDay.get(d))title.append(" · ").append(r.optString("title"));canvas.save();canvas.clipRect(x,chartTop,x+22,chartBottom);canvas.translate(x+17,chartTop+8);canvas.rotate(90);canvas.drawText(title.toString(),0,0,paint);canvas.restore();paint.setTextSize(23);}
            for(WeekGeometry.Timed event:geometry.days.get(d)){
                float width=(COLUMN-rail-8f)/event.lanes,left=x+rail+4+event.lane*width,y=chartTop+geometry.top(event,1),h=geometry.height(event,1);paint.setColor(0xffdfebfa);canvas.drawRoundRect(left,y,left+width-3,y+Math.max(2,h),5,5,paint);
                TextPaint caption=new TextPaint(Paint.ANTI_ALIAS_FLAG);caption.setColor(0xff202c3b);caption.setTextSize(18);StaticLayout label=text(event.record.optString("title"),caption,Math.max(1,(int)width-10));canvas.save();canvas.clipRect(left,y,left+width-3,y+h);canvas.translate(left+3,y+2);label.draw(canvas);canvas.restore();
            }
            java.util.List<JSONObject> due=geometry.deadlines.get(d);paint.setColor(0xffedf2f8);canvas.drawRect(x,chartBottom+34,x+COLUMN,chartBottom+40,paint);if(!due.isEmpty()){ink.setTextSize(18);CharSequence name=TextUtils.ellipsize(due.get(0).optString("title")+(due.size()>1?" +"+(due.size()-1):""),ink,COLUMN-16,TextUtils.TruncateAt.END);canvas.drawText(name.toString(),x+8,chartBottom+26,ink);ink.setTextSize(22);}
        }
        paint.setTextSize(19);paint.setStrokeWidth(1);
        for(int minute=geometry.firstMinute;minute<=geometry.lastMinute;minute+=60){float y=chartTop+minute-geometry.firstMinute;paint.setColor(0xffb4c0ce);canvas.drawLine(MARGIN+LEFT,y,WIDTH-MARGIN,y,paint);paint.setColor(minute<480||minute>1320?0xff437bb2:0xff647487);canvas.drawText(DayEvents.clock(minute),MARGIN+8,Math.min(chartBottom-3,Math.max(chartTop+18,y+7)),paint);}
        paint.setColor(0xffb4c0ce);for(int col=0;col<=7;col++){float x=MARGIN+LEFT+col*COLUMN;canvas.drawLine(x,top,x,chartBottom+40,paint);}canvas.drawLine(MARGIN,top,WIDTH-MARGIN,top,paint);canvas.drawLine(MARGIN,chartBottom+40,WIDTH-MARGIN,chartBottom+40,paint);
        paint.setColor(0xff647487);paint.setTextSize(18);canvas.drawText("截止",MARGIN+12,chartBottom+26,paint);paint.setTextSize(22);canvas.drawText("完整事项（含短时、重叠安排与截止待办）",MARGIN,appendixTop-16,paint);
        for(int d=0;d<7;d++){float x=MARGIN+LEFT+d*COLUMN+12,y=appendixTop;for(StaticLayout block:appendix.get(d)){canvas.save();canvas.translate(x,y);block.draw(canvas);canvas.restore();y+=block.getHeight()+22;}}
        paint.setColor(0xff647487);paint.setTextSize(16);canvas.drawText("按实际时间排布 · 默认 08:00–22:00，超出自动延长 · 已完成事项不展示",MARGIN,height-9,paint);return image;
    }
}
