package app.planaloud;
import android.app.Application;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class LauncherIconTest {
    Context c;android.content.SharedPreferences p;
    @Before public void start(){c=RuntimeEnvironment.getApplication();p=c.getSharedPreferences("settings",0);p.edit().clear().commit();}
    private void select(ProviderAppearance choice){p.edit().putString("launcher_appearance",choice.id).commit();assertTrue(ProviderAppearance.syncLauncher(c,p));for(ProviderAppearance theme:ProviderAppearance.values()){int state=c.getPackageManager().getComponentEnabledSetting(new ComponentName(c,c.getPackageName()+".Launcher_"+theme.id));boolean enabled=state==PackageManager.COMPONENT_ENABLED_STATE_ENABLED||state==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&theme==ProviderAppearance.DEEPSEEK;assertEquals(theme.id,theme==choice,enabled);}}
    @Test public void everyIconUsesAStableManifestIdAndCanSwitchBothWays(){for(ProviderAppearance theme:ProviderAppearance.values()){assertEquals(theme.name(),theme.id);select(theme);}select(ProviderAppearance.DEEPSEEK);}
    @Test public void actualComponentStateIsRecheckedAfterExternalChanges(){select(ProviderAppearance.GPT);c.getPackageManager().setComponentEnabledSetting(new ComponentName(c,c.getPackageName()+".Launcher_GPT"),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);select(ProviderAppearance.GPT);}
    @Test @Config(sdk=33) public void android13BatchSwitchAndThemedMasksAreDistinct()throws Exception{select(ProviderAppearance.CLAUDE);select(ProviderAppearance.GEMINI);Set<Integer> masks=new HashSet<>();for(ProviderAppearance theme:ProviderAppearance.values())try(XmlResourceParser xml=c.getResources().getXml(theme.icon)){int value=0;StringBuilder trace=new StringBuilder();while(xml.next()!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){trace.append(xml.getName()).append(" ");if("monochrome".equals(xml.getName())){for(int i=0;i<xml.getAttributeCount();i++)trace.append(xml.getAttributeName(i)).append("=").append(xml.getAttributeValue(i));value=xml.getAttributeResourceValue("http://schemas.android.com/apk/res/android","drawable",0);}}assertNotEquals(theme.id+" SDK="+android.os.Build.VERSION.SDK_INT+" "+c.getResources().getConfiguration()+" "+trace,0,value);assertTrue(theme.id,masks.add(value));}}
}
