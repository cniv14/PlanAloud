package app.planaloud;
import org.junit.Test;
import java.time.LocalTime;
import static org.junit.Assert.*;
public class DayTimeTest {
    @Test public void progressSpansWholeLocalDayAndResetsAtMidnight(){
        assertEquals(0,DayTime.minute(LocalTime.MIDNIGHT));assertEquals(0,DayTime.percent(LocalTime.MIDNIGHT));
        assertEquals(25,DayTime.percent(LocalTime.of(6,0)));assertEquals(50,DayTime.percent(LocalTime.NOON));assertEquals(75,DayTime.percent(LocalTime.of(18,0)));
        assertEquals(1439,DayTime.minute(LocalTime.of(23,59)));assertEquals(99,DayTime.percent(LocalTime.of(23,59)));assertTrue(DayTime.label(LocalTime.NOON).contains("12:00"));
    }
}
