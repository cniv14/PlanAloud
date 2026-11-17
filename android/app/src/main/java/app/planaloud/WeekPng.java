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
        .setMessage("① 从今天点「周预览」，用上一周／下一周选择日期。\n\n② 在表格中横向、纵向或斜向拖动；顶部星期行和左侧时段列固定对齐。点事项看详情，长按查看快捷操作。\n\n③ 全天事项用当天的连续浅色底表示，时段事项排在其中。「另有…项」可查看未展开的事项。\n\n④ 点「导出 PNG」，在系统文件选择器中选择文件夹并保存。图片包含当前所选七天的完整日程，含未在屏幕展开的事项，不受滚动位置影响。取消保存不会导出。\n\n⑤ 首页彩色进度条可点开查看事件与颜色；桌面小组件可直接勾选完成，无需打开界面。")
        .setPositiveButton("知道了",null).setNeutralButton("完整使用引导",(d,w)->Onboarding.show(a)).show();}
    static void previewExample(MainActivity a){Bitmap[] image={null};a.job(()->{image[0]=render(WeekSchedule.example(LocalDate.now()),true);return "示例已生成";},()->{
        ImageView preview=new ImageView(a);preview.setImageBitmap(image[0]);preview.setAdjustViewBounds(true);preview.setContentDescription("七天完整周日程 PNG 示例，不包含真实数据");ScrollView scroll=new ScrollView(a);scroll.addView(preview);new MaterialAlertDialogBuilder(a).setTitle("导出样式示例").setView(scroll).setMessage("实际导出：今天 → 周预览 → 导出 PNG → 选择保存位置。示例不会保存文件或修改你的日程。")
            .setPositiveButton("明白了",null).show();});}
    private static StaticLayout text(String value,TextPaint paint,int width){return StaticLayout.Builder.obtain(value,0,value.length(),paint,width).setIncludePad(false).setLineSpacing(3,1).build();}
    private static String entry(JSONObject record,LocalDate day,int band){String when=band<0?"全天":band==3?"截止 "+PlanningFields.deadlineLabel(record):PlanTime.dailyLabel(record,day);return when+"\n"+record.optString("title")+(record.optString("location").isEmpty()?"":"\n"+record.optString("location"));}
    static Bitmap render(WeekSchedule week,boolean example)throws IOException{
        TextPaint ink=new TextPaint(Paint.ANTI_ALIAS_FLAG);ink.setColor(0xff202c3b);ink.setTextSize(24);
        java.util.List<java.util.List<java.util.List<StaticLayout>>> blocks=new ArrayList<>();int[] heights={84,112,112,112,112};
        for(int day=0;day<7;day++){
            java.util.List<java.util.List<StaticLayout>> slots=new ArrayList<>();blocks.add(slots);
            for(int slot=0;slot<5;slot++){
                java.util.List<JSONObject> records=slot==0?week.allDay.get(day):week.days.get(day).get(slot-1);java.util.List<StaticLayout> items=new ArrayList<>();slots.add(items);int height=20;
                for(JSONObject record:records){StaticLayout layout=text(entry(record,week.first.plusDays(day),slot-1),ink,COLUMN-28);items.add(layout);height+=layout.getHeight()+26;}
                heights[slot]=Math.max(heights[slot],height);
            }
        }
        int height=216;for(int row:heights){if(row>10000||height>10000-row)throw new IOException("周日程内容过多，图片高度超过限制；请减少单周事项后再导出");height+=row;}
        Bitmap image=Bitmap.createBitmap(WIDTH,height,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff202c3b);paint.setTextSize(32);paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText(example?"周日程导出示例（非真实数据）":"序言 · 周日程",MARGIN,44,paint);paint.setTextSize(23);paint.setTypeface(Typeface.DEFAULT);canvas.drawText(week.first+" — "+week.first.plusDays(6),MARGIN,80,paint);
        int top=104,header=80;paint.setColor(0xffedf2f8);canvas.drawRect(MARGIN,top,WIDTH-MARGIN,top+header,paint);paint.setTextSize(23);paint.setColor(0xff202c3b);canvas.drawText("时段",MARGIN+16,top+46,paint);
        for(int day=0;day<7;day++){int x=MARGIN+LEFT+day*COLUMN;LocalDate date=week.first.plusDays(day);canvas.drawText(date.format(DateTimeFormatter.ofPattern("EEE",Locale.CHINA)),x+12,top+30,paint);canvas.drawText(date.format(DateTimeFormatter.ofPattern("M/d")),x+12,top+62,paint);}
        int y=top+header;String[] labels={"全天","上午","下午","晚上","截止"};
        for(int day=0;day<7;day++){if(!week.allDay.get(day).isEmpty()){paint.setColor(0xffedf4ea);canvas.drawRect(MARGIN+LEFT+day*COLUMN,y,MARGIN+LEFT+(day+1)*COLUMN,height-32,paint);}}
        for(int slot=0;slot<5;slot++){
            paint.setColor(0xff202c3b);canvas.drawText(labels[slot],MARGIN+16,y+38,paint);
            for(int day=0;day<7;day++){float x=MARGIN+LEFT+day*COLUMN+14,cy=y+12;for(StaticLayout block:blocks.get(day).get(slot)){if(slot>0){paint.setColor(0xffedf2fa);canvas.drawRoundRect(x-5,cy-5,x+COLUMN-23,cy+block.getHeight()+9,8,8,paint);}canvas.save();canvas.translate(x,cy);block.draw(canvas);canvas.restore();cy+=block.getHeight()+26;}}
            paint.setColor(0xffb4c0ce);paint.setStrokeWidth(1);canvas.drawLine(MARGIN,y,WIDTH-MARGIN,y,paint);y+=heights[slot];
        }
        paint.setColor(0xffb4c0ce);paint.setStrokeWidth(1);canvas.drawLine(MARGIN,top,WIDTH-MARGIN,top,paint);canvas.drawLine(MARGIN,y,WIDTH-MARGIN,y,paint);canvas.drawLine(MARGIN,top,MARGIN,y,paint);for(int col=0;col<=7;col++){float x=MARGIN+LEFT+col*COLUMN;canvas.drawLine(x,top,x,y,paint);}
        paint.setColor(0xff647487);paint.setTextSize(16);canvas.drawText("完整七天 · 已完成事项不展示 · 全天底色 / 时段安排",MARGIN,height-9,paint);return image;
    }
}
