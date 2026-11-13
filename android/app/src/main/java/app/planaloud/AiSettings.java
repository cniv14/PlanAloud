package app.planaloud;

import android.content.*;
import android.text.InputType;
import android.widget.*;
import com.google.android.material.textfield.*;
import org.json.*;
import java.util.*;

/** Native exposed dropdowns with editable model/URL and optional request parameters. */
public final class AiSettings {
    private static final String[] NAMES = {"DeepSeek", "阿里云百炼（中国内地）", "Kimi", "OpenAI",
            "Claude", "Gemini", "豆包 Seed", "GLM", "Grok", "MiniMAX", "Llama", "Copilot", "自定义"};
    private static final String[] ROOTS = {"https://api.deepseek.com", "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "https://api.moonshot.cn/v1", "https://api.openai.com/v1"};
    private static final String[][] MODELS = {{"deepseek-flash","deepseek-v4-pro"}, {"qwen-plus","qwen3-vl-plus"},
            {"kimi-k2.6"}, {"gpt-4.1-mini"}};

    private static int selectedProvider(SharedPreferences prefs, String endpoint) {
        int saved = Arrays.asList(NAMES).indexOf(prefs.getString("ai_provider", ""));
        if (saved >= 0) return saved;
        int inferred = Arrays.asList(ROOTS).indexOf(endpoint);
        return inferred >= 0 ? inferred : NAMES.length - 1;
    }
    private static MaterialAutoCompleteTextView dropdown(MainActivity a,LinearLayout form,String label,String[] values,String selected,boolean editable){TextInputLayout box=new TextInputLayout(a,null,com.google.android.material.R.attr.textInputStyle);box.setHint(label);box.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);box.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);MaterialAutoCompleteTextView field=new MaterialAutoCompleteTextView(box.getContext());field.setInputType(editable?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS:InputType.TYPE_NULL);field.setTextSize(16);field.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);field.setMinHeight(Ui.dp(a,56));field.setPaddingRelative(Ui.dp(a,16),Ui.dp(a,16),Ui.dp(a,12),Ui.dp(a,16));field.setSimpleItems(values);field.setText(selected,false);box.addView(field,new LinearLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,Ui.dp(a,7),0,Ui.dp(a,7));form.addView(box,lp);return field;}
    private static String keyName(String root){return "ai_key_"+UUID.nameUUIDFromBytes(root.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public static String configuredKey(MainActivity a,String root,String entered)throws Exception{if(!entered.trim().isEmpty())return entered.trim();if(root.equals(a.prefs.getString("api_url","")))return Secrets.get(a,"api_key");return Secrets.get(a,keyName(root));}
    public static void show(MainActivity a){SharedPreferences p=a.prefs;LinearLayout form=a.fullPage("AI 配置",a::home);String saved=p.getString("api_url","");int selected=selectedProvider(p,saved);
        MaterialAutoCompleteTextView provider=dropdown(a,form,"厂商",NAMES,NAMES[selected],false);
        TextInputEditText endpoint=Ui.field(a,form,"API 根地址",saved,false);MaterialAutoCompleteTextView model=dropdown(a,form,"模型（可选择或手动输入）",selected<MODELS.length?MODELS[selected]:new String[0],p.getString("model",""),true);TextInputEditText key=Ui.field(a,form,"API Key（留空使用此接口已保存的密钥）","",false);key.setInputType(129);
        TextView info=Ui.help(a,"可填写 OpenAI Chat Completions 兼容接口。不同地址的 API Key 分开保存。");
        form.addView(info);
        TextView iconInfo=Ui.help(a,"当前图标 "+ProviderAppearance.current(p).label+" · 可在外观设置中固定");
        form.addView(iconInfo);
        provider.setOnItemClickListener((parent,view,position,id)->{
            if (position < ROOTS.length) {
                if (!endpoint.getText().toString().trim().equals(ROOTS[position])) key.setText("");
                endpoint.setText(ROOTS[position]);
                model.setSimpleItems(MODELS[position]);
                model.setText(MODELS[position][0],false);
                info.setText("保存后更新模型设置。应用图标可在外观设置中固定。");
            } else {
                model.setSimpleItems(new String[0]);
                if (position != NAMES.length - 1) {
                    if (Arrays.asList(ROOTS).contains(endpoint.getText().toString().trim())) {
                        endpoint.setText("");
                        key.setText("");
                    }
                    model.setText("", false);
                }
                info.setText("请输入所用服务的兼容 API 地址和模型 ID，可使用支持该模型的网关。");
            }
        });
        form.addView(Ui.button(a,"从接口获取模型列表",false,()->{final String root,secret;try{root=normalize(endpoint.getText().toString());secret=configuredKey(a,root,key.getText().toString());if(secret.isEmpty())throw new Exception("请先填写该接口的 API Key");}catch(Exception e){a.message(e.getMessage());return;}final List<String> ids=new ArrayList<>();a.job(()->{JSONArray data=Network.request(root+"/models",null,secret).getJSONArray("data");for(int i=0;i<Math.min(300,data.length());i++){String name=data.getJSONObject(i).optString("id");if(!name.isEmpty())ids.add(name);}return "已获取 "+ids.size()+" 个模型";},()->{model.setSimpleItems(ids.toArray(new String[0]));info.setText("模型列表已更新，点下拉箭头选择。手动输入的模型不会被覆盖。");},info::setText);}));
        com.google.android.material.materialswitch.MaterialSwitch approval=new com.google.android.material.materialswitch.MaterialSwitch(a);approval.setText("允许 AI 自动批准");approval.setChecked(p.getBoolean("assistant_auto_approve",false));form.addView(approval);approval.setOnCheckedChangeListener((v,on)->p.edit().putBoolean("assistant_auto_approve",on).apply());form.addView(Ui.help(a,"直接交代给 AI 的新事项会立即添加。需确认的操作可在聊天或收集箱批准。"));
        form.addView(Ui.text(a,"生成参数",18,true));TextInputEditText temperature=Ui.field(a,form,"Temperature（留空用服务商默认值）",p.getString("ai_temperature",""),false),topP=Ui.field(a,form,"Top P（留空用默认值）",p.getString("ai_top_p",""),false),tokens=Ui.field(a,form,"最大输出 Token（留空用默认值）",p.getString("ai_max_tokens",""),false),timeout=Ui.field(a,form,"响应超时（秒，10–300）",String.valueOf(p.getInt("ai_timeout",60)),false);
        for(TextInputEditText field:new TextInputEditText[]{temperature,topP})field.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);tokens.setInputType(InputType.TYPE_CLASS_NUMBER);timeout.setInputType(InputType.TYPE_CLASS_NUMBER);
        MaterialAutoCompleteTextView tokenField=dropdown(a,form,"Token 参数名",new String[]{"max_tokens","max_completion_tokens"},p.getString("ai_token_field","max_tokens"),false),thinking=dropdown(a,form,"思考模式",new String[]{"默认","关闭","开启"},p.getString("ai_thinking","默认"),false);
        TextInputEditText extra=Ui.field(a,form,"额外请求参数 JSON（选填）",p.getString("ai_extra",""),true);form.addView(Ui.help(a,"例如 {\"response_format\":{\"type\":\"json_object\"}}。不同模型支持的参数不同；未填写的参数不发送。"));
        form.addView(Ui.button(a,"保存 AI 配置",true,()->{try{String root=normalize(endpoint.getText().toString()),name=model.getText().toString().trim(),secret=configuredKey(a,root,key.getText().toString());if(name.isEmpty())throw new Exception("请选择或填写模型 ID");if(secret.isEmpty())throw new Exception("请填写该接口的 API Key");number(temperature.getText().toString(),0,2,"Temperature");number(topP.getText().toString(),0.000001,1,"Top P");String limit=tokens.getText().toString().trim();if(!limit.isEmpty()&&(Integer.parseInt(limit)<1||Integer.parseInt(limit)>1000000))throw new Exception("Token 上限需在1–1000000之间");int seconds=Integer.parseInt(timeout.getText().toString());if(seconds<10||seconds>300)throw new Exception("超时需为10–300秒");String json=extra.getText().toString().trim();if(!json.isEmpty()){JSONObject object=new JSONObject(json);for(String reserved:new String[]{"model","messages","stream"})if(object.has(reserved))throw new Exception("额外参数不能覆盖 "+reserved);}
            ProviderAppearance previous=ProviderAppearance.current(p);
            Secrets.put(a,keyName(root),secret);Secrets.put(a,"api_key",secret);p.edit().putString("api_url",root).putString("model",name).putString("ai_provider",provider.getText().toString()).putString("ai_temperature",temperature.getText().toString().trim()).putString("ai_top_p",topP.getText().toString().trim()).putString("ai_max_tokens",limit).putString("ai_token_field",tokenField.getText().toString()).putString("ai_thinking",thinking.getText().toString()).putString("ai_extra",json).putInt("ai_timeout",seconds).apply();key.setText("");
            ProviderAppearance.syncLauncher(a,p);
            if(previous!=ProviderAppearance.current(p)&&ThemeColors.usesProvider(a)){
                AgendaWidget.refresh(a);
                a.recreate();
            }else{
                info.setText("AI 配置已保存到本机");
                iconInfo.setText("当前图标 "+ProviderAppearance.current(p).label+" · 可在外观设置中固定");
                a.message("已保存 AI 配置");
            }
        }catch(Exception e){info.setText("未保存："+e.getMessage());a.message(e.getMessage());}}));
    }
    private static void number(String value,double min,double max,String label)throws Exception{if(value.trim().isEmpty())return;double number=Double.parseDouble(value.trim());if(!Double.isFinite(number)||number<min||number>max)throw new Exception(label+" 超出范围");}
    public static String normalize(String address)throws Exception{String root=Network.endpoint(address,true);return root.endsWith("/chat/completions")?root.substring(0,root.length()-17):root;}
}
