package app.planaloud;
import android.content.Context;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** The shipped Markdown manual is the same source as docs/功能说明.md. */
final class FeatureGuide {
    private static LinkedHashMap<String,String> sections(Context c)throws Exception{
        String markdown;try(InputStream in=c.getAssets().open("help/features.md")){markdown=MarkdownFiles.read(in);}LinkedHashMap<String,String> result=new LinkedHashMap<>();for(String part:markdown.split("(?m)(?=^## )")){if(!part.startsWith("## "))continue;int end=part.indexOf('\n');String title=part.substring(3,end<0?part.length():end);String id=title.split(" — ",2)[0].trim();result.put(id,part.trim());}return result;
    }
    static JSONObject read(Context c,JSONObject args)throws Exception{
        LinkedHashMap<String,String> parts=sections(c);String topic=args.optString("topic").trim(),query=args.optString("query").trim().toLowerCase(Locale.ROOT);JSONArray index=new JSONArray();StringBuilder text=new StringBuilder();
        for(Map.Entry<String,String> item:parts.entrySet()){String value=item.getValue(),title=value.substring(3,value.indexOf('\n'));if((topic.isEmpty()||topic.equals(item.getKey()))&&(query.isEmpty()||value.toLowerCase(Locale.ROOT).contains(query))){index.put(new JSONObject().put("topic",item.getKey()).put("title",title));if(!topic.isEmpty()||!query.isEmpty())text.append(value).append("\n\n");}}
        if(!topic.isEmpty()&&index.length()==0)throw new IOException("未知功能章节，请用 guide.read {} 查询目录");int total=text.length(),offset=Math.max(0,Math.min(total,args.optInt("offset"))),end=Math.min(total,offset+10000);if(end<total&&end>offset&&Character.isHighSurrogate(text.charAt(end-1)))end--;
        return new JSONObject().put("source","docs/功能说明.md").put("app_version",c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName).put("topics",index).put("markdown",text.substring(offset,end)).put("offset",offset).put("next_offset",end).put("has_more",end<total).put("hint","topic 按章节读取；query 按关键词查找；使用 offset 继续读取。命令参数查 help，功能页面查 screen.list。");
    }
    static void show(MainActivity a){LinearLayout page=a.fullPage("完整功能说明",a::home);page.addView(Ui.text(a,"完整功能说明",22,true));page.addView(Ui.help(a,"与 AI 使用同一份说明，可按主题阅读。"));try{for(Map.Entry<String,String> item:sections(a).entrySet()){String markdown=item.getValue();String title=markdown.substring(3,markdown.indexOf('\n'));page.addView(Ui.tile(a,title.substring(title.indexOf(" — ")+3),"",()->section(a,title,markdown)));}}catch(Exception error){a.message("无法读取功能说明");Diagnostics.record(a,"feature-guide",error);}}
    private static void section(MainActivity a,String title,String markdown){LinearLayout page=a.fullPage(title,()->show(a));TextView text=Ui.text(a,"",15,false);text.setTextIsSelectable(true);page.addView(text);io.noties.markwon.Markwon.create(a).setMarkdown(text,markdown);}
}
