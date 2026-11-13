package app.planaloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.json.JSONObject;

final class Forecast {
    static String summary(Context c, LocalDate day) {
        SharedPreferences p = c.getSharedPreferences("settings", 0);
        try {
            JSONObject cache = new JSONObject(p.getString("forecast", "{}"));
            if (validSource(cache.optString("source"))
                    && cache.optString("lat").equals(p.getString("lat", ""))
                    && cache.optString("lon").equals(p.getString("lon", ""))) {
                String value = cache.getJSONObject("days").optString(day.toString());
                if (!value.isEmpty()) return value + (System.currentTimeMillis() - cache.optLong("updated") > 6 * 3600000L ? " · 缓存待刷新" : "");
            }
        } catch (Exception ignored) {}
        return day.isBefore(LocalDate.now()) ? "暂无历史天气" : day.isAfter(LocalDate.now().plusDays(9)) ? "超出预报范围" : "尚无预报，点此获取";
    }

    private static boolean validSource(String source) {
        return OpenWeatherSources.OPEN_METEO.equals(source) || OpenWeatherSources.MET_NORWAY.equals(source);
    }

    static synchronized String refresh(Context c) throws Exception {
        SharedPreferences p = c.getSharedPreferences("settings", 0);
        String latitude = p.getString("lat", ""), longitude = p.getString("lon", "");
        if (latitude.isEmpty() || longitude.isEmpty()) throw new Exception("请先选择天气城市或授权大致位置");
        return refresh(c, Double.parseDouble(latitude), Double.parseDouble(longitude),
                p.getString("city", "当前位置"), p.getString("weather_timezone", ""));
    }

    static synchronized String refresh(Context c, double lat, double lon, String city, String timezone) throws Exception {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180)
            throw new IllegalArgumentException("无效经纬度");
        lat = Math.round(lat * 100) / 100.0;
        lon = Math.round(lon * 100) / 100.0;
        ZoneId zone = timezone.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(timezone);
        OpenWeatherSources.Reading result = OpenWeatherSources.fetch(c, lat, lon, zone);
        String latitude = Double.toString(lat), longitude = Double.toString(lon);
        long updated = System.currentTimeMillis();
        JSONObject cache = new JSONObject().put("source", result.source).put("days", result.days)
                .put("lat", latitude).put("lon", longitude).put("updated", updated);
        c.getSharedPreferences("settings", 0).edit()
                .putString("lat", latitude).putString("lon", longitude).putString("city", city)
                .putString("weather_timezone", timezone).putString("weather_source", result.source).putBoolean("weather_via_server",result.server)
                .putString("weather_cache", result.current).putString("weather_time", LocalDateTime.now().toString())
                .putLong("weather_updated", updated).putString("forecast", cache.toString()).apply();
        AgendaWidget.refresh(c);
        return "已更新天气 · " + (result.server ? "序言服务端 / " : "") + result.source;
    }

    static void add(MainActivity a, LinearLayout page, LocalDate day, Runnable refresh) {
        page.addView(Ui.button(a, a.prefs.getString("city", "天气") + " · " + summary(a, day), false, () -> {
            if (a.prefs.getString("lat", "").isEmpty()) new SettingsPanels(a).weather();
            else a.job(() -> refresh(a), refresh);
        }));
    }
}
