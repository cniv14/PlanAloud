package app.planaloud;
import android.view.*;
import android.widget.TextView;
import android.util.TypedValue;
import java.util.*;
/** Counteracts zoom for text while keeping transformed card geometry and hit targets. */
final class WeekTextScale {
    interface CanvasText {void textZoom(float zoom);}
    private final View root;private final Map<TextView,Float> sizes=new IdentityHashMap<>();private final Map<TextView,Integer> lines=new IdentityHashMap<>();
    WeekTextScale(View root){this.root=root;capture(root);}
    private void capture(View view){if(view instanceof TextView text){sizes.put(text,text.getTextSize());lines.put(text,text.getMaxLines());}if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)capture(group.getChildAt(i));}
    void apply(float zoom){float factor=Math.max(1,zoom);for(Map.Entry<TextView,Float> entry:sizes.entrySet()){TextView text=entry.getKey();text.setTextSize(TypedValue.COMPLEX_UNIT_PX,entry.getValue()/factor);int max=lines.get(text);if(max>0&&max<Integer.MAX_VALUE)text.setMaxLines((int)Math.min(Integer.MAX_VALUE,Math.round(max*(double)factor)));}canvas(root,factor);}
    private void canvas(View view,float zoom){if(view instanceof CanvasText text)text.textZoom(zoom);if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)canvas(group.getChildAt(i),zoom);}
}
