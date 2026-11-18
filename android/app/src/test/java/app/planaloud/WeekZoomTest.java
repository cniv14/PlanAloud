package app.planaloud;
import android.app.Application;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class) @LooperMode(LooperMode.Mode.PAUSED)
public class WeekZoomTest {
    MainActivity a;ActivityController<MainActivity> controller;
    @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().putString("device_id","existing").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();a=controller.get();}
    @After public void close(){controller.pause().stop().destroy();}
    private TwoAxisScroll pan(){TwoAxisScroll pan=new TwoAxisScroll(a);pan.addView(new View(a),new FrameLayout.LayoutParams(1000,2000));pan.measure(View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));pan.layout(0,0,300,400);pan.zoomable(1,z->{},()->{});return pan;}
    @Test public void zoomRetainsTheContentUnderTheGestureFocusAndClampsBounds(){TwoAxisScroll pan=pan();pan.scrollTo(200,300);pan.setZoom(2,150,200);assertEquals(350,(pan.getScrollX()+150)/pan.zoom(),.1);assertEquals(500,(pan.getScrollY()+200)/pan.zoom(),.1);pan.setZoom(.01f,150,200);assertEquals(.3f,pan.zoom(),.001);pan.scrollTo(99999,99999);assertEquals(0,pan.getScrollX());assertEquals(200,pan.getScrollY());pan.setZoom(100,0,0);assertEquals(3,pan.zoom(),.001);}
    private MotionEvent pointers(long now,int action,float x1,float x2){MotionEvent.PointerProperties p1=new MotionEvent.PointerProperties(),p2=new MotionEvent.PointerProperties();p1.id=0;p1.toolType=MotionEvent.TOOL_TYPE_FINGER;p2.id=1;p2.toolType=MotionEvent.TOOL_TYPE_FINGER;MotionEvent.PointerCoords c1=new MotionEvent.PointerCoords(),c2=new MotionEvent.PointerCoords();c1.x=x1;c1.y=160;c1.pressure=c1.size=1;c2.x=x2;c2.y=160;c2.pressure=c2.size=1;return MotionEvent.obtain(now,now,action,2,new MotionEvent.PointerProperties[]{p1,p2},new MotionEvent.PointerCoords[]{c1,c2},0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);}
    @Test public void realTwoPointerGestureZoomsWithoutClickingAChild(){TwoAxisScroll pan=pan();pan.getChildAt(0).setOnClickListener(v->fail("Pinch became a click"));long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,80,160,0);pan.dispatchTouchEvent(down);down.recycle();MotionEvent second=pointers(now+10,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),80,220);pan.dispatchTouchEvent(second);second.recycle();for(int i=0;i<5;i++){MotionEvent move=pointers(now+30+i*20,MotionEvent.ACTION_MOVE,80-i*12,220+i*12);pan.dispatchTouchEvent(move);move.recycle();}assertTrue(pan.zoom()>1.2f);MotionEvent cancel=pointers(now+150,MotionEvent.ACTION_CANCEL,32,268);pan.dispatchTouchEvent(cancel);cancel.recycle();}
    @Test public void classicPreviewIsOneFixedTableAndZoomKeepsAxesAligned(){a.showWeek(java.time.LocalDate.now());org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle();View root=a.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800);View page=root.findViewWithTag("classic-week-page");assertNotNull(page);for(android.view.ViewParent parent=page.getParent();parent!=null;parent=parent.getParent())assertFalse(parent instanceof ScrollView);assertNull(page.findViewWithTag("week-deadlines"));TwoAxisScroll pan=page.findViewWithTag("week-pan");pan.setZoom(.6f,100,100);root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800);pan.scrollTo(100,100);View times=page.findViewWithTag("week-times"),heads=page.findViewWithTag("week-headings");assertEquals(-pan.getScrollY(),times.getTranslationY(),.1);assertEquals(Math.round(128*.6f),((android.view.ViewGroup)heads).getChildAt(0).getWidth(),1);}
}
