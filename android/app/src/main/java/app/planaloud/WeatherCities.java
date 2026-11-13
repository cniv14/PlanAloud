package app.planaloud;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

final class WeatherCities {
    static JSONArray search(Context context, String query) throws Exception {
        Exception unavailable = null;
        JSONArray matches = new JSONArray();
        String encoded = URLEncoder.encode(query, "UTF-8");
        if (OpenWeatherSources.usesServer(context)) try {
            JSONArray results = OpenWeatherSources.server(context, "kind=cities&q=" + encoded)
                    .getJSONObject("data").optJSONArray("results");
            if (results != null && results.length() > 0) return results;
        } catch (IOException | org.json.JSONException e) {
            unavailable = e;
        }
        try {
            JSONObject response = OpenWeatherSources.json(context,
                    "https://geocoding-api.open-meteo.com/v1/search?name="
                    + encoded + "&count=8&language=zh&format=json");
            if (response.optBoolean("error")) throw new IOException("城市搜索暂不可用");
            unavailable = null;
            JSONArray results = response.optJSONArray("results");
            if (results != null && results.length() > 0) return results;
        } catch (IOException | org.json.JSONException e) {
            unavailable = e;
        }
        try {
            for (Address address : systemSearch(context, query)) {
                if (!address.hasLatitude() || !address.hasLongitude()) continue;
                String name = address.getLocality();
                if (name == null || name.isEmpty()) name = address.getFeatureName();
                if (name == null || name.isEmpty()) name = query;
                matches.put(new JSONObject().put("name", name)
                        .put("admin1", address.getAdminArea()).put("country", address.getCountryName())
                        .put("latitude", address.getLatitude()).put("longitude", address.getLongitude()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            if (unavailable == null) unavailable = e;
        }
        if (matches.length() == 0 && unavailable != null)
            throw new IOException("城市搜索暂不可用，可使用当前位置或输入经纬度。", unavailable);
        return matches;
    }

    private static List<Address> systemSearch(Context context, String query) throws Exception {
        if (!Geocoder.isPresent()) return Collections.emptyList();
        Geocoder geocoder = new Geocoder(context, Locale.CHINA);
        if (Build.VERSION.SDK_INT >= 33) {
            CompletableFuture<List<Address>> result = new CompletableFuture<>();
            geocoder.getFromLocationName(query, 5, new Geocoder.GeocodeListener() {
                public void onGeocode(List<Address> addresses) { result.complete(addresses); }
                public void onError(String errorMessage) { result.completeExceptionally(new IOException("系统城市搜索暂不可用")); }
            });
            return result.get(4, TimeUnit.SECONDS);
        }
        FutureTask<List<Address>> task = new FutureTask<>(() -> geocoder.getFromLocationName(query, 5));
        Thread thread = new Thread(task, "weather-city-search");
        thread.setDaemon(true);
        thread.start();
        try {
            List<Address> addresses = task.get(4, TimeUnit.SECONDS);
            return addresses == null ? Collections.emptyList() : addresses;
        } finally {
            task.cancel(true);
        }
    }
}
