package app.planaloud;
import android.app.Application;
import android.content.*;
import android.os.Looper;
import app.planaloud.ui.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class) @LooperMode(LooperMode.Mode.PAUSED)
public class DisplayTrialTest {
    Context c;SharedPreferences p;ActivityController<MainActivity> controller;
    @Before public void start(){c=RuntimeEnvironment.getApplication();p=c.getSharedPreferences("settings",0);p.edit().clear().putString("device_id","existing").commit();}
    @After public void close(){DisplayTrial.revert(c);if(controller!=null)controller.pause().stop().destroy();}
    @Test public void expiryRestoresOldValuesBeforeContextWrapping(){int density=c.getResources().getConfiguration().densityDpi;assertTrue(DisplayTrial.begin(c,70,160,"neo"));org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(11));Context wrapped=UiScale.wrap(c);assertEquals(density,wrapped.getResources().getConfiguration().densityDpi);assertFalse(p.contains("ui_scale"));assertFalse(p.contains("font_scale"));assertFalse(p.contains("ui_style"));assertFalse(DisplayTrial.pending(c));}
    @Test public void explicitConfirmationKeepsValuesAfterDeadline(){DisplayTrial.begin(c,80,120,"neo");assertTrue(DisplayTrial.confirm(c));org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(11));UiScale.wrap(c);assertEquals(80,p.getInt("ui_scale",0));assertEquals(120,p.getInt("font_scale",0));assertEquals("neo",p.getString("ui_style",""));assertFalse(DisplayTrial.pending(c));}
    @Test public void lateConfirmationIsRejectedAndReverts(){DisplayTrial.begin(c,80,140,"neo");org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(10));assertFalse(DisplayTrial.confirm(c));assertEquals(100,p.getInt("ui_scale",100));assertEquals("classic",p.getString("ui_style","classic"));}
    @Test public void aSecondTrialNeverUsesAnUnconfirmedConfigurationAsBackup(){DisplayTrial.begin(c,70,160,"neo");DisplayTrial.begin(c,110,90,"neo");DisplayTrial.revert(c);assertEquals(100,p.getInt("ui_scale",100));assertEquals(100,p.getInt("font_scale",100));assertEquals("classic",p.getString("ui_style","classic"));}
    @Test public void promptTimeoutRestoresWithoutAnyClick(){DisplayTrial.begin(c,80,120,"neo");controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=controller.get();assertNotNull(a.displayTrial);shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(11));assertFalse(DisplayTrial.pending(c));assertEquals(100,p.getInt("ui_scale",100));assertEquals("classic",p.getString("ui_style","classic"));}
    @Test public void recreationAndBackgroundTimeDoNotRestartTenSeconds(){DisplayTrial.begin(c,80,120,"classic");controller=Robolectric.buildActivity(MainActivity.class).setup();long deadline=p.getLong("display_trial_elapsed",0);shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(4));controller.recreate();assertEquals(deadline,p.getLong("display_trial_elapsed",0));assertTrue(DisplayTrial.remaining(c)<=6000);org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(7));assertTrue(DisplayTrial.recoverIfExpired(c));assertFalse(DisplayTrial.pending(c));}
}
