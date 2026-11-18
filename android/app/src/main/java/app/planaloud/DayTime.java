package app.planaloud;
import java.time.LocalTime;
import java.util.Locale;
final class DayTime {
    static final int MINUTES=1440;
    static int minute(LocalTime time){return time.getHour()*60+time.getMinute();}
    static int percent(LocalTime time){return minute(time)*100/MINUTES;}
    static String label(LocalTime time){return String.format(Locale.CHINA,"%02d:%02d · 今日已过 %d%%",time.getHour(),time.getMinute(),percent(time));}
}
