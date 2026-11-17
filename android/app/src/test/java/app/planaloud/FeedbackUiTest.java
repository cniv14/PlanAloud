package app.planaloud;

import android.app.Application;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import java.io.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class FeedbackUiTest {
    private MainActivity activity;private ActivityController<MainActivity> controller;
    @Before public void launch(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("settings",0).edit().clear().putString("device_id","existing").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();layout();}
    @After public void close(){controller.pause().stop().destroy();}
    private void layout(){shadowOf(Looper.getMainLooper()).idle();View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,1920);}
    private TextView text(View v,String title){if(v instanceof TextView&&title.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView child=text(((ViewGroup)v).getChildAt(i),title);if(child!=null)return child;}return null;}
    private <T extends View>T find(View v,Class<T> type){if(type.isInstance(v))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T child=find(((ViewGroup)v).getChildAt(i),type);if(child!=null)return child;}return null;}
    private void jobs()throws Exception{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("worker");f.setAccessible(true);((ExecutorService)f.get(activity)).submit(()->{}).get(15,TimeUnit.SECONDS);layout();}
    private Uri picture(int width,int height)throws Exception{Bitmap image=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);image.eraseColor(Color.CYAN);File file=File.createTempFile("wallpaper-",".jpg",activity.getCacheDir());try(OutputStream out=new FileOutputStream(file)){assertTrue(image.compress(Bitmap.CompressFormat.JPEG,90,out));}return Uri.fromFile(file);}
    @Test public void wallpaperCropSavesReadableContentUriWithoutClosingActivity()throws Exception{
        AtomicReference<Uri> saved=new AtomicReference<>();ImageTools.crop(activity,picture(640,480),"裁切壁纸",saved::set);jobs();
        TextView rotate=text(activity.getWindow().getDecorView(),"旋转");assertNotNull(rotate);rotate.performClick();layout();text(activity.getWindow().getDecorView(),"完成裁剪").performClick();jobs();
        assertNotNull(saved.get());assertEquals("content",saved.get().getScheme());try(InputStream in=activity.getContentResolver().openInputStream(saved.get())){Bitmap result=BitmapFactory.decodeStream(in);assertNotNull(result);assertTrue(result.getWidth()<=1600&&result.getHeight()<=1600);}assertFalse(activity.isFinishing());
    }
    @Test public void wallpaperDecodeBoundsLargeInputAndCorrectsExifRotation()throws Exception{
        Uri input=picture(4000,2200);Bitmap bounded=WallpaperImages.decode(activity,input,1800);assertTrue(Math.max(bounded.getWidth(),bounded.getHeight())<=1800);
        Uri rotated=picture(240,120);android.media.ExifInterface exif=new android.media.ExifInterface(rotated.getPath());exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION,"6");exif.saveAttributes();Bitmap portrait=WallpaperImages.decode(activity,rotated,1800);assertEquals(120,portrait.getWidth());assertEquals(240,portrait.getHeight());
    }
    @Test public void cropCallbackFailureIsReportedWithoutFinishing()throws Exception{
        ImageTools.crop(activity,picture(64,64),uri->{throw new IllegalStateException("test callback");});jobs();text(activity.getWindow().getDecorView(),"使用整图").performClick();layout();assertFalse(activity.isFinishing());
    }
    @Test public void weekCanvasPansDiagonallyAndClampsAtBothEnds(){
        TwoAxisScroll pan=new TwoAxisScroll(activity);View large=new View(activity);large.setOnClickListener(v->fail("A drag must not click the cell"));pan.addView(large,new android.widget.FrameLayout.LayoutParams(900,1100));FrameLayout root=new FrameLayout(activity);root.addView(pan);pan.measure(View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(250,View.MeasureSpec.EXACTLY));pan.layout(0,0,300,250);
        long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,220,220,0),move=MotionEvent.obtain(now,now+80,MotionEvent.ACTION_MOVE,100,90,0);pan.dispatchTouchEvent(down);pan.dispatchTouchEvent(move);MotionEvent continued=MotionEvent.obtain(now,now+90,MotionEvent.ACTION_MOVE,80,60,0);pan.dispatchTouchEvent(continued);continued.recycle();assertEquals(140,pan.getScrollX());assertEquals(160,pan.getScrollY());MotionEvent cancel=MotionEvent.obtain(now,now+100,MotionEvent.ACTION_CANCEL,80,60,0);pan.dispatchTouchEvent(cancel);cancel.recycle();down.recycle();move.recycle();pan.scrollTo(9999,9999);assertEquals(600,pan.getScrollX());assertEquals(850,pan.getScrollY());
    }
    @Test public void weekAllDayBackgroundExtendsBeyondItsHeader()throws Exception{
        LocalDate day=LocalDate.now();JSONObject full=Data.make("event","整天事项").put("start_at",day.atStartOfDay().toString()).put("all_day",true);Data.get(activity).save(full);LinearLayout page=activity.fullPage("周测试",activity::home);WeekPreview.show(activity,page,day,x->{},()->{});layout();View body=page.findViewWithTag("week-all-day");assertNotNull(body);assertNotNull(body.getBackground());assertTrue(body.getHeight()>Ui.dp(activity,300));assertNotNull(find(page,TwoAxisScroll.class));
    }
    @Test public void appearanceEntriesAreSquareAndContainIconPreview(){
        new SettingsPanels(activity).appearance();layout();TextView label=text(activity.getWindow().getDecorView(),"应用图标");assertNotNull(label);View body=(View)label.getParent();View card=(View)body.getParent();assertEquals(card.getWidth(),card.getHeight());assertNotNull(find(body,ImageView.class));assertNull(text(activity.getWindow().getDecorView(),"动态流光"));
    }
    @Test public void automaticApprovalPreferenceChangesWhetherDeletionExecutes()throws Exception{
        JSONObject record=Data.make("task","待确认删除");Data.get(activity).save(record);JSONObject args=new JSONObject().put("id",record.getString("id")).put("revision",record.getString("revision"));DayCommands.Access access=new DayCommands.Access("agent:test","ui-test",true,false,false);
        activity.prefs.edit().putBoolean("assistant_auto_approve",false).commit();JSONObject queued=DayCommands.execute(activity,"record.delete",args,access);assertTrue(queued.getBoolean("requires_approval"));assertFalse(Data.get(activity).find(record.getString("id")).optBoolean("deleted"));
        DayCommands.approve(activity,queued.getString("approval_token"),"ui-test",true);assertTrue(Data.get(activity).find(record.getString("id")).optBoolean("deleted"));
        JSONObject second=Data.make("task","直接删除");Data.get(activity).save(second);activity.prefs.edit().putBoolean("assistant_auto_approve",true).commit();JSONObject result=DayCommands.execute(activity,"record.delete",new JSONObject().put("id",second.getString("id")).put("revision",second.getString("revision")),access);assertFalse(result.optBoolean("requires_approval"));assertTrue(Data.get(activity).find(second.getString("id")).optBoolean("deleted"));
    }
    @Test public void chatCanConfigureApprovalButOnlyWithValidSession()throws Exception{
        activity.prefs.edit().putString("assistant_session","ui-token").putLong("assistant_expiry",System.currentTimeMillis()+60000).commit();Bundle input=new Bundle();input.putString("session","ui-token");input.putString("json","{\"enabled\":false}");Bundle reply=AssistantPlugin.call(activity,"assistant_approval_settings",input);assertFalse(new JSONObject(reply.getString("json")).getBoolean("enabled"));input.putString("session","wrong");input.putString("json","{\"enabled\":true}");try{AssistantPlugin.call(activity,"assistant_approval_settings",input);fail("Invalid session accepted");}catch(SecurityException expected){}assertFalse(activity.prefs.getBoolean("assistant_auto_approve",true));
    }
    @Test public void guideHasPracticeControlsWithLiveCompletionFeedback(){
        Onboarding.show(activity);layout();text(activity.getWindow().getDecorView(),"下一步").performClick();layout();assertNotNull(text(activity.getWindow().getDecorView(),"先动手试一试"));com.google.android.material.checkbox.MaterialCheckBox check=find(activity.getWindow().getDecorView(),com.google.android.material.checkbox.MaterialCheckBox.class);assertNotNull(check);check.setChecked(true);layout();assertNotNull(text(activity.getWindow().getDecorView(),"示例待办已完成 ✓ 再点一次可以恢复"));assertNotNull(text(activity.getWindow().getDecorView(),"跳过"));
    }
    @Test public void weekHeadersStayPinnedWhileFollowingTheirOwnAxis(){
        LinearLayout page=activity.fullPage("周测试",activity::home);WeekPreview.show(activity,page,LocalDate.now(),x->{},()->{});layout();
        View decor=activity.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,700);
        View head=page.findViewWithTag("week-headings"),times=page.findViewWithTag("week-times");ViewGroup lower=(ViewGroup)((View)times.getParent()).getParent();TwoAxisScroll pan=(TwoAxisScroll)lower.getChildAt(1);
        pan.scrollTo(120,100);assertEquals(120,pan.getScrollX());assertTrue(pan.getScrollY()>0);assertEquals(120,((View)head.getParent()).getScrollX());assertEquals(0f,head.getTranslationY(),0);assertEquals(0f,times.getTranslationX(),0);assertEquals(-pan.getScrollY(),times.getTranslationY(),0);
        ViewGroup firstDay=(ViewGroup)((ViewGroup)pan.getChildAt(0)).getChildAt(0);ViewGroup body=(ViewGroup)firstDay.getChildAt(0);assertNotNull(body.getChildAt(0).getBackground());
    }
    @Test public void eventColorsClipAtMidnightAndSeparateOverlaps()throws Exception{
        LocalDate day=LocalDate.of(2099,1,2);JSONObject first=Data.make("event","跨天").put("start_at",day.minusDays(1).atTime(23,0).toString()).put("end_at",day.atTime(1,0).toString());JSONObject second=Data.make("event","重叠").put("start_at",day.atTime(0,30).toString()).put("end_at",day.atTime(2,0).toString());Data.get(activity).save(first);Data.get(activity).save(second);
        java.util.List<DayEvents.Segment> events=DayEvents.load(activity,day);assertEquals(2,events.size());assertEquals(0,events.get(0).start);assertEquals(60,events.get(0).end);assertEquals(30,events.get(1).start);assertEquals(120,events.get(1).end);assertNotEquals(events.get(0).lane,events.get(1).lane);assertNotEquals(events.get(0).color,events.get(1).color);assertEquals("00:00–01:00 · 跨天",events.get(0).label());assertNotNull(DayEvents.widget(activity,day));
    }
    @Test public void widgetCompletionUpdatesDataWithoutOpeningActivity()throws Exception{
        JSONObject record=Data.make("task","组件勾选");Data.get(activity).save(record);org.robolectric.shadows.ShadowApplication app=shadowOf((Application)activity.getApplicationContext());while(app.getNextStartedActivity()!=null){}
        new WidgetActions().onReceive(activity,new Intent().putExtra("record_id",record.getString("id")).putExtra("complete",true));WidgetActions.worker.submit(()->{}).get(15,TimeUnit.SECONDS);assertFalse(Data.get(activity).find(record.getString("id")).optString("completed").isEmpty());assertNull(app.getNextStartedActivity());
    }

}
