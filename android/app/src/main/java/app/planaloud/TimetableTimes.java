package app.planaloud;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.*;

/** Shared parsing for preview, editor and import. */
final class TimetableTimes {
    private static final Pattern ROW=Pattern.compile("^(?:第\\s*)?([0-9一二三四五六七八九十两]+)\\s*(?:节|课)?\\s*[=:：]?\\s*(\\d{1,2}[:：]\\d{2})(?:\\s*(?:-|—|–|~|～|至|到)\\s*(\\d{1,2}[:：]\\d{2}))?$");
    static int period(String value) {
        String n=value.trim().replace("第", "").replace("节", "").replace("课", "").replace("两", "二").trim();
        if(n.matches("[0-9]+"))return Integer.parseInt(n);
        int ten=n.indexOf('十');
        if(ten>=0)return (ten==0?1:digit(n.substring(0,ten)))*10+(ten==n.length()-1?0:digit(n.substring(ten+1)));
        return digit(n);
    }
    private static int digit(String n) {int value="一二三四五六七八九".indexOf(n);if(n.length()!=1||value<0)throw new IllegalArgumentException("无效节次："+n);return value+1;}
    static String time(String value) {String[] parts=value.trim().replace('：', ':').split(":");if(parts.length!=2)throw new IllegalArgumentException("时间应为 HH:mm："+value);return LocalTime.of(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])).toString();}
    static Map<Integer,String[]> parse(String text) {
        Map<Integer,String[]> result=new LinkedHashMap<>();
        for(String line:text.split("[\\r\\n;；]+")) {
            if(line.trim().isEmpty())continue;
            Matcher row=ROW.matcher(line.trim());
            if(!row.matches())throw new IllegalArgumentException("无法识别节次："+line+"；示例：第一节 08:00-08:45");
            int number=period(row.group(1));if(number<1)throw new IllegalArgumentException("节次必须大于0");
            String start=time(row.group(2)),end=row.group(3)==null?null:time(row.group(3));
            if(end!=null&&!LocalTime.parse(end).isAfter(LocalTime.parse(start)))throw new IllegalArgumentException("第"+number+"节结束时间必须晚于开始时间");
            if(result.put(number,end==null?new String[]{start}:new String[]{start,end})!=null)throw new IllegalArgumentException("第"+number+"节重复，请保留一条对照");
        }
        return result;
    }
    static String[] resolve(Map<Integer,String[]> mapping,String start,String end,String first,String last) {
        String[] begin=first.trim().isEmpty()?null:mapping.get(period(first));
        String[] finish=last.trim().isEmpty()?begin:mapping.get(period(last));
        if(start.trim().isEmpty())start=begin==null?"":begin[0];
        if(end.trim().isEmpty()&&finish!=null&&finish.length>1)end=finish[1];
        return new String[]{start.trim().isEmpty()?"":time(start),end.trim().isEmpty()?"":time(end)};
    }
}
