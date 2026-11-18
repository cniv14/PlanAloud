package app.planaloud;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class HomeNavigationTest {
    @Test public void defaultSwipesSkipHiddenTabsAndKeepWeekUnderToday(){
        int mask=HomeNavigation.mask(true,false,true,false);
        assertEquals(Arrays.asList("agenda","week","course","settings"),HomeNavigation.pages(mask));
        assertEquals("course",HomeNavigation.adjacent(mask,"week",true));
        assertEquals("week",HomeNavigation.adjacent(mask,"course",false));
        assertEquals(100,HomeNavigation.itemId("week"));
        assertNull(HomeNavigation.adjacent(mask,"note",true));
    }
    @Test public void allVisibilityCombinationsHaveSymmetricBoundedRoutes(){
        for(int bits=0;bits<16;bits++){
            int mask=HomeNavigation.mask((bits&1)!=0,(bits&2)!=0,(bits&4)!=0,(bits&8)!=0);
            List<String> pages=HomeNavigation.pages(mask);
            assertEquals("settings",pages.get(pages.size()-1));
            assertNull(HomeNavigation.adjacent(mask,pages.get(0),false));
            assertNull(HomeNavigation.adjacent(mask,"settings",true));
            for(int i=0;i<pages.size();i++){
                assertTrue(HomeNavigation.visible(mask,pages.get(i)));
                if(i>0)assertEquals(pages.get(i-1),HomeNavigation.adjacent(mask,pages.get(i),false));
                if(i+1<pages.size())assertEquals(pages.get(i+1),HomeNavigation.adjacent(mask,pages.get(i),true));
            }
            assertEquals((bits&1)!=0,pages.contains("week"));
        }
    }
    @Test public void dragDoesNotLeakEventsWhenReversedOrInterrupted(){
        PageSwipe swipe=new PageSwipe(1,8);
        swipe.begin(200,100,true,true,true);
        assertTrue(swipe.move(140,100,1));
        assertTrue(swipe.move(195,100,1));
        assertEquals(0,swipe.finish(195,100));
        swipe.begin(200,100,true,true,true);
        assertTrue(swipe.move(120,100,1));
        assertTrue(swipe.move(80,100,2));
        assertEquals(0,swipe.finish(20,100));
    }
    @Test public void verticalScrollInteractiveControlsAndBoundariesDoNotTurnPages(){
        PageSwipe swipe=new PageSwipe(1,8);
        swipe.begin(100,100,true,true,true);
        assertFalse(swipe.move(105,130,1));assertFalse(swipe.move(0,130,1));assertEquals(0,swipe.finish(0,130));
        swipe.begin(100,100,false,true,true);assertFalse(swipe.move(0,100,1));assertEquals(0,swipe.finish(0,100));
        swipe.begin(100,100,true,false,true);assertFalse(swipe.move(0,100,1));assertEquals(0,swipe.finish(0,100));
    }
    @Test public void committedSwipesUseBothDistanceAndDirection(){
        PageSwipe swipe=new PageSwipe(1,8);
        swipe.begin(200,100,true,true,true);assertTrue(swipe.move(170,100,1));assertEquals(0,swipe.finish(150,100));
        swipe.begin(200,100,true,true,true);assertTrue(swipe.move(100,110,1));assertEquals(1,swipe.finish(100,110));
        swipe.begin(200,100,true,true,true);assertTrue(swipe.move(300,110,1));assertEquals(-1,swipe.finish(300,110));
        swipe.begin(200,100,true,true,true);assertTrue(swipe.move(100,100,1));swipe.reset();assertEquals(0,swipe.finish(100,100));
    }
}
