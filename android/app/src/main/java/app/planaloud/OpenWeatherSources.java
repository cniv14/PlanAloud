package app.planaloud;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

final class OpenWeatherSources {
    static final String OPEN_METEO = "Open-Meteo";
    static final String MET_NORWAY = "MET Norway";
    private static OkHttpClient client;

    static final class Reading {
        final String source;
        final String current;
        final JSONObject days;
        final boolean server;

        Reading(String source, String current, JSONObject days) {
            this(source, current, days, false);
        }

        Reading(String source, String current, JSONObject days, boolean server) {
            this.source = source;
            this.current = current;
            this.days = days;
            this.server = server;
        }
    }

    static Reading fetch(Context c, double lat, double lon, ZoneId zone) throws Exception {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180)
            throw new IOException("无效天气位置");
        String latitude = Double.toString(lat), longitude = Double.toString(lon);
        Callable<Reading> direct = () -> firstAvailable(
                () -> openMeteo(json(c, "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                        + "&longitude=" + longitude + "&timezone=auto&forecast_days=10"
                        + "&current=temperature_2m,weather_code"
                        + "&daily=temperature_2m_min,temperature_2m_max,weather_code,precipitation_probability_max")),
                () -> metNorway(json(c, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat="
                        + latitude + "&lon=" + longitude), zone, Instant.now()));
        if (!usesServer(c)) return direct.call();
        return firstAvailable(() -> {
            JSONObject result = server(c, "kind=forecast&lat=" + latitude + "&lon=" + longitude);
            JSONObject data = result.getJSONObject("data");
            String source = result.getString("source");
            Reading reading;
            if (OPEN_METEO.equals(source)) reading = openMeteo(data);
            else if (MET_NORWAY.equals(source)) reading = metNorway(data, zone, Instant.now());
            else throw new IOException("服务端天气来源未知");
            return new Reading(reading.source, reading.current, reading.days, true);
        }, direct);
    }

    static boolean usesServer(Context c) {
        SharedPreferences p = c.getSharedPreferences("settings", 0);
        return p.getBoolean("weather_use_server", true) && !p.getString("server", "").isEmpty()
                && !p.getString("user_id", "").isEmpty() && c.getSharedPreferences("secrets",0).contains("token") && "dayflow".equals(p.getString("sync_backend", "dayflow"));
    }

    static JSONObject server(Context c, String query) throws Exception {
        SharedPreferences p = c.getSharedPreferences("settings", 0);
        try {
            String token = Secrets.get(c, "token");
            if (token.isEmpty()) throw new IOException("尚未登录序言服务端");
            String url = Network.serverEndpoint(p.getString("server", "")) + "/v1/weather?" + query;
            Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + token).build();
            return execute(client(c).newBuilder().followRedirects(false).callTimeout(10, TimeUnit.SECONDS).build(), request);
        } catch (org.json.JSONException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("无法读取服务端天气", e);
        }
    }

    static Reading firstAvailable(Callable<Reading> primary, Callable<Reading> backup) throws Exception {
        try {
            return primary.call();
        } catch (IOException | org.json.JSONException | java.time.DateTimeException first) {
            try {
                return backup.call();
            } catch (IOException | org.json.JSONException | java.time.DateTimeException second) {
                IOException error = new IOException("天气来源暂不可用，请稍后重试。已有天气缓存会保留。", second);
                error.addSuppressed(first);
                throw error;
            }
        }
    }

    static JSONObject json(Context c, String url) throws Exception {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "PlanAloud/1.4.2 (https://github.com/cniv14/PlanAloud)")
                .header("Accept", "application/json").build();
        return execute(client(c),request);
    }

    private static JSONObject execute(OkHttpClient transport, Request request) throws Exception {
        try (Response response = transport.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null)
                throw new IOException("天气服务 HTTP " + response.code());
            return new JSONObject(Network.read(response.body().byteStream(), 1024 * 1024));
        }
    }

    private static synchronized OkHttpClient client(Context c) {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .cache(new Cache(new File(c.getCacheDir(), "weather-http"), 2 * 1024 * 1024))
                    .connectTimeout(4, TimeUnit.SECONDS).callTimeout(7, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    static Reading openMeteo(JSONObject response) throws Exception {
        JSONObject current = response.getJSONObject("current"), daily = response.getJSONObject("daily");
        String now = wmo(current.getInt("weather_code")) + " " + degrees(current.getDouble("temperature_2m")) + "℃";
        JSONArray dates = daily.getJSONArray("time"), code = daily.getJSONArray("weather_code");
        JSONArray min = daily.getJSONArray("temperature_2m_min"), max = daily.getJSONArray("temperature_2m_max");
        JSONArray rain = daily.optJSONArray("precipitation_probability_max");
        JSONObject days = new JSONObject();
        for (int i = 0; i < dates.length(); i++) {
            if (code.isNull(i) || min.isNull(i) || max.isNull(i)) continue;
            String date = LocalDate.parse(dates.getString(i)).toString();
            int probability = rain == null ? -1 : rain.optInt(i, -1);
            days.put(date, forecast(wmo(code.getInt(i)), min.getDouble(i), max.getDouble(i))
                    + (probability < 0 || probability > 100 ? "" : " · 降水 " + probability + "%"));
        }
        if (days.length() == 0) throw new IOException("Open-Meteo 无有效预报");
        return new Reading(OPEN_METEO, now, days);
    }

    static Reading metNorway(JSONObject response, ZoneId zone, Instant now) throws Exception {
        JSONArray series = response.getJSONObject("properties").getJSONArray("timeseries");
        Map<LocalDate, Daily> ranges = new LinkedHashMap<>();
        LocalDate today = now.atZone(zone).toLocalDate();
        long nearest = Long.MAX_VALUE;
        String current = "";
        for (int i = 0; i < series.length(); i++) {
            JSONObject entry = series.getJSONObject(i);
            Instant timestamp = Instant.parse(entry.getString("time"));
            JSONObject data = entry.getJSONObject("data");
            double temp = data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature");
            java.time.ZonedDateTime local = timestamp.atZone(zone);
            if (local.toLocalDate().isBefore(today)) continue;
            String sky = symbol(data);
            long distance = Math.abs(timestamp.getEpochSecond() - now.getEpochSecond());
            if (distance < nearest) {
                nearest = distance;
                current = sky + " " + degrees(temp) + "℃ · 估算";
            }
            Daily day = ranges.computeIfAbsent(local.toLocalDate(), ignored -> new Daily());
            day.min = Math.min(day.min, temp);
            day.max = Math.max(day.max, temp);
            int noon = Math.abs(local.getHour() - 12);
            if (!"天气".equals(sky) && noon < day.nearestNoon) {
                day.sky = sky;
                day.nearestNoon = noon;
            }
        }
        if (nearest > 6 * 3600L || ranges.isEmpty()) throw new IOException("MET Norway 无有效近期预报");
        JSONObject days = new JSONObject();
        for (Map.Entry<LocalDate, Daily> entry : ranges.entrySet()) {
            Daily day = entry.getValue();
            days.put(entry.getKey().toString(), forecast(day.sky, day.min, day.max));
        }
        return new Reading(MET_NORWAY, current, days);
    }

    private static final class Daily {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        String sky = "天气";
        int nearestNoon = Integer.MAX_VALUE;
    }

    private static String symbol(JSONObject data) {
        for (String field : new String[]{"next_1_hours", "next_6_hours", "next_12_hours"}) {
            JSONObject period = data.optJSONObject(field);
            JSONObject summary = period == null ? null : period.optJSONObject("summary");
            if (summary == null) continue;
            String code = summary.optString("symbol_code");
            if (code.contains("thunder")) return "雷雨";
            if (code.contains("sleet")) return "雨夹雪";
            if (code.contains("snow")) return "雪";
            if (code.contains("rain")) return "雨";
            if (code.startsWith("fog")) return "雾";
            if (code.startsWith("clearsky")) return "晴";
            if (code.startsWith("cloudy")) return "阴";
            if (code.contains("cloudy") || code.startsWith("fair")) return "多云";
        }
        return "天气";
    }

    private static String wmo(int code) {
        if (code == 0) return "晴";
        if (code <= 2) return "多云";
        if (code == 3) return "阴";
        if (code == 45 || code == 48) return "雾";
        if (code >= 51 && code <= 67) return "雨";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "雪";
        if (code >= 80 && code <= 82) return "阵雨";
        if (code >= 95 && code <= 99) return "雷雨";
        return "天气";
    }

    private static long degrees(double value) throws IOException {
        if (!Double.isFinite(value)) throw new IOException("天气温度缺失");
        return Math.round(value);
    }

    private static String forecast(String sky, double min, double max) throws IOException {
        return sky + " " + degrees(min) + "–" + degrees(max) + "℃";
    }

    static String cached(Context c, String fallback) {
        SharedPreferences p = c.getSharedPreferences("settings", 0);
        String source = p.getString("weather_source", "");
        return OPEN_METEO.equals(source) || MET_NORWAY.equals(source)
                ? p.getString("weather_cache", fallback) : fallback;
    }
}
