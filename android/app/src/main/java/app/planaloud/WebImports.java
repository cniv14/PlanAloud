package app.planaloud;
import android.widget.*;
import com.google.android.material.textfield.TextInputEditText;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.json.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;

/** jsoup parses saved HTML without executing page scripts or sending account cookies. */
final class WebImports {
 static void show(MainActivity a){LinearLayout page=a.fullPage("网页与日程导入",a::home);page.addView(Ui.text(a,"导入网页或日历",22,true));TextInputEditText url=Ui.field(a,page,"公开网页 / ICS 地址（可选）","",false),text=Ui.field(a,page,"粘贴 HTML、CSV 或 ICS 内容","",true);page.addView(Ui.help(a,"仅读取你填写的页面，不接收账号或登录Cookie。教务系统可先用浏览器导出HTML再导入；规则无法确定的表格先转正文，由你或AI确认。"));page.addView(Ui.button(a,"读取地址",true,()->{String address=url.getText().toString();final String[] result={null};a.job(()->{result[0]=fetch(address);return "内容已读取，请确认导入方式";},()->preview(a,result[0]));}));page.addView(Ui.button(a,"解析粘贴内容",false,()->{String body=text.getText().toString();if(body.trim().isEmpty()){a.message("请先粘贴内容");return;}preview(a,body);}));page.addView(Ui.button(a,"选择 HTML / CSV / ICS 文件",false,()->a.pickFile(103,"*/*")));}
 private static String pageUrl(String input)throws Exception{URI uri=new URI(input.trim());Network.endpoint(new URI(uri.getScheme(),uri.getRawAuthority(),uri.getPath(),null,null).toString(),true);return new URI(uri.getScheme(),uri.getRawAuthority(),uri.getPath(),uri.getQuery(),null).toString();}
 static String fetch(String input)throws Exception{URL target=new URL(pageUrl(input));for(int i=0;i<4;i++){HttpURLConnection connection=(HttpURLConnection)target.openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(30000);connection.setInstanceFollowRedirects(false);try{int status=connection.getResponseCode();if(status>=300&&status<400){String next=connection.getHeaderField("Location");if(next==null)throw new IOException("网页重定向无目标");URL redirect=new URL(target,next);pageUrl(redirect.toString());if(!redirect.getHost().equalsIgnoreCase(target.getHost()))throw new IOException("地址重定向至其他网站，请填写最终网址");target=redirect;continue;}if(status!=200)throw new IOException("网页HTTP "+status+"，可能需要浏览器登录后导出文件");try(InputStream in=connection.getInputStream()){return Network.read(in,1024*1024);}}finally{connection.disconnect();}}throw new IOException("页面重定向次数过多");}
 private static void preview(MainActivity a,String body){LinearLayout page=a.fullPage("校对导入内容",()->show(a));String plain=body.trim().startsWith("<")?Jsoup.parse(body).body().text():body;TextView preview=Ui.text(a,plain.substring(0,Math.min(plain.length(),3000)),15,false);preview.setTextIsSelectable(true);page.addView(preview);page.addView(Ui.button(a,"解析为日程预览",true,()->{final JSONArray[] rows={null};a.job(()->{rows[0]=Imports.schedule(body);return "已解析 "+rows[0].length()+" 项，尚未加入日程";},()->a.importPreview(rows[0]));}));page.addView(Ui.button(a,"正文放入收集箱继续校对",false,()->{String content=plain.length()>12000?plain.substring(0,12000):plain;a.showCaptureText(content);}));page.addView(Ui.help(a,"ICS每次展开一年以内，最大4000项。重复订阅用UID与发生时刻去重，导入不覆盖已编辑事项。日期或时间不明确的资料先放入收集箱核对。CSV 使用 title,start_at,end_at,location；日期时间例如 2026-11-23T09:00。"));}
 static JSONArray html(String source)throws Exception{Document doc=Jsoup.parse(source);String csv=tableCsv(doc);if(!csv.isEmpty())return Imports.schedule(csv);String plain=doc.body().text();if(plain.length()>12000)plain=plain.substring(0,12000);if(plain.trim().isEmpty())throw new Exception("网页没有可读取的正文");return new JSONArray().put(Data.make("inbox",doc.title().isEmpty()?"网页日程 · 待确认":doc.title()).put("content",plain).put("source","HTML导入·需校对").put("confirmation_status","pending"));}
 private static String column(String value){String key=value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+","");String[][] names={{"title","名称","标题","事项"},{"start_at","开始时间","开始日期时间"},{"end_at","结束时间","结束日期时间"},{"location","地点"},{"content","备注","正文"},{"recurrence","重复"},{"recurrence_until","重复截止日期"}};for(String[] list:names)for(String alias:list)if(key.equals(alias))return list[0];return key;}
 private static String quote(String value){return "\""+value.replace("\"","\"\"")+"\"";}
 private static String tableCsv(Document doc)throws Exception{
  for(Element table:doc.select("table")){
   Elements rows=table.select("tr");if(rows.size()<2)continue;
   if(rows.size()>500)throw new Exception("表格行数过多，请拆分导入");
   List<String> headers=new ArrayList<>();for(Element cell:rows.get(0).select("th,td"))headers.add(column(cell.text()));
   if(!headers.containsAll(Arrays.asList("title","start_at")))continue;
   StringBuilder out=new StringBuilder(String.join(",",headers)).append('\n');
   for(int i=1;i<rows.size();i++){
    Elements cells=rows.get(i).select("td,th");if(cells.size()!=headers.size())throw new Exception("表格含合并单元格，请转正文后核对");
    for(int j=0;j<cells.size();j++){if(j>0)out.append(',');out.append(quote(cells.get(j).text()));}out.append('\n');
   }
   return out.toString();
  }
  return "";
 }
}
