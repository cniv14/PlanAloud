package app.planaloud;
import android.app.Application;
import android.text.TextPaint;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class TimelineCalloutsTest {
    private List<DayEvents.Segment> events(int count){List<DayEvents.Segment> result=new ArrayList<>();for(int i=0;i<count;i++)result.add(new DayEvents.Segment(480+i,540+i,0xff5298e5,0,"测试事件名称"+i));return result;}
    @Test public void labelsUseBothSidesAndNeverOverlapWithinOneRow(){TextPaint paint=new TextPaint();paint.setTextSize(12);List<DayEvents.Segment> events=events(3);TimelineCallouts.Layout layout=TimelineCallouts.layout(events,0,360,paint,8,6,132);assertEquals(3,layout.labels.size());assertTrue(layout.above>0);assertTrue(layout.below>0);for(TimelineCallouts.Label a:layout.labels){assertTrue(a.left>=8);assertTrue(a.right<=352.1f);assertEquals(8+344*(a.event.start+a.event.end)/2880f,a.anchor,.01);for(TimelineCallouts.Label b:layout.labels)if(a!=b&&a.side==b.side&&a.row==b.row)assertTrue(a.right+5.9f<=b.left||b.right+5.9f<=a.left);}}
    @Test public void densePagesExposeEveryEventWithoutDroppingAnyName(){TextPaint paint=new TextPaint();paint.setTextSize(16);List<DayEvents.Segment> events=events(80);Set<Integer> seen=new HashSet<>();int start=0,pages=0;while(start<events.size()){TimelineCallouts.Layout layout=TimelineCallouts.layout(events,start,240,paint,8,6,132);assertFalse(layout.labels.isEmpty());assertTrue(layout.next>start);assertTrue(layout.above<=2&&layout.below<=2);for(TimelineCallouts.Label label:layout.labels)assertTrue(seen.add(label.index));start=layout.next;pages++;}assertEquals(80,seen.size());assertTrue(pages>1);}
}
