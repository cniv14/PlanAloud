package app.planaloud;
import org.junit.Test;
import org.json.JSONObject;
import java.time.*;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=android.app.Application.class)
public class WeekGeometryTest {
    private final LocalDate date=LocalDate.of(2026,11,18);
    private JSONObject event(int from,int to)throws Exception{return new JSONObject().put("title","安排").put("start_at",date.atStartOfDay().plusMinutes(from).toString()).put("end_at",date.atStartOfDay().plusMinutes(to).toString());}
    @Test public void realClockPositionsAndDurationReplaceDayParts()throws Exception{WeekSchedule week=new WeekSchedule(date);week.days.get(0).get(0).add(event(9*60,10*60));week.days.get(0).get(0).add(event(10*60,12*60));WeekGeometry g=new WeekGeometry(week);assertEquals(480,g.firstMinute);assertEquals(1320,g.lastMinute);assertEquals(72,g.top(g.days.get(0).get(0),1.2f),.01);assertEquals(72,g.height(g.days.get(0).get(0),1.2f),.01);assertEquals(144,g.height(g.days.get(0).get(1),1.2f),.01);assertEquals(1,g.days.get(0).get(0).lanes);}
    @Test public void outsideHoursExtendAndMidnightIntervalsClip()throws Exception{WeekSchedule week=new WeekSchedule(date);week.days.get(0).get(0).add(event(-60,90));week.days.get(0).get(2).add(event(23*60,25*60));WeekGeometry g=new WeekGeometry(week);assertEquals(0,g.firstMinute);assertEquals(1440,g.lastMinute);assertEquals(0,g.days.get(0).get(0).start);assertEquals(1440,g.days.get(0).get(1).end);}
    @Test public void overlapColumnsAreSharedOnlyWithinConnectedGroups()throws Exception{WeekSchedule week=new WeekSchedule(date);week.days.get(0).get(0).add(event(540,660));week.days.get(0).get(0).add(event(570,600));week.days.get(0).get(0).add(event(600,630));week.days.get(0).get(1).add(event(780,840));WeekGeometry g=new WeekGeometry(week);assertEquals(2,g.days.get(0).get(0).lanes);assertEquals(2,g.days.get(0).get(2).lanes);assertEquals(g.days.get(0).get(1).lane,g.days.get(0).get(2).lane);assertEquals(1,g.days.get(0).get(3).lanes);assertEquals(0,g.days.get(0).get(3).lane);}
    @Test public void deadlineOnlyTasksNeverBecomeAppointments()throws Exception{WeekSchedule week=new WeekSchedule(date);week.days.get(0).get(3).add(new JSONObject().put("title","交报告").put("due_date",date.toString()));WeekGeometry g=new WeekGeometry(week);assertTrue(g.days.get(0).isEmpty());assertEquals(1,g.deadlines.get(0).size());assertEquals(480,g.firstMinute);assertEquals(1320,g.lastMinute);}
}
