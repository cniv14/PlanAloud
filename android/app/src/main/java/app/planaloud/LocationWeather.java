package app.planaloud;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.*;

/** Foreground single-shot location, using precise coordinates when authorized. */
public final class LocationWeather {
    private final MainActivity a;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LocationListener listener;
    private boolean done=true,precise;
    private Location best;
    private int generation;
    public LocationWeather(MainActivity a){this.a=a;}
    public void requestPrecise(){
        if(a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},201);return;}
        request();
    }
    public void request(){
        if(a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPrecise();return;}
        cancel();done=false;best=null;int request=++generation;
        precise=a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        LocationManager manager=(LocationManager)a.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers=new ArrayList<>();
        if(precise&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER))providers.add(LocationManager.GPS_PROVIDER);
        if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))providers.add(LocationManager.NETWORK_PROVIDER);
        if(providers.isEmpty()){done=true;a.message("请开启系统定位，或手动设置天气经纬度");return;}
        for(String provider:providers)try{
            Location loc=manager.getLastKnownLocation(provider);
            if(fresh(loc)&&better(loc,best))best=loc;
        }catch(SecurityException ignored){}
        if(accurate(best)){finish();return;}
        a.message(precise?"正在获取精确位置…":"正在获取大致位置，可在系统权限中开启精确位置");
        handler.postDelayed(()->{if(!done&&request==generation){if(best!=null)finish();else{cancel();a.message("定位暂不可用，请重试或手动设置经纬度");}}},20000);
        listener=new LocationListener(){
            public void onLocationChanged(Location loc){if(done||request!=generation||!fresh(loc))return;if(better(loc,best))best=loc;if(accurate(best))finish();}
            public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}
        };
        for(String provider:providers)try{
            // Updates allow a rough first network fix to improve while GPS acquires a fix.
            manager.requestLocationUpdates(provider,1000,0,listener,Looper.getMainLooper());
        }catch(SecurityException|IllegalArgumentException ignored){}
    }
    private static boolean fresh(Location loc){return loc!=null&&loc.hasAccuracy()&&SystemClock.elapsedRealtimeNanos()-loc.getElapsedRealtimeNanos()<120L*1000000000;}
    private static boolean better(Location loc,Location previous){return previous==null||loc.getAccuracy()<previous.getAccuracy();}
    private boolean accurate(Location loc){return loc!=null&&loc.getAccuracy()<=(precise?200:5000);}
    private void finish(){Location location=best;cancel();load(location);}
    private void load(Location location){a.job(()->{String city="当前位置";if(Geocoder.isPresent()){
        java.util.concurrent.FutureTask<String> resolve=new java.util.concurrent.FutureTask<>(()->{List<Address> addresses=new Geocoder(a,Locale.CHINA).getFromLocation(location.getLatitude(),location.getLongitude(),1);if(addresses!=null&&!addresses.isEmpty()){Address address=addresses.get(0);LinkedHashSet<String> parts=new LinkedHashSet<>();for(String part:new String[]{address.getLocality(),address.getSubLocality(),address.getThoroughfare()})if(part!=null&&!part.isEmpty())parts.add(part);if(!parts.isEmpty())return String.join(" · ",parts);if(address.getSubAdminArea()!=null)return address.getSubAdminArea();}return "当前位置";});Thread thread=new Thread(resolve,"weather-location-name");thread.setDaemon(true);thread.start();try{city=resolve.get(3,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception ignored){resolve.cancel(true);}
        }return PlannerServices.weather(a,location.getLatitude(),location.getLongitude(),city);},a::refreshWeatherUi);}
    public void cancel(){handler.removeCallbacksAndMessages(null);generation++;done=true;if(listener!=null)try{((LocationManager)a.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(listener);}catch(SecurityException ignored){}listener=null;}
}
