package app.planaloud;
import android.app.Application;
import android.graphics.*;
import android.view.View;
import java.time.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class) @LooperMode(LooperMode.Mode.PAUSED)
public class TimelineFoldTest {
    MainActivity a;ActivityController<MainActivity> controller;
    @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().putString("device_id","existing").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();a=controller.get();}
    @After public void close(){controller.pause().stop().destroy();}
    @Test public void defaultFoldOnlyIncludesCurrentTimeAndTapTogglesAllNames(){DayTimeline chart=new DayTimeline(a,()->LocalTime.of(10,0));chart.setEvents(Arrays.asList(new DayEvents.Segment(540,600,Color.BLUE,0,"已经结束"),new DayEvents.Segment(600,660,Color.GREEN,0,"当前时段"),new DayEvents.Segment(660,720,Color.RED,0,"后续事件")));chart.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));chart.layout(0,0,360,chart.getMeasuredHeight());assertFalse(chart.isExpanded());assertEquals(Collections.singletonList("当前时段"),chart.visibleNames());chart.performClick();assertTrue(chart.isExpanded());assertEquals(3,chart.visibleNames().size());chart.performClick();assertFalse(chart.isExpanded());assertEquals(Collections.singletonList("当前时段"),chart.visibleNames());}
    @Test public void emptyCurrentPeriodDoesNotRecommendAFutureEvent(){assertTrue(DayEvents.at(Arrays.asList(new DayEvents.Segment(600,660,Color.BLUE,0,"下一节")),LocalTime.of(9,36)).isEmpty());}
    @Test public void completingAnEventDoesNotChangeItsTimePositionOrColor()throws Exception{LocalDate day=LocalDate.of(2098,2,12);JSONObject record=Data.make("event","完成不影响时间").put("start_at",day.atTime(9,0).toString()).put("end_at",day.atTime(11,0).toString());Data.get(a).save(record);DayEvents.Segment before=find(DayEvents.load(a,day),record.optString("title"));TaskActions.complete(a,record);DayEvents.Segment after=find(DayEvents.load(a,day),record.optString("title"));assertEquals(before.start,after.start);assertEquals(before.end,after.end);assertEquals(before.lane,after.lane);assertEquals(before.color,after.color);assertEquals(1,DayEvents.at(Collections.singletonList(after),LocalTime.of(10,0)).size());}
    private DayEvents.Segment find(List<DayEvents.Segment> events,String title){for(DayEvents.Segment s:events)if(s.title.equals(title))return s;throw new AssertionError("Missing event");}
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void bordersSeparateAdjacentBlocksEvenWhenTheirColorsMatch(){Bitmap image=Bitmap.createBitmap(100,12,Bitmap.Config.ARGB_8888);int fill=0xff8daee0;DayEvents.draw(new Canvas(image),new Paint(Paint.ANTI_ALIAS_FLAG),Arrays.asList(new DayEvents.Segment(0,720,fill,0,"A"),new DayEvents.Segment(720,1440,fill,0,"B")),0,0,100,12,Color.BLACK);assertEquals(fill,image.getPixel(25,5));assertNotEquals(image.getPixel(25,5),image.getPixel(50,5));}
}
