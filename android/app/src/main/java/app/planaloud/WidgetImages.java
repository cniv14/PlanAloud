package app.planaloud;

import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.widget.RemoteViews;

/** Keep launcher image actions independent of RemoteViews' shared bitmap index cache. */
final class WidgetImages {
    private WidgetImages(){}
    static void set(RemoteViews views,int viewId,Bitmap image){
        views.setImageViewIcon(viewId,Icon.createWithBitmap(image));
    }
}
