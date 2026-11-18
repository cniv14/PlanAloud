package app.planaloud;

import java.util.*;

/** One ordering for visible tabs, swipe destinations and selection IDs. */
final class HomeNavigation {
    static final String[] TABS={"agenda","note","course","inbox","settings","week"};
    static final int SETTINGS=1<<4, WEEK=1<<5;
    static int mask(boolean today,boolean notes,boolean calendar,boolean inbox){return SETTINGS|WEEK|(today?1:0)|(notes?2:0)|(calendar?4:0)|(inbox?8:0);}
    static int itemId(String page){return 100+Arrays.asList(TABS).indexOf(page);}
    static boolean visible(int mask,String page){int i=itemId(page)-100;return i>=0&&i<TABS.length&&(mask&(1<<i))!=0;}
    static List<String> pages(int mask){List<String> pages=new ArrayList<>();for(String tab:new String[]{"agenda","week","note","course","inbox","settings"})if(visible(mask,tab))pages.add(tab);return pages;}
    static String adjacent(int mask,String current,boolean forward){List<String> pages=pages(mask);int i=pages.indexOf(current);if(i<0)return null;int next=i+(forward?1:-1);return next<0||next>=pages.size()?null:pages.get(next);}
    static String first(int mask){return pages(mask).get(0);}
}
