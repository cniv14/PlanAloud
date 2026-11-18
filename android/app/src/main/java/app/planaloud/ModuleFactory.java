package app.planaloud;
import android.app.Activity;import android.content.Intent;
/** Manifest components remain fixed in the base; optional patches supply implementation classes. */
// Android reads appComponentFactory only on API 28+; older versions use the built-in classes.
@android.annotation.TargetApi(28)
public final class ModuleFactory extends android.app.AppComponentFactory {
 public Activity instantiateActivity(ClassLoader cl,String name,Intent intent)throws InstantiationException,IllegalAccessException,ClassNotFoundException{
  ClassLoader module=ModuleUpdates.loader(name);if(module!=null)try{return super.instantiateActivity(module,name,intent);}catch(Throwable e){String id=name.substring("app.planaloud.".length(),name.lastIndexOf('.'));ModuleUpdates.disable(id,"启动异常，已使用内置版本");}
  return super.instantiateActivity(cl,name,intent);
 }
}
