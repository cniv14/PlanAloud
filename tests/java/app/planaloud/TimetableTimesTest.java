package app.planaloud;
import java.util.*;

public final class TimetableTimesTest {
    static int checks;
    private static void equal(Object actual,Object expected){checks++;if(!Objects.equals(actual,expected))throw new AssertionError(actual+" != "+expected);}
    private static void invalid(String value){checks++;try{TimetableTimes.parse(value);}catch(IllegalArgumentException|java.time.DateTimeException expected){return;}throw new AssertionError("Accepted invalid mapping: "+value);}
    public static void main(String[] args){
        for(String value:new String[]{"1=08:00-08:45","第一节 8:00到8:45","第1节：08：00～08：45","一 8:00至8:45","1 08:00—08:45"}){
            String[] times=TimetableTimes.parse(value).get(1);equal(times[0],"08:00");equal(times[1],"08:45");
        }
        Map<Integer,String[]> mapping=TimetableTimes.parse("第一节 8:00-8:45\r\n第二节 8:55-9:40；第十一节 19:00-19:45");
        equal(mapping.size(),3);equal(mapping.get(11)[0],"19:00");
        equal(TimetableTimes.period("第二十一节"),21);
        equal(Arrays.toString(TimetableTimes.resolve(mapping,"","","第一节","第二节")),"[08:00, 09:40]");
        equal(Arrays.toString(TimetableTimes.resolve(mapping,"8:05","","1","2")),"[08:05, 09:40]");
        equal(Arrays.toString(TimetableTimes.resolve(mapping,"","","1","")),"[08:00, 08:45]");
        equal(Arrays.toString(TimetableTimes.resolve(mapping,"10:00","10:45","1","2")),"[10:00, 10:45]");
        equal(Arrays.toString(TimetableTimes.resolve(mapping,"","","3","")),"[, ]");
        equal(TimetableTimes.parse("").size(),0);
        equal(TimetableTimes.parse("1=8:00").get(1).length,1);
        invalid("第一节 八点到九点");invalid("1=25:00-26:00");invalid("1=8:60-9:00");invalid("1=9:00-8:45");invalid("0=8:00-8:45");invalid("1=8:00-8:45\n第一节 9:00-9:45");
        System.out.println("PASS "+checks+" timetable parsing checks");
    }
}
