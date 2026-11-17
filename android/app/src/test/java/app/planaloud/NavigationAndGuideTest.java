package app.planaloud;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
public class NavigationAndGuideTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private SharedPreferences prefs;
    @Before public void clear(){prefs=RuntimeEnvironment.getApplication().getSharedPreferences("settings",0);prefs.edit().clear().commit();}
    @After public void close(){if(controller!=null)controller.pause().stop().destroy();}
    private void launch(boolean existing){
        if(existing)prefs.edit().putString("device_id","existing-device").commit();
        controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();layout();
    }
    private void layout(){shadowOf(Looper.getMainLooper()).idle();View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,1920);}
    private <T extends View>T find(View root,Class<T> type){if(type.isInstance(root))return type.cast(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){T value=find(((ViewGroup)root).getChildAt(i),type);if(value!=null)return value;}return null;}
    private TextView text(View root,String text){if(root instanceof TextView&&text.contentEquals(((TextView)root).getText()))return (TextView)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){TextView value=text(((ViewGroup)root).getChildAt(i),text);if(value!=null)return value;}return null;}
    private void click(String label){TextView button=text(activity.getWindow().getDecorView(),label);assertNotNull(label,button);assertTrue(label,button.isShown());button.performClick();layout();}
    @Test public void switchingPagesRetainsMenuItemsAndOneCheckedDestination(){
        launch(true);BottomNavigationView nav=find(activity.getWindow().getDecorView(),BottomNavigationView.class);
        assertNotNull(nav);MenuItem today=nav.getMenu().findItem(100),calendar=nav.getMenu().findItem(102),settings=nav.getMenu().findItem(104);
        for(int destination:new int[]{104,102,100,102,104,100}){
            nav.setSelectedItemId(destination);layout();
            assertSame(today,nav.getMenu().findItem(100));assertSame(calendar,nav.getMenu().findItem(102));assertSame(settings,nav.getMenu().findItem(104));
            assertEquals(destination,nav.getSelectedItemId());int checked=0;
            for(int i=0;i<nav.getMenu().size();i++)if(nav.getMenu().getItem(i).isChecked())checked++;
            assertEquals(1,checked);assertTrue(nav.getMenu().findItem(destination).isChecked());
        }
        activity.home();layout();assertSame(today,nav.getMenu().findItem(100));
        assertNull(nav.getMenu().findItem(101));assertNull(nav.getMenu().findItem(103));
    }
    private android.graphics.Rect contentBounds()throws Exception{
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("content");field.setAccessible(true);android.graphics.Rect bounds=new android.graphics.Rect();assertTrue(((View)field.get(activity)).getGlobalVisibleRect(bounds));return bounds;
    }
    private void gesture(float x,float y,float endX,float endY){
        long time=android.os.SystemClock.uptimeMillis();
        for(int i=0;i<3;i++){
            int action=i==0?MotionEvent.ACTION_DOWN:i==1?MotionEvent.ACTION_MOVE:MotionEvent.ACTION_UP;
            MotionEvent event=MotionEvent.obtain(time,time+i*80,action,i==0?x:endX,i==0?y:endY,0);
            activity.dispatchTouchEvent(event);event.recycle();
        }
        layout();
    }
    @Test public void dispatchedSwipesUseVisiblePagesAndKeepSelection()throws Exception{
        launch(true);BottomNavigationView nav=find(activity.getWindow().getDecorView(),BottomNavigationView.class);
        for(int id:new int[]{100,102,104}){
            android.graphics.Rect bounds=contentBounds();float x=bounds.centerX()+120,y=bounds.top+Ui.dp(activity,6);gesture(x,y,x-240,y);
            assertEquals(id,nav.getSelectedItemId());assertTrue(nav.getMenu().findItem(id).isChecked());
        }
        android.graphics.Rect bounds=contentBounds();float x=bounds.centerX()-120,y=bounds.top+Ui.dp(activity,6);gesture(x,y,x+240,y);assertEquals(102,nav.getSelectedItemId());
    }
    @Test public void verticalDragAndBottomBarDoNotBecomePageSwipes()throws Exception{
        launch(true);BottomNavigationView nav=find(activity.getWindow().getDecorView(),BottomNavigationView.class);
        android.graphics.Rect bounds=contentBounds();long time=android.os.SystemClock.uptimeMillis();float x=bounds.centerX(),y=bounds.top+100;
        int[] actions={MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP};
        float[] xs={x,x+1,x-240,x-240},ys={y,y+80,y+80,y+80};
        for(int i=0;i<actions.length;i++){MotionEvent event=MotionEvent.obtain(time,time+i*60,actions[i],xs[i],ys[i],0);activity.dispatchTouchEvent(event);event.recycle();}
        layout();assertEquals(100,nav.getSelectedItemId());
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("weekPreview");field.setAccessible(true);assertFalse(field.getBoolean(activity));
        nav.getGlobalVisibleRect(bounds);gesture(bounds.centerX(),bounds.centerY(),bounds.centerX()-240,bounds.centerY());assertFalse(field.getBoolean(activity));
    }
    @Test public void firstInstallGuideCanBeSkippedAndDoesNotReturnAfterRecreation(){
        launch(false);assertNotNull(activity.onboarding);assertTrue(Onboarding.pending(prefs));click("跳过");
        assertNull(activity.onboarding);assertTrue(prefs.getBoolean("onboarding_completed",false));assertFalse(prefs.getBoolean("sync_auto",false));
        controller.recreate();activity=controller.get();layout();assertNull(activity.onboarding);
    }
    @Test public void existingInstallIsNotInterruptedAndGuideCanBeReplayed(){
        launch(true);assertNull(activity.onboarding);prefs.edit().putBoolean("sync_auto",true).commit();Onboarding.show(activity);layout();click("跳过");assertTrue(prefs.getBoolean("sync_auto",false));
    }
    @Test public void cloudChoiceDoesNotEnableSyncBeforeConfiguration(){
        launch(false);click("云端同步");click("下一步");controller.recreate();activity=controller.get();layout();
        assertNotNull(text(activity.getWindow().getDecorView(),"先认识几个常用操作"));click("下一步");click("稍后配置，先开始使用");
        assertEquals("cloud",prefs.getString("usage_mode",""));assertFalse(prefs.getBoolean("sync_auto",false));assertNull(activity.onboarding);
    }
    @Test public void removedLocalVoiceFilesAreCleanedWithoutDeletingOtherUserFiles()throws Exception{
        java.io.File directory=new java.io.File(RuntimeEnvironment.getApplication().getFilesDir(),"voice");directory.mkdirs();
        java.io.File model=new java.io.File(directory,"ggml-model.bin"),unrelated=new java.io.File(directory,"notes.txt");
        java.nio.file.Files.write(model.toPath(),new byte[]{1});java.nio.file.Files.write(unrelated.toPath(),new byte[]{2});
        prefs.edit().putInt("voice_provider",2).commit();app.planaloud.voice.VoiceSettings.migrate(RuntimeEnvironment.getApplication());
        assertFalse(model.exists());assertTrue(unrelated.exists());assertEquals(3,prefs.getInt("voice_provider",-1));
    }
    @Test public void localChoiceCompletesGuideWithoutAccountOrNetworkSetup(){
        launch(false);click("下一步");click("下一步");click("开始本地使用");assertEquals("local",prefs.getString("usage_mode",""));assertNull(activity.onboarding);
    }
}
