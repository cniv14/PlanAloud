package app.planaloud;
import android.app.Application;
import java.time.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WeekSpansTest {
    @Test public void oneMultiDayTaskConnectsAcrossDatesAndOnlyLabelsToday()throws Exception{LocalDate first=LocalDate.of(2026,11,16);WeekSchedule week=new WeekSchedule(first);JSONObject r=new JSONObject().put("id","long").put("title","长期研发").put("all_day",true).put("start_at",first.minusDays(2).atStartOfDay().toString()).put("end_at",first.plusDays(5).atStartOfDay().toString());for(int d=0;d<5;d++)week.allDay.get(d).add(r);WeekSpans spans=new WeekSpans(week);assertEquals(1,spans.spans.size());assertEquals(0,spans.spans.get(0).first);assertEquals(4,spans.spans.get(0).last);assertEquals(2,spans.spans.get(0).titleDay(first,first.plusDays(2)));assertEquals(-1,spans.spans.get(0).titleDay(first,first.plusDays(6)));}
    @Test public void overlappingBackgroundNamesUseDifferentRows()throws Exception{LocalDate first=LocalDate.of(2026,11,16);WeekSchedule week=new WeekSchedule(first);for(int n=0;n<3;n++)week.backgroundProjects.add(new JSONObject().put("id","p"+n).put("kind","project").put("title","项目"+n).put("due_date",first.plusDays(6).toString()));WeekSpans spans=new WeekSpans(week);assertEquals(3,spans.lanes);assertNotEquals(spans.spans.get(0).lane,spans.spans.get(1).lane);}
    @Test public void oneDayAllDayAndOvernightAppointmentsDoNotTurnIntoLongBackgrounds()throws Exception{LocalDate first=LocalDate.of(2026,11,16);assertFalse(WeekSpans.eligible(new JSONObject().put("all_day",true).put("start_at",first.atStartOfDay().toString()).put("end_at",first.plusDays(1).atStartOfDay().toString())));assertFalse(WeekSpans.eligible(new JSONObject().put("kind","event").put("start_at",first.atTime(23,0).toString()).put("end_at",first.plusDays(1).atTime(2,0).toString())));}
}
