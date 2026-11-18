package app.planaloud;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import androidx.appcompat.content.res.AppCompatResources;
final class NavigationIcons {
    static Drawable icon(Context c,int index){
        int[] normal={R.drawable.ic_calendar,R.drawable.ic_note,R.drawable.ic_schedule,R.drawable.ic_inbox,R.drawable.ic_settings,R.drawable.ic_schedule};
        int[] selected={R.drawable.ic_calendar_selected,R.drawable.ic_note_selected,R.drawable.ic_schedule_selected,R.drawable.ic_inbox_selected,R.drawable.ic_settings_selected,R.drawable.ic_schedule_selected};
        StateListDrawable icon=new StateListDrawable();icon.addState(new int[]{android.R.attr.state_checked},AppCompatResources.getDrawable(c,selected[index]));icon.addState(new int[]{android.R.attr.state_selected},AppCompatResources.getDrawable(c,selected[index]));icon.addState(new int[]{},AppCompatResources.getDrawable(c,normal[index]));return icon;
    }
}
