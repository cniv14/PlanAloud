package app.planaloud;
import android.app.Application;
import android.content.*;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class) @LooperMode(LooperMode.Mode.PAUSED)
public class UiStyleTest {
    MainActivity activity;ActivityController<MainActivity> controller;
    @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("settings",0).edit().clear().putString("device_id","existing").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();}
    @After public void close(){controller.pause().stop().destroy();}
    private View described(View view,String name){if(name.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View child=described(((ViewGroup)view).getChildAt(i),name);if(child!=null)return child;}return null;}
    private void neo(){activity.prefs.edit().putString("ui_style","neo").commit();controller.recreate();activity=controller.get();}
    @Test public void defaultClassicAndSwitchingNeverModifyRecords()throws Exception{assertFalse(UiStyle.neo(activity));JSONObject record=Data.make("task","保留的事项");Data.get(activity).save(record);neo();assertTrue(UiStyle.neo(activity));assertNotNull(activity.getWindow().getDecorView().findViewWithTag("neo-shell"));activity.changeUiStyle("classic");controller.recreate();activity=controller.get();assertFalse(UiStyle.neo(activity));assertNull(activity.getWindow().getDecorView().findViewWithTag("neo-shell"));assertEquals(record.getString("revision"),Data.get(activity).find(record.getString("id")).getString("revision"));}
    @Test public void neoSwitchesAxisAndListAndKeepsSelectedWeekForExport(){neo();java.time.LocalDate first=java.time.LocalDate.of(2099,3,9);activity.showWeek(first);View root=activity.getWindow().getDecorView();assertNotNull(root.findViewWithTag("week-table"));described(root,"切换到列表视图").performClick();root=activity.getWindow().getDecorView();assertNotNull(root.findViewWithTag("neo-list"));described(root,"保存周日程 PNG").performClick();Intent intent=shadowOf(activity).getNextStartedActivityForResult().intent;assertEquals("image/png",intent.getType());assertTrue(intent.getStringExtra(Intent.EXTRA_TITLE).contains(first.toString()));assertNotNull(root.findViewWithTag("neo-ai"));}
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(qualifiers="w360dp-h800dp") public void neoNativePreviewKeepsToolbarAndAiAccessible()throws Exception{neo();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800);View ai=root.findViewWithTag("neo-ai");assertNotNull(ai);assertEquals("问序言 AI",ai.getContentDescription());assertTrue(ai.getWidth()>=Ui.dp(activity,48));com.google.android.material.floatingactionbutton.FloatingActionButton fab=(com.google.android.material.floatingactionbutton.FloatingActionButton)ai;assertNull(fab.getImageTintList());assertEquals(Ui.dp(activity,28),fab.getShapeAppearanceModel().getTopLeftCornerSize().getCornerSize(new RectF(0,0,56,56)),.1f);assertNotNull(described(root,"更多功能"));assertNotNull(described(root,"切换到列表视图"));Bitmap image=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);root.draw(new Canvas(image));File file=new File("build/reports/neo-week.png");file.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(file)){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}}
}
