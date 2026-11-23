package app.planaloud;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Imports {
    public static List<List<String>> csv(String text) throws Exception {
        List<List<String>> result=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quote=false;
        text=text.replace("\uFEFF","");
        for(int i=0;i<text.length();i++){char ch=text.charAt(i);
            if(ch=='"'){if(quote&&i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else quote=!quote;}
            else if(!quote&&(ch==','||ch=='\n')){row.add(cell.toString().trim());cell.setLength(0);if(ch=='\n'){if(row.stream().anyMatch(s->!s.trim().isEmpty()))result.add(row);row=new ArrayList<>();}}
            else if(ch!='\r'||quote)cell.append(ch);
        }if(quote)throw new Exception("CSV 引号未闭合");
        row.add(cell.toString().trim());if(row.stream().anyMatch(s->!s.trim().isEmpty()))result.add(row);return result;
    }
    /** Date-based schedules share the same fields as manual and assistant-created events. */
    public static JSONArray schedule(String text) throws Exception {
        if(text.trim().startsWith("BEGIN:VCALENDAR"))return ics(text);
        if(text.trim().startsWith("<"))return WebImports.html(text);
        List<List<String>> rows=csv(text);
        if(rows.size()<2)throw new Exception("日程没有数据行");
        List<String> header=rows.get(0);
        for(String key:new String[]{"title","start_at"})if(!header.contains(key))throw new Exception("缺少日程列："+key+"；使用 title,start_at,end_at,location，无需学期或周次");
        if(rows.size()>4001)throw new Exception("单次最多导入4000项日程");
        JSONArray records=new JSONArray();
        for(int i=1;i<rows.size();i++){
            List<String> values=rows.get(i);
            if(values.size()!=header.size())throw new Exception("第"+(i+1)+"行列数不匹配");
            Map<String,String> fields=new HashMap<>();
            for(int j=0;j<header.size();j++)fields.put(header.get(j),values.get(j));
            String title=fields.get("title"),start=fields.get("start_at"),end=fields.getOrDefault("end_at","");
            if(title.trim().isEmpty())throw new Exception("第"+(i+1)+"行标题不能为空");
            LocalDateTime at=LocalDateTime.parse(start);
            if(!end.isEmpty()&&!LocalDateTime.parse(end).isAfter(at))throw new Exception("结束时间必须晚于开始时间");
            JSONObject event=Data.make("event",title).put("start_at",start).put("end_at",end).put("source","日程CSV导入");
            for(String key:new String[]{"location","content","recurrence","recurrence_until"})if(fields.containsKey(key))event.put(key,fields.get(key));
            stable(event,"schedule-csv:"+title+":"+at+":"+event.optString("location"));
            Data.validate(event);records.put(event);
        }
        return records;
    }
    private static void stable(JSONObject r,String seed) throws Exception {
        r.put("id",UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString());
    }
    private static LocalDateTime icalTime(String key,String value) throws Exception {
        if(value.matches("\\d{8}"))return LocalDate.parse(value,DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
        DateTimeFormatter f=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        if(value.endsWith("Z"))return LocalDateTime.parse(value.substring(0,value.length()-1),f).atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime result=LocalDateTime.parse(value,f);
        int i=key.indexOf("TZID=");if(i>=0){String zone=key.substring(i+5).split(";")[0];result=result.atZone(ZoneId.of(zone)).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();}return result;
    }
    public static JSONArray ics(String text)throws Exception{return IcsImports.parse(text);}
}
