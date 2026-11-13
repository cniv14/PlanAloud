package app.planaloud;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.*;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import org.json.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Focused native settings pages, without nested alert dialogs. */
public final class SettingsPanels {
    private final MainActivity a;private final SharedPreferences p;
    public SettingsPanels(MainActivity activity){a=activity;p=a.prefs;}
    private void item(LinearLayout parent,String title,String summary,Runnable action){MaterialCardView card=Ui.card(a);LinearLayout row=Ui.padded(a);row.addView(Ui.text(a,title+"  ›",16,true));row.addView(Ui.help(a,summary));card.addView(row);card.setOnClickListener(v->action.run());parent.addView(card);}
    private void toggle(LinearLayout form,String label,String hint,boolean value,java.util.function.Consumer<Boolean> onChange){MaterialSwitch control=new MaterialSwitch(a);control.setText(label);control.setChecked(value);control.setPadding(0,Ui.dp(a,10),0,Ui.dp(a,10));form.addView(control);if(!hint.isEmpty())form.addView(Ui.help(a,hint));control.setOnCheckedChangeListener((v,on)->onChange.accept(on));}
    public void root(LinearLayout form){
        form.addView(Ui.text(a,"设置",25,true));
        LinearLayout first=Ui.row(a),second=Ui.row(a);form.addView(first);form.addView(second);
        Ui.cell(a,first,Ui.tile(a,"外观","主题、壁纸、小组件",this::appearance));
        Ui.cell(a,first,Ui.tile(a,"同步","备份、云端、局域网",this::sync));
        Ui.cell(a,second,Ui.tile(a,"AI 模型","厂商与模型",this::api));
        Ui.cell(a,second,Ui.tile(a,"更新","本体与内嵌模块",this::updates));
        form.addView(Ui.button(a,"更多功能",true,()->CommandScreen.show(a)));
    }
    public void appearance(){LinearLayout form=a.fullPage("外观与主题",a::home);form.addView(Ui.text(a,"主题",18,true));ChipGroup group=new ChipGroup(a);group.setSingleSelection(true);group.setSelectionRequired(true);String[] labels={"跟随系统","浅色","深色"},values={"system","light","dark"};for(int i=0;i<3;i++){String value=values[i];Chip chip=new Chip(a);chip.setText(labels[i]);chip.setCheckable(true);chip.setChecked(p.getString("theme","system").equals(value));chip.setOnClickListener(v->{p.edit().putString("theme",value).apply();DayFlowApp.applyTheme(value);AgendaWidget.refresh(a);});group.addView(chip);}form.addView(group);
        Spinner floatingMask=TaskTypes.spinner(a,form,"悬浮窗透明遮罩",Arrays.asList("应用主题色（默认）","中性色"));
        floatingMask.setSelection("neutral".equals(p.getString(FloatingAssistant.MASK_COLOR,"theme"))?1:0);
        floatingMask.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int position,long id){
                p.edit().putString(FloatingAssistant.MASK_COLOR,position==0?"theme":"neutral").apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        toggle(form,"减少序言动效","保留布局，关闭主要过渡与壁纸切换动画。",p.getBoolean("reduce_motion",false),on->p.edit().putBoolean("reduce_motion",on).apply());
        toggle(form,"紧凑列表与表格","减少卡片和表格留白，操作区域仍至少48dp。",p.getBoolean("compact_ui",false),on->{p.edit().putBoolean("compact_ui",on).apply();a.recreate();});
        form.addView(Ui.tile(a,"应用图标",ProviderAppearance.current(p).label,()->ProviderAppearance.show(a)));
        form.addView(Ui.tile(a,"桌面小组件","单双列排布、背景和桌面添加",()->widget(this::appearance)));
        ThemeColors.settings(a,form);
        toggle(form,"动态流光","仅在前台运行，省电模式暂停。",p.getBoolean("motion",false),on->{p.edit().putBoolean("motion",on).apply();a.refreshBackdrop();});toggle(form,"背景模糊","Android 12 及以上可用。",p.getBoolean("blur",true),on->{p.edit().putBoolean("blur",on).apply();a.refreshBackdrop();});toggle(form,"左右滑动切换页面","今天左滑进入周预览；输入和裁剪区域不触发切页。",p.getBoolean("swipe",true),on->p.edit().putBoolean("swipe",on).apply());form.addView(Ui.text(a,"背景不透明度",14,true));Slider slider=new Slider(a);slider.setValueFrom(65);slider.setValueTo(100);slider.setStepSize(1);slider.setValue(p.getInt("opacity",86));slider.addOnChangeListener((s,value,user)->{if(user){p.edit().putInt("opacity",Math.round(value)).apply();a.refreshBackdrop();}});form.addView(slider);
    }
    void widget(Runnable back){
        LinearLayout page=a.fullPage("桌面小组件",back);page.addView(Ui.text(a,"桌面小组件",24,true));
        page.addView(Ui.help(a,"单列展开标题、时间、截止倒计时、地点和备注。两列为紧凑排布，窄尺寸自动使用单列。"));
        Spinner layout=TaskTypes.spinner(a,page,"排布",Arrays.asList("单列","矩阵 · 两列"));layout.setSelection("grid".equals(p.getString("widget_layout","grid"))?1:0);
        List<String> modes=Arrays.asList("glass","solid","image","wallpaper");Spinner style=TaskTypes.spinner(a,page,"背景样式",Arrays.asList("液态玻璃","主题实色","自选图片 + 玻璃","跟随应用壁纸 + 玻璃"));style.setSelection(Math.max(0,modes.indexOf(p.getString("widget_background","glass"))));
        TextInputEditText tint=Ui.field(a,page,"玻璃底层遮罩 · 48–94%",Integer.toString(p.getInt("widget_tint",73)),false);tint.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout pair=Ui.row(a);page.addView(pair);
        Ui.cell(a,pair,Ui.button(a,"应用样式",true,()->{try{int value=Integer.parseInt(tint.getText().toString());if(value<48||value>94)throw new Exception("遮罩需在 48–94%");String mode=modes.get(style.getSelectedItemPosition());if(mode.equals("image")&&!WallpaperImages.widget(a).isFile())throw new Exception("请先选择背景图");p.edit().putString("widget_layout",layout.getSelectedItemPosition()==0?"list":"grid").putString("widget_background",mode).putInt("widget_tint",value).apply();AgendaWidget.refresh(a);a.message("已更新桌面小组件");}catch(Exception e){a.message(e.getMessage());}}));
        Ui.cell(a,pair,Ui.button(a,"选择背景图",false,()->a.pickFile(120,"image/*")));
        toggle(page,"显示收集箱","显示待确认数量和最新内容。",p.getBoolean("widget_inbox",true),on->{p.edit().putBoolean("widget_inbox",on).apply();AgendaWidget.refresh(a);});
        page.addView(Ui.button(a,"添加到桌面",false,()->{AppWidgetManager manager=AppWidgetManager.getInstance(a);if(manager.isRequestPinAppWidgetSupported()){manager.requestPinAppWidget(new ComponentName(a,AgendaWidget.class),null,null);a.message("请在桌面启动器中确认添加");}else a.message("请长按桌面 → 小组件 → 序言");}));
        page.addView(Ui.help(a,"玻璃使用半透明渐变、高光边缘与图片遮罩。桌面系统不允许小组件实时模糊下方内容。"));
    }
    public void weather(){
        LinearLayout form=a.fullPage("位置与天气",a::home);
        form.addView(Ui.text(a,p.getString("city","当前位置"),23,true));
        form.addView(Ui.text(a,OpenWeatherSources.cached(a,"尚未获取天气"),30,true));
        String source=p.getString("weather_source","");
        if(!OpenWeatherSources.cached(a,"").isEmpty())
            form.addView(Ui.help(a,"来源 "+(p.getBoolean("weather_via_server",false)?"序言服务端 / ":"")+source+" · "+p.getString("weather_time","").replace("T"," ")));
        form.addView(Ui.help(a,"天气优先使用 Open-Meteo，服务不可用时自动切换到 MET Norway。可使用大致位置、搜索城市或输入经纬度。"));
        toggle(form,"优先使用序言服务端","连接序言账号后生效。服务端不可用时自动改用开放天气来源。",
                p.getBoolean("weather_use_server",true),on->p.edit().putBoolean("weather_use_server",on).apply());
        if(!p.getString("lat","").isEmpty())
            form.addView(Ui.button(a,"刷新已选位置天气",false,()->a.job(()->Forecast.refresh(a),this::weather)));
        form.addView(Ui.button(a,"使用当前位置获取天气",true,()->a.locationWeather.request()));
        toggle(form,"自动更新位置天气","打开应用时，缓存超过6小时才刷新。",p.getBoolean("auto_location",false),
                on->{p.edit().putBoolean("auto_location",on).apply();if(on)a.locationWeather.request();});
        form.addView(Ui.button(a,"搜索并选择城市",false,this::citySearch));
        form.addView(Ui.button(a,"输入经纬度",false,this::weatherCoordinates));
        TextView credit=Ui.help(a,"");
        credit.setText(android.text.Html.fromHtml("数据来自 <a href='https://open-meteo.com/'>Open-Meteo</a> 和 <a href='https://api.met.no/'>MET Norway</a>，采用 <a href='https://creativecommons.org/licenses/by/4.0/'>CC BY 4.0</a>。备用源按预报时段汇总。",android.text.Html.FROM_HTML_MODE_LEGACY));
        credit.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());form.addView(credit);
    }

    private void weatherCoordinates(){
        LinearLayout form=a.fullPage("输入天气位置",this::weather);
        TextInputEditText latitude=Ui.field(a,form,"纬度（-90 至 90）",p.getString("lat",""),false);
        TextInputEditText longitude=Ui.field(a,form,"经度（-180 至 180）",p.getString("lon",""),false);
        int decimal=android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED;
        latitude.setInputType(decimal);longitude.setInputType(decimal);
        form.addView(Ui.button(a,"获取天气",true,()->{
            try{
                double lat=Double.parseDouble(latitude.getText().toString().trim());
                double lon=Double.parseDouble(longitude.getText().toString().trim());
                selectWeather(lat,lon,"自定义位置","");
            }catch(NumberFormatException e){a.message("请输入有效经纬度");}
        }));
    }

    private void selectWeather(double lat,double lon,String city,String timezone){
        a.job(()->{
            String result=Forecast.refresh(a,lat,lon,city,timezone);
            p.edit().putBoolean("auto_location",false).apply();
            return result;
        },this::weather);
    }

    private void citySearch(){
        LinearLayout form=a.fullPage("选择城市",this::weather);
        TextInputEditText query=Ui.field(a,form,"城市名称，例如北京、上海","",false);
        form.addView(Ui.button(a,"搜索城市",true,()->{
            String name=query.getText().toString().trim();
            if(name.isEmpty()){query.setError("请填写城市名称");return;}
            final JSONArray[] result={null};
            a.job(()->{result[0]=WeatherCities.search(a,name);return result[0].length()==0?"没有找到城市，请尝试完整名称":null;},()->{
                if(result[0].length()==0)return;
                LinearLayout list=a.fullPage("选择城市",this::citySearch);
                for(int i=0;i<result[0].length();i++){
                    JSONObject city=result[0].optJSONObject(i);
                    if(city==null||!Double.isFinite(city.optDouble("latitude"))||!Double.isFinite(city.optDouble("longitude")))continue;
                    String title=city.optString("name");
                    String detail=java.util.stream.Stream.of(city.optString("admin1"),city.optString("country"))
                            .filter(value->!value.isEmpty()).distinct().collect(java.util.stream.Collectors.joining(" · "));
                    item(list,title,detail,()->selectWeather(city.optDouble("latitude"),city.optDouble("longitude"),title,city.optString("timezone")));
                }
            });
        }));
    }
    private void capture(){GroupCaptureSettings.show(a);}
    private void api(){AiSettings.show(a);}

    private void useBackend(String backend){try{boolean windows=backend.equals("taskflow");if(p.getString(windows?"tf_address":"server","").isEmpty()||Secrets.get(a,windows?"tf_token":"token").isEmpty())throw new Exception(windows?"请先连接 Windows":"请先登录 序言服务器");p.edit().putString("sync_backend",backend).putBoolean("sync_auto",true).apply();SyncJob.configure(a);sync();a.message("已切换同步服务，本地内容保留");}catch(Exception e){a.message(e.getMessage());}}
    public void sync(){LinearLayout form=a.fullPage("数据与同步",a::home);form.addView(Ui.help(a,"当前同步服务："+(p.getString("sync_backend","dayflow").equals("taskflow")?"TaskFlow Windows":"序言云端／局域网")));form.addView(Ui.button(a,"连接 TaskFlow Windows 端",true,()->TaskFlowSettings.show(a)));if(!p.getString("server","").isEmpty())form.addView(Ui.button(a,"使用已登录的 序言服务",false,()->useBackend("dayflow")));if(!p.getString("tf_address","").isEmpty())form.addView(Ui.button(a,"使用已连接的 TaskFlow Windows",false,()->useBackend("taskflow")));form.addView(Ui.help(a,"文字及图片笔记、日程、每日待办、课程、收集箱、阶段任务与完成/删除状态都使用同一套同步。文件或云端均保留事项 ID，冲突保留副本。本地使用无需账号。"));toggle(form,"自动同步全部内容","连接服务器账号后，修改按系统网络条件同步，另有每15分钟系统调度。省电策略可能延后；也可立即同步。",p.getBoolean("sync_auto",false),on->{p.edit().putBoolean("sync_auto",on).apply();SyncJob.configure(a);});form.addView(Ui.help(a,"同步状态："+p.getString("sync_message","尚未同步")+"\n上次成功："+p.getString("sync_last","暂无")));form.addView(Ui.help(a,"API Key、登录凭据、通知授权、壁纸与天气位置是设备配置，不包含在同步文件中。"));form.addView(Ui.button(a,"导出同步文件",true,()->a.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"planaloud-"+LocalDate.now()+".dayflow"),101)));form.addView(Ui.button(a,"合并导入同步文件",false,()->a.pickFile(102,"*/*")));form.addView(Ui.button(a,"云端 / 局域网账号",false,this::account));form.addView(Ui.button(a,"立即同步",false,()->a.job(()->Network.sync(a),null)));form.addView(Ui.button(a,"查看与处理同步冲突",false,()->SyncConflicts.show(a,0)));form.addView(Ui.button(a,"退出服务器账号",false,()->a.job(()->{return TaskFlowSettings.logout(a);},null)));}
    void account(){LinearLayout form=a.fullPage("服务器账号",this::sync);TextInputEditText server=Ui.field(a,form,"服务器地址（域名或IP，可含端口）",p.getString("server",""),false),user=Ui.field(a,form,"用户名",p.getString("username",""),false),password=Ui.field(a,form,"密码","",false),invite=Ui.field(a,form,"邀请码（注册时选填）","",false);password.setInputType(129);MaterialSwitch register=new MaterialSwitch(a);register.setText("注册新账号");form.addView(register);TextView status=Ui.help(a,"默认端口8765。域名默认 HTTPS，局域网 IP 可用 HTTP。请填服务端地址，不能填 GitHub 项目网址。登录失败时保留本页输入。" );form.addView(status);
        form.addView(Ui.button(a,"检查服务器连接",false,()->{String address=server.getText().toString();status.setText("正在连接服务器…");a.job(()->Network.checkServer(address),()->status.setText("服务器连接正常，可继续登录"),message->status.setText("连接失败："+message));}));
        form.addView(Ui.button(a,"连接并登录",true,()->{String address=server.getText().toString(),username=user.getText().toString(),pass=password.getText().toString(),code=invite.getText().toString();boolean create=register.isChecked();status.setText("正在登录…");a.job(()->PlannerServices.login(a,address,username,pass,code,create),this::sync,message->status.setText("登录失败："+message+"\n输入内容已保留，请修改后重试。"));}));}

    private String version(){try{return a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;}catch(Exception ignored){return "未知";}}
    public void updates(){UpdateCenter.show(a);}
    void licenses(){LinearLayout page=a.fullPage("开源组件与许可",a::home);page.addView(Ui.help(a,"序言原创代码采用 MIT。复用的组件保留各自许可证。"));String[] labels={"lunar-java · MIT","jsoup · MIT","节假日数据 · MIT","biweekly · BSD 2-Clause","Material / AndroidX · Apache 2.0","完整第三方声明（含插件）"},files={"lunar-java-MIT.txt","jsoup-MIT.txt","holiday-cn-MIT.txt","biweekly-BSD.txt","Apache-2.0.txt","THIRD_PARTY_NOTICES.md"};for(int i=0;i<labels.length;i++){String label=labels[i],file=files[i];page.addView(Ui.button(a,label,false,()->{try(java.io.InputStream input=a.getAssets().open("licenses/"+file)){String text=Network.read(input,32768);LinearLayout form=a.fullPage(label,this::licenses);TextView body=Ui.text(a,text,13,false);body.setTextIsSelectable(true);form.addView(body);}catch(Exception e){a.message(e.getMessage());}}));}}
    public void showUpdate(JSONObject info){LinearLayout form=a.fullPage("发现新版本 "+info.optString("version"),this::updates);form.addView(Ui.text(a,info.optString("notes"),16,false));form.addView(Ui.help(a,"大小 "+(info.optLong("size")/1024)+" KB。清单验证签名，安装包验证 SHA-256，由系统确认安装。"));String found=null;try{found=Updates.cached(a,info);}catch(Exception ignored){}final String cached=found;if(cached!=null)form.addView(Ui.help(a,"安装包已缓存并通过校验，本次不会重复下载。"));form.addView(Ui.button(a,cached==null?"下载并安装":"安装已下载的更新",true,()->{if(cached!=null){try{Updates.install(a,cached);}catch(Exception e){a.message(e.getMessage());}return;}a.job(()->{String path=Updates.download(a,info);a.runOnUiThread(()->{if(!a.isDestroyed())try{Updates.install(a,path);}catch(Exception e){a.message(e.getMessage());}});return "更新包已验证";},null);}));}
}
