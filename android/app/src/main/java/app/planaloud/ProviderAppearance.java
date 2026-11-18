package app.planaloud;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

enum ProviderAppearance {
    DEEPSEEK("DEEPSEEK", "DeepSeek", R.mipmap.ic_launcher, R.style.ProviderPalette_deepseek, R.color.provider_deepseek_primary, "deepseek", "deepseek.com"),
    CLAUDE("CLAUDE", "Claude", R.mipmap.ic_launcher_claude, R.style.ProviderPalette_claude, R.color.provider_claude_primary, "claude|anthropic", "anthropic.com"),
    GEMINI("GEMINI", "Gemini", R.mipmap.ic_launcher_gemini, R.style.ProviderPalette_gemini, R.color.provider_gemini_primary, "gemini|gemma", "generativelanguage.googleapis.com"),
    QWEN("QWEN", "Qwen", R.mipmap.ic_launcher_qwen, R.style.ProviderPalette_qwen, R.color.provider_qwen_primary, "qwen[0-9]*|qwq|qvq|千问|通义|百炼", "dashscope.aliyuncs.com"),
    SEED("SEED", "豆包 Seed", R.mipmap.ic_launcher_seed, R.style.ProviderPalette_seed, R.color.provider_seed_primary, "doubao|seed|豆包", "volces.com"),
    KIMI("KIMI", "Kimi", R.mipmap.ic_launcher_kimi, R.style.ProviderPalette_kimi, R.color.provider_kimi_primary, "kimi|moonshot", "moonshot.cn", "moonshot.ai"),
    GLM("GLM", "GLM", R.mipmap.ic_launcher_glm, R.style.ProviderPalette_glm, R.color.provider_glm_primary, "glm[0-9]*|chatglm[0-9]*|zhipu|智谱", "bigmodel.cn", "z.ai"),
    GROK("GROK", "Grok", R.mipmap.ic_launcher_grok, R.style.ProviderPalette_grok, R.color.provider_grok_primary, "grok[0-9]*|xai", "x.ai"),
    MINIMAX("MINIMAX", "MiniMAX", R.mipmap.ic_launcher_minimax, R.style.ProviderPalette_minimax, R.color.provider_minimax_primary, "minimax|abab[0-9]*", "minimax.io", "minimaxi.com"),
    LLAMA("LLAMA", "Llama", R.mipmap.ic_launcher_llama, R.style.ProviderPalette_llama, R.color.provider_llama_primary, "llama[0-9]*"),
    COPILOT("COPILOT", "Copilot", R.mipmap.ic_launcher_copilot, R.style.ProviderPalette_copilot, R.color.provider_copilot_primary, "copilot", "copilot.microsoft.com", "githubcopilot.com"),
    GPT("GPT", "GPT", R.mipmap.ic_launcher_gpt, R.style.ProviderPalette_gpt, R.color.provider_gpt_primary, "gpt[0-9]*|chatgpt|openai|o[1-9]", "openai.com");

    private static final String KEY = "launcher_appearance";
    private static final String AUTO = "auto";
    final String id;
    final String label;
    final int icon, style, accent;
    private final Pattern modelFamily;
    private final String[] domains;

