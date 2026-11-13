package app.planaloud;

import android.view.View;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import org.json.*;
import java.util.*;

final class WallpaperScreen {
    static void show(MainActivity a){
        try{
            JSONObject state=WallpaperRules.read(a);JSONArray images=state.getJSONArray("images"),rules=state.getJSONArray("rules");
            LinearLayout page=a.fullPage("壁纸轮播与规则",a::home);page.addView(Ui.text(a,"壁纸与场景",25,true));
            LinearLayout actions=Ui.row(a);page.addView(actions);Ui.cell(a,actions,Ui.button(a,"＋ 导入壁纸",true,()->a.pickFile(119,"image/*")));Ui.cell(a,actions,Ui.button(a,"＋ 添加规则",false,()->rule(a,-1)));
            TextInputEditText period=Ui.field(a,page,"轮播间隔 · 分钟（0 关闭）",state.optString("rotate_minutes","0"),false);period.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            List<String> names=new ArrayList<>(),ids=new ArrayList<>();names.add("原壁纸 / 主题背景");ids.add("");for(int i=0;i<images.length();i++){JSONObject item=images.getJSONObject(i);ids.add(item.getString("id"));names.add(item.getString("name"));}
            Spinner fallback=TaskTypes.spinner(a,page,"无匹配时的默认壁纸",names);fallback.setSelection(Math.max(0,ids.indexOf(state.optString("fallback"))));
            page.addView(Ui.button(a,"保存轮播与默认壁纸",true,()->a.job(()->{JSONObject latest=WallpaperRules.read(a);WallpaperRules.save(a,latest.put("rotate_minutes",Integer.parseInt(period.getText().toString())).put("fallback",ids.get(fallback.getSelectedItemPosition())));return "壁纸规则已更新";},()->{a.refreshBackdrop();show(a);})));
            page.addView(Ui.help(a,"数字越大优先级越高。同优先级命中多张时可轮播；没有命中时，开启轮播会从图库选图。仅应用在前台时刷新壁纸规则。"));
            page.addView(Ui.text(a,"规则 · "+rules.length(),18,true));LinearLayout line=null;
            for(int i=0;i<rules.length();i++){if(i%2==0){line=Ui.row(a);page.addView(line);}JSONObject entry=rules.getJSONObject(i);int index=i,image=ids.indexOf(entry.getString("image"));Ui.cell(a,line,Ui.tile(a,image<0?"未找到图片":names.get(image),entry.getInt("priority")+" · "+entry.getString("when"),()->rule(a,index)));}if(rules.length()%2==1)Ui.cell(a,line,new View(a));
            LinearLayout files=Ui.row(a);page.addView(files);
            Ui.cell(a,files,Ui.button(a,"导入规则 JSON",false,()->a.pickFile(121,"*/*")));
            Ui.cell(a,files,Ui.button(a,"导出规则 JSON",false,()->a.startActivityForResult(new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(android.content.Intent.EXTRA_TITLE,"wallpaper-rules.json"),122)));
            page.addView(Ui.button(a,"编辑 JSON 配置",false,()->raw(a)));
            page.addView(Ui.help(a,"配置 JSON 只保存规则与图片 ID，不包含图片内容。导入时需要本机已有对应图片。"));
        }catch(Exception e){a.message(e.getMessage());}
    }
    private static void rule(MainActivity a,int index){try{
        JSONObject state=WallpaperRules.read(a);JSONArray images=state.getJSONArray("images"),rules=state.getJSONArray("rules");if(images.length()==0){a.message("请先导入一张壁纸");return;}
        JSONObject current=index<0?new JSONObject():rules.getJSONObject(index);List<String> ids=new ArrayList<>(),names=new ArrayList<>();for(int i=0;i<images.length();i++){ids.add(images.getJSONObject(i).getString("id"));names.add(images.getJSONObject(i).getString("name"));}
        LinearLayout page=a.fullPage("壁纸规则",()->show(a));Spinner picture=TaskTypes.spinner(a,page,"使用图片",names);picture.setSelection(Math.max(0,ids.indexOf(current.optString("image"))));
        TextInputEditText priority=Ui.field(a,page,"优先级",current.optString("priority","10"),false);priority.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        TextInputEditText condition=Ui.field(a,page,"匹配条件",current.optString("when",""),true);
        String[] expressions={"time[night]","time[day]","time[morning]","time[weekend]","weather[sunny]","weather[cloudy]","weather[rain]","weather[snow]","event[active]","event[none]","event[course]","event[task]","always"};
        Spinner choice=TaskTypes.spinner(a,page,"插入条件",Arrays.asList("夜间","白天","上午","周末","晴天","多云","下雨","下雪","有进行中的安排","没有进行中的安排","上课中","待办进行中","始终"));
        LinearLayout operators=Ui.row(a);page.addView(operators);
        Ui.cell(a,operators,Ui.button(a,"且  &&",false,()->append(condition," && ",expressions[choice.getSelectedItemPosition()])));
        Ui.cell(a,operators,Ui.button(a,"或  ||",false,()->append(condition," || ",expressions[choice.getSelectedItemPosition()])));
        Ui.cell(a,operators,Ui.button(a,"替换条件",false,()->condition.setText(expressions[choice.getSelectedItemPosition()])));
        page.addView(Ui.help(a,"且先于或；可在表达式里加括号，也可输入 time[22:00-06:00] 或 event[title:关键词]。"));
        page.addView(Ui.button(a,"保存规则",true,()->{try{JSONObject newRule=new JSONObject().put("image",ids.get(picture.getSelectedItemPosition())).put("priority",Integer.parseInt(priority.getText().toString())).put("when",condition.getText().toString().trim());rules.put(index<0?rules.length():index,newRule);WallpaperRules.save(a,state);a.refreshBackdrop();show(a);}catch(Exception e){a.message(e.getMessage());}}));
        if(index>=0)page.addView(Ui.button(a,"删除此规则",false,()->{try{rules.remove(index);WallpaperRules.save(a,state);a.refreshBackdrop();show(a);}catch(Exception e){a.message(e.getMessage());}}));
    }catch(Exception e){a.message(e.getMessage());}}
    private static void append(TextInputEditText e,String op,String part){String current=e.getText().toString().trim();e.setText(current.isEmpty()?part:current+op+part);e.setSelection(e.length());}
    private static void raw(MainActivity a){try{LinearLayout page=a.fullPage("壁纸 JSON",()->show(a));TextInputEditText field=Ui.field(a,page,"规则配置",WallpaperRules.read(a).toString(2),true);field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(256*1024)});page.addView(Ui.button(a,"验证并保存",true,()->{try{WallpaperRules.save(a,new JSONObject(field.getText().toString()));a.refreshBackdrop();show(a);}catch(Exception e){a.message(e.getMessage());}}));}catch(Exception e){a.message(e.getMessage());}}
}
