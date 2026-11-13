package app.planaloud;

import com.nlf.calendar.Solar;
import com.nlf.calendar.Lunar;
import java.time.LocalDate;
import android.util.LruCache;

/** Offline Chinese calendar provided by the MIT-licensed lunar-java project. */
public final class ChineseDates {
    private static final LruCache<LocalDate,String> labels=new LruCache<>(100);
    public static Lunar lunar(LocalDate date){return Solar.fromYmd(date.getYear(),date.getMonthValue(),date.getDayOfMonth()).getLunar();}
    public static synchronized String label(LocalDate date){
        String cached=labels.get(date);if(cached!=null)return cached;
        Lunar lunar=lunar(date);String value=lunar.getJieQi();if(value==null)value="";
        if(value.isEmpty()&&!lunar.getFestivals().isEmpty())value=lunar.getFestivals().get(0);
        if(value.isEmpty())value=lunar.getDay()==1?lunar.getMonthInChinese()+"月":lunar.getDayInChinese();
        labels.put(date,value);return value;
    }
    static String dayLabel(LocalDate date){
        Lunar lunar=lunar(date);
        return lunar.getDay()==1?lunar.getMonthInChinese()+"月":lunar.getDayInChinese();
    }
    static java.util.List<String> festivals(LocalDate date){
        Solar solar=Solar.fromYmd(date.getYear(),date.getMonthValue(),date.getDayOfMonth());
        java.util.ArrayList<String> labels=new java.util.ArrayList<>(solar.getFestivals());
        Lunar lunar=solar.getLunar();
        labels.addAll(lunar.getFestivals());
        String term=lunar.getJieQi();
        if(term!=null&&!term.isEmpty())labels.add(term);
        return labels;
    }
    public static String description(LocalDate date){Lunar l=lunar(date);return "农历 "+l.getMonthInChinese()+"月"+l.getDayInChinese()+" · "+l.getYearInGanZhi()+"年 "+l.getYearShengXiao()+" · "+label(date);}
}