    ProviderAppearance(String id, String label, int icon, int style, int accent, String family, String... domains) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.style = style;
        this.accent = accent;
        modelFamily = Pattern.compile("(?:^|[^a-z0-9])(?:" + family + ")(?:$|[^a-z0-9])");
        this.domains = domains;
    }

    static ProviderAppearance infer(String provider, String model, String endpoint) {
        // Try the model identifier before the gateway/vendor prefix in namespaced IDs.
        String id = model.toLowerCase(Locale.ROOT);
        String leaf = id.substring(id.lastIndexOf('/') + 1);
        for (ProviderAppearance theme : values()) if (theme.modelFamily.matcher(leaf).find()) return theme;
        String vendor = provider.toLowerCase(Locale.ROOT);
        for (ProviderAppearance theme : values()) if (theme.modelFamily.matcher(vendor).find()) return theme;
        for (ProviderAppearance theme : values()) if (theme.modelFamily.matcher(id).find()) return theme;
        String host = android.net.Uri.parse(endpoint).getHost();
        if (host != null) {
            host = host.toLowerCase(Locale.ROOT);
            for (ProviderAppearance theme : values()) {
                for (String domain : theme.domains) if (host.equals(domain) || host.endsWith("." + domain)) return theme;
            }
        }
        return DEEPSEEK;
    }

    static ProviderAppearance current(SharedPreferences prefs) {
        String choice = prefs.getString(KEY, AUTO);
        for (ProviderAppearance theme : values()) if (theme.id.equals(choice)) return theme;
        return infer(prefs.getString("ai_provider", ""), prefs.getString("model", ""), prefs.getString("api_url", ""));
    }

    private ComponentName launcher(Context context) {
        return new ComponentName(context, context.getPackageName() + ".Launcher_" + id);
    }

    private static boolean enabled(Context c,ProviderAppearance theme){int state=c.getPackageManager().getComponentEnabledSetting(theme.launcher(c));return state==PackageManager.COMPONENT_ENABLED_STATE_ENABLED||state==PackageManager.COMPONENT_ENABLED_STATE_DEFAULT&&theme==DEEPSEEK;}
    private static boolean applied(Context c,ProviderAppearance selected){for(ProviderAppearance theme:values())if(enabled(c,theme)!=(theme==selected))return false;return true;}
    static boolean syncLauncher(Context context,SharedPreferences prefs){
        ProviderAppearance selected=current(prefs);PackageManager manager=context.getPackageManager();int flags=PackageManager.DONT_KILL_APP|(Build.VERSION.SDK_INT>=30?PackageManager.SYNCHRONOUS:0);
        try{
            manager.getActivityInfo(selected.launcher(context),PackageManager.MATCH_DISABLED_COMPONENTS);
            if(applied(context,selected))return true;
            if(Build.VERSION.SDK_INT>=33)try{List<PackageManager.ComponentEnabledSetting> changes=new ArrayList<>();for(ProviderAppearance theme:values())if(enabled(context,theme)!=(theme==selected))changes.add(new PackageManager.ComponentEnabledSetting(theme.launcher(context),theme==selected?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED,flags));if(!changes.isEmpty())manager.setComponentEnabledSettings(changes);}catch(RuntimeException atomicFailure){Log.w("ProviderAppearance","Retrying icon change with individual component settings",atomicFailure);}
            if(!applied(context,selected)){manager.setComponentEnabledSetting(selected.launcher(context),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,flags);for(ProviderAppearance theme:values())if(theme!=selected&&enabled(context,theme))manager.setComponentEnabledSetting(theme.launcher(context),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,flags);}
            if(!applied(context,selected))throw new IllegalStateException("桌面入口状态没有成功更新");return true;
        }catch(PackageManager.NameNotFoundException|RuntimeException error){Diagnostics.record(context,"launcher-icon",error);return false;}
    }

    static void show(MainActivity activity) {
        LinearLayout page = activity.fullPage("应用图标", () -> new SettingsPanels(activity).appearance());
        page.addView(Ui.text(activity, "应用图标", 22, true));
        SharedPreferences prefs = activity.prefs;
        ProviderAppearance selected = current(prefs);
        boolean automatic = AUTO.equals(prefs.getString(KEY, AUTO));
        page.addView(Ui.tile(activity, "跟随 AI 厂商和模型",
                automatic ? "已选 · " + selected.label : "保存 AI 配置后自动切换图标",
                () -> select(activity, AUTO)));
        page.addView(Ui.help(activity, "点选图标可固定使用。强调色来源设为「图标配色」时，应用颜色也会一起切换。"));

        int columns = activity.getResources().getConfiguration().screenWidthDp >= 360 ? 3 : 2;
        ProviderAppearance[] options = values();
        for (int first = 0; first < options.length; first += columns) {
            LinearLayout row = Ui.row(activity);
            page.addView(row);
            for (int i = first; i < Math.min(first + columns, options.length); i++) {
                ProviderAppearance theme = options[i];
                boolean checked = !automatic && theme == selected;
                MaterialCardView card = Ui.card(activity);
                LinearLayout content = Ui.column(activity);
                content.setGravity(Gravity.CENTER);
                content.setPadding(Ui.dp(activity, 4), Ui.dp(activity, 10), Ui.dp(activity, 4), Ui.dp(activity, 10));
                ImageView image = new ImageView(activity);
                image.setImageResource(theme.icon);
                content.addView(image, new LinearLayout.LayoutParams(Ui.dp(activity, 54), Ui.dp(activity, 54)));
                TextView label = Ui.text(activity, theme.label, 14, checked);
                label.setGravity(Gravity.CENTER);
                content.addView(label);
                card.addView(content);
                card.setContentDescription(theme.label + (checked ? "，已选" : ""));
                if (checked) {
                    card.setStrokeColor(Ui.primary(activity));
                    card.setStrokeWidth(Ui.dp(activity, 2));
                }
                card.setOnClickListener(view -> select(activity, theme.id));
                Ui.cell(activity, row, card);
            }
        }
    }

    static void select(MainActivity activity,String choice){
        String old=activity.prefs.getString(KEY,AUTO);
        activity.job(()->{if(!activity.prefs.edit().putString(KEY,choice).commit())throw new java.io.IOException("无法保存图标选择");if(!syncLauncher(activity,activity.prefs)){activity.prefs.edit().putString(KEY,old).commit();syncLauncher(activity,activity.prefs);throw new java.io.IOException("图标没有成功切换，请重试；详细原因已记录到诊断日志");}return "已应用 "+current(activity.prefs).label+" 图标";},activity::recreate);
    }
}
