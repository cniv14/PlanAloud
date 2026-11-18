package app.planaloud;

import android.app.Application;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import app.planaloud.ui.UiScale;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class DisplayAndTimelineTest {
    ActivityController<MainActivity> controller;MainActivity activity;
    @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().putString("device_id","existing").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();}
    @After public void close(){controller.pause().stop().destroy();}
    private <T extends View>T find(View v,Class<T> type){if(type.isInstance(v))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T result=find(((ViewGroup)v).getChildAt(i),type);if(result!=null)return result;}return null;}
    private View described(View v,String name){if(name.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=described(((ViewGroup)v).getChildAt(i),name);if(result!=null)return result;}return null;}
    private DayTimeline chart(int count){DayTimeline chart=new DayTimeline(activity);List<DayEvents.Segment> events=new ArrayList<>();for(int i=0;i<count;i++)events.add(new DayEvents.Segment(480+i*30,540+i*30,Color.rgb((30+i*13)%225,100,200),i%2,"事件名称"+i));chart.setEvents(events);chart.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));chart.layout(0,0,360,chart.getMeasuredHeight());return chart;}
    @Test public void interfaceAndFontSettingsAreIndependentAndDoNotMutateSystemConfiguration(){Configuration system=new Configuration();system.densityDpi=400;system.fontScale=1.25f;system.screenWidthDp=400;system.screenHeightDp=800;Configuration changed=UiScale.configuration(system,80,120);assertEquals(320,changed.densityDpi);assertEquals(500,changed.screenWidthDp);assertEquals(1.5f,changed.fontScale,.001f);assertEquals(400,system.densityDpi);assertEquals(1.25f,system.fontScale,.001f);assertEquals(70,UiScale.interfacePercent(-1));assertEquals(160,UiScale.fontPercent(999));}
    @Test public void activityRecreationLoadsPersistedUiAndFontScale(){int density=activity.getResources().getConfiguration().densityDpi;float font=activity.getResources().getConfiguration().fontScale;activity.prefs.edit().putInt("ui_scale",80).putInt("font_scale",120).commit();controller.recreate();activity=controller.get();assertEquals(Math.round(density*.8f),activity.getResources().getConfiguration().densityDpi);assertEquals(font*1.2f,activity.getResources().getConfiguration().fontScale,.01f);}
    @Test public void classicKeepsOriginalTextActionsAndNavigation(){View root=activity.getWindow().getDecorView();com.google.android.material.bottomnavigation.BottomNavigationView nav=find(root,com.google.android.material.bottomnavigation.BottomNavigationView.class);assertEquals(com.google.android.material.bottomnavigation.BottomNavigationView.LABEL_VISIBILITY_LABELED,nav.getLabelVisibilityMode());assertFalse(UiStyle.neo(activity));assertTrue(nav.getMenu().findItem(100).isCheckable());assertTrue(nav.getMenu().findItem(102).isCheckable());assertNull(root.findViewWithTag("neo-shell"));}
    @Test public void calloutsUseNearbyNamesAndDensePagesStayCompact(){DayTimeline chart=chart(100);assertTrue(chart.getMeasuredHeight()<=Ui.dp(activity,200));List<String> first=chart.visibleNames();assertTrue(first.size()>0);assertTrue(first.size()<100);assertEquals("事件名称0",first.get(0));chart.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null);List<String> next=chart.visibleNames();assertFalse(next.contains("事件名称0"));for(String name:next)assertFalse(name.contains(":"));assertFalse(next.equals(first));}
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void nativeCalloutPreviewRendersWithLongTitles()throws Exception{DayTimeline chart=chart(5);chart.setEvents(Arrays.asList(new DayEvents.Segment(480,540,0xff5298e5,0,"大学基础课程与一段很长的事件名称"),new DayEvents.Segment(600,690,0xffdd7896,0,"小组讨论"),new DayEvents.Segment(840,960,0xff46b69e,0,"整理报告"),new DayEvents.Segment(1080,1140,0xffe79c46,0,"晚间阅读"),new DayEvents.Segment(1200,1260,0xff8d78dc,0,"明日计划")));chart.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));chart.layout(0,0,360,chart.getMeasuredHeight());Bitmap bitmap=Bitmap.createBitmap(360,chart.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Ui.surface(activity));chart.draw(canvas);File out=new File("build/reports/home-timeline-callouts.png");out.getParentFile().mkdirs();try(OutputStream stream=new FileOutputStream(out)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}assertTrue(out.length()>100);}
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(qualifiers="w360dp-h800dp") public void nativeTimetablePreviewShowsRealTimeGrid()throws Exception{
        java.time.LocalDate day=java.time.LocalDate.now();Data.get(activity).save(Data.make("event","整天准备项目").put("start_at",day.atStartOfDay().toString()).put("all_day",true));Data.get(activity).save(Data.make("event","学习资料").put("start_at",day.atTime(9,0).toString()).put("end_at",day.atTime(10,0).toString()));Data.get(activity).save(Data.make("event","小组讨论").put("start_at",day.atTime(9,30).toString()).put("end_at",day.atTime(11,0).toString()));Data.get(activity).save(Data.make("task","提交报告").put("due_date",day.toString()));for(boolean week:new boolean[]{false,true}){if(week)activity.showWeek(day);else activity.home();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800);View nav=find(root,com.google.android.material.bottomnavigation.BottomNavigationView.class);android.graphics.Rect bounds=new android.graphics.Rect();StringBuilder diagnostics=new StringBuilder();for(View current=nav;current!=null;current=current.getParent() instanceof View?(View)current.getParent():null)diagnostics.append(current.getClass().getSimpleName()).append(" y=").append(current.getY()).append(" h=").append(current.getHeight()).append(" visibility=").append(current.getVisibility()).append("; ");assertTrue(diagnostics.toString(),nav.getGlobalVisibleRect(bounds));assertTrue(diagnostics.toString(),bounds.bottom<=800);assertTrue(diagnostics.toString()+bounds,bounds.top>600);File boundsFile=new File("build/reports/dock-bounds.txt");boundsFile.getParentFile().mkdirs();try(java.io.FileWriter writer=new java.io.FileWriter(boundsFile,true)){writer.write(diagnostics.toString()+bounds+"\n");}Bitmap bitmap=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));File out=new File(week?"build/reports/week-time-grid.png":"build/reports/home-compact.png");out.getParentFile().mkdirs();try(OutputStream stream=new FileOutputStream(out)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}if(week)assertNotNull(root.findViewWithTag("week-deadlines"));}
    }

}
