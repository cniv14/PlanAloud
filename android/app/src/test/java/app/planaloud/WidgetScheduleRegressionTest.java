package app.planaloud;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.widget.*;
import java.time.*;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class WidgetScheduleRegressionTest {
    private Context context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("settings",0).edit().clear().commit();}

    @Test @Config(sdk=31) public void singleDayRowsClipBothImageAndAnimationAtTheirActualSize(){
        for(int layout:new int[]{R.layout.widget_item,R.layout.widget_item_detailed,R.layout.widget_agenda}){
            View row=new RemoteViews(context.getPackageName(),layout).apply(context,new FrameLayout(context));
            assertTrue(row.getClipToOutline());
            assertNotNull(row.getBackground());
            if(layout!=R.layout.widget_agenda){
                assertNotNull(row.findViewById(R.id.widget_item_background));
                assertNotNull(row.findViewById(R.id.widget_item_animation));
            }
        }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void olderAndroidKeepsRoundedBitmapCorners()throws Exception{
        JSONObject record=Data.make("event","旧系统圆角").put("card_background","color").put("card_texture","none");
        android.graphics.Bitmap image=EventColors.widget(context,record,.5f,300,110);
        assertEquals(0,android.graphics.Color.alpha(image.getPixel(0,0)));
        assertEquals(255,android.graphics.Color.alpha(image.getPixel(150,55)));
        image.recycle();
    }

    @Test public void twoDayWidgetKeepsOriginalStyleDespiteGlobalAndRecordOverrides()throws Exception{
        context.getSharedPreferences("settings",0).edit().putString("card_background","motion:gold").putString("card_texture","flow").commit();
        JSONObject record=Data.make("event","双日测试").put("card_background","motion:prism").put("card_texture","glass").put("priority","high");
        TwoDayCollection.Rows factory=new TwoDayCollection.Rows(context,0);
        factory.day=LocalDate.now();factory.rows=Collections.singletonList(record);
        View row=factory.getViewAt(0).apply(context,new FrameLayout(context));
        assertTrue(row instanceof LinearLayout);
        assertNull(row.getBackground());
        assertEquals(0xfff1f0fa,((TextView)row.findViewById(R.id.two_day_item_title)).getCurrentTextColor());
        assertEquals(0xffa8a7bc,((TextView)row.findViewById(R.id.two_day_item_meta)).getCurrentTextColor());
        assertEquals(0xffb6a0ef,((TextView)row.findViewById(R.id.two_day_item_done)).getCurrentTextColor());
        assertNotNull(row.findViewById(R.id.two_day_stripe));
    }

    @Test public void scheduleCsvNeedsDatesWithoutTermAndImportsIdempotently()throws Exception{
        String csv="title,start_at,end_at,location\n阅读,2098-11-23T07:00,2098-11-23T08:00,图书馆\n";
        JSONArray rows=Imports.schedule(csv);
        assertEquals("event",rows.getJSONObject(0).getString("kind"));
        assertEquals("2098-11-23T07:00",rows.getJSONObject(0).getString("start_at"));
        assertEquals(rows.getJSONObject(0).getString("id"),Imports.schedule(csv).getJSONObject(0).getString("id"));
        assertEquals(1,Data.get(context).importNew(rows));
        assertEquals(0,Data.get(context).importNew(Imports.schedule(csv)));
    }

    @Test public void existingCourseRecordsStayInAgendaWithTheirRevisionAndReminder()throws Exception{
        LocalDate date=LocalDate.of(2098,11,24);
        JSONObject old=Data.make("event","旧版课程").put("kind","course").put("start_at",date.atTime(9,0).toString()).put("reminder",true);
        Data.get(context).save(old);String revision=old.getString("revision");
        assertTrue(DayAgenda.load(context,date).stream().anyMatch(row->old.optString("id").equals(row.optString("id"))));
        assertEquals(old.getString("id"),Data.get(context).business("旧版课程","event",0).getJSONObject(0).getString("id"));
        JSONObject stored=Data.get(context).find(old.getString("id"));
        assertEquals(revision,stored.getString("revision"));assertTrue(stored.getBoolean("reminder"));
        assertEquals("event",Data.make("course","兼容旧调用").getString("kind"));
    }

    @Test public void retiredCourseCommandsAreAbsentFromAssistantCatalog()throws Exception{
        for(DayCommands.Entry entry:DayCommands.entries())assertFalse(entry.id.startsWith("course."));
        assertFalse(FeatureGuide.read(context,new JSONObject().put("topic","calendar")).getString("markdown").contains("stage_timetable"));
    }

    @Test public void recurringSchedulesUseNormalWeeklyRules()throws Exception{
        JSONObject event=Data.make("event","重复安排").put("start_at","2098-11-24T09:00").put("recurrence","weekly").put("recurrence_until","2098-12-15");
        Data.validate(event);Data.get(context).save(event);
        assertTrue(DayAgenda.load(context,LocalDate.of(2098,12,1)).stream().anyMatch(row->"重复安排".equals(row.optString("title"))));
    }
}
