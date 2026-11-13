package app.planaloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.Spinner;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.utilities.Hct;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ThemeColors {
    private static final List<String> SOURCES = Arrays.asList("provider", "system", "wallpaper", "custom", "default");
    private static final int DEFAULT_SEED = 0xff386a5c;

    private static String source(SharedPreferences prefs) {
        String explicit = prefs.getString("color_source", "");
        if ("provider".equals(explicit)) return explicit;
        if (!prefs.getBoolean("dynamic_color", true)) return "default";
        return explicit.isEmpty() ? "provider" : explicit;
    }

    static boolean usesProvider(Context context) {
        return "provider".equals(source(context.getSharedPreferences("settings", 0)));
    }

    static void extract(Context context, Bitmap bitmap) {
        Bitmap sample = Bitmap.createScaledBitmap(bitmap, 96, 96, true);
        try {
            Integer seed = new DynamicColorsOptions.Builder().setContentBasedSource(sample).build().getContentBasedSeedColor();
            if (seed != null) context.getSharedPreferences("settings", 0).edit().putInt("wallpaper_seed", seed).apply();
        } finally {
            if (sample != bitmap) sample.recycle();
        }
    }

    private static boolean extractWallpaper(Context context) {
        File file = new File(context.getFilesDir(), "wallpaper.jpg");
        if (!file.isFile()) return false;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 16;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        if (bitmap == null) return false;
        try {
            extract(context, bitmap);
            return true;
        } finally {
            bitmap.recycle();
        }
    }

    static void apply(MainActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("settings", 0);
        String source = source(prefs);
        if (source.equals("provider")) {
            activity.getTheme().applyStyle(ProviderAppearance.current(prefs).style, true);
            return;
        }
        if (source.equals("default") || Build.VERSION.SDK_INT < 31) return;

        if (source.equals("wallpaper") && !prefs.contains("wallpaper_seed")) extractWallpaper(activity);
        DynamicColorsOptions.Builder options = new DynamicColorsOptions.Builder();
        if (source.equals("wallpaper") && prefs.contains("wallpaper_seed"))
            options.setContentBasedSource(prefs.getInt("wallpaper_seed", DEFAULT_SEED));
        else if (source.equals("custom")) options.setContentBasedSource(prefs.getInt("custom_seed", DEFAULT_SEED));
        DynamicColors.applyToActivityIfAvailable(activity, options.build());
    }

    static int accent(Context context, boolean dark) {
        SharedPreferences prefs = context.getSharedPreferences("settings", 0);
        String source = source(prefs);
        if (source.equals("provider")) {
            int night = dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
            if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != night) {
                Configuration config = new Configuration();
                config.uiMode = night;
                context = context.createConfigurationContext(config);
            }
            return context.getColor(ProviderAppearance.current(prefs).accent);
        }
        if (Build.VERSION.SDK_INT >= 31 && !source.equals("default")) {
            if (source.equals("custom") || source.equals("wallpaper") && prefs.contains("wallpaper_seed")) {
                int seed = prefs.getInt(source.equals("custom") ? "custom_seed" : "wallpaper_seed", DEFAULT_SEED);
                Hct color = Hct.fromInt(seed);
                return Hct.from(color.getHue(), color.getChroma(), dark ? 80 : 40).toInt();
            }
            return context.getColor(dark ? android.R.color.system_accent1_200 : android.R.color.system_accent1_700);
        }
        return dark ? 0xffa0d0bf : DEFAULT_SEED;
    }

    static void settings(MainActivity activity, LinearLayout page) {
        SharedPreferences prefs = activity.prefs;
        Spinner source = TaskTypes.spinner(activity, page, "强调色来源",
                Arrays.asList("图标配色", "系统壁纸", "应用内壁纸", "自定义颜色", "序言经典色"));
        source.setSelection(Math.max(0, SOURCES.indexOf(source(prefs))));
        TextInputEditText seed = Ui.field(activity, page, "自定义颜色（#RRGGBB）",
                String.format(Locale.ROOT, "#%06X", prefs.getInt("custom_seed", DEFAULT_SEED) & 0xffffff), false);
        page.addView(Ui.help(activity, "图标配色随当前图标切换，并适配深浅主题。壁纸和自定义动态取色需要 Android 12 及以上。"));
        LinearLayout colors=Ui.row(activity);page.addView(colors);
        Ui.cell(activity,colors,Ui.iconButton(activity, "应用配色", android.R.drawable.ic_menu_manage, false, () -> {
            String selected = SOURCES.get(source.getSelectedItemPosition());
            SharedPreferences.Editor editor = prefs.edit();
            if (selected.equals("custom")) {
                String color = seed.getText().toString().trim();
                if (!color.matches("#[0-9a-fA-F]{6}")) {
                    seed.setError("请填写 #RRGGBB 颜色");
                    return;
                }
                editor.putInt("custom_seed", Color.parseColor(color));
            }
            if (selected.equals("wallpaper") && !extractWallpaper(activity)) {
                activity.message("请先选择可读取的应用壁纸");
                return;
            }
            editor.putString("color_source", selected).putBoolean("dynamic_color", !selected.equals("default")).apply();
            AgendaWidget.refresh(activity);
            activity.recreate();
        }));
        Ui.cell(activity,colors,Ui.iconButton(activity,"壁纸轮播",android.R.drawable.ic_menu_rotate,true,()->WallpaperScreen.show(activity)));
        LinearLayout wallpaper=Ui.row(activity);page.addView(wallpaper);
        Ui.cell(activity,wallpaper,Ui.iconButton(activity,"选择壁纸",android.R.drawable.ic_menu_gallery,false,()->activity.pickFile(104,"image/*")));
        Ui.cell(activity,wallpaper,Ui.iconButton(activity,"恢复背景",android.R.drawable.ic_menu_revert,false,()->{
            new java.io.File(activity.getFilesDir(),"wallpaper.jpg").delete();
            prefs.edit().remove("wallpaper_seed").apply();
            AgendaWidget.refresh(activity);activity.recreate();
        }));
    }
}
