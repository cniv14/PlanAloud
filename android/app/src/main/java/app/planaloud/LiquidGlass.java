package app.planaloud;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Translucent surfaces over the shared wallpaper, with a directional highlight along the rim. */
final class LiquidGlass {
    private LiquidGlass() {}

    static boolean dark(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    static int tint(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    static void outline(View view, float radius) {
        Surface rim = new Surface(view.getContext(), radius, false);
        rim.setBounds(0, 0, view.getWidth(), view.getHeight());
        view.getOverlay().add(rim);
        view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->rim.setBounds(0,0,r-l,b-t));
    }

    static Drawable panel(Context context, float radius) {
        return new Surface(context, radius, true);
    }

    static Drawable navSelection(Context context) {
        int color = Ui.color(context, com.google.android.material.R.attr.colorSecondaryContainer);
        int inset = Ui.dp(context, 3);
        float radius = Ui.dp(context, 12);
        return new Drawable() {
            @Override public ConstantState getConstantState() {
                return new ConstantState() {
                    @Override public Drawable newDrawable() { return navSelection(context); }
                    @Override public int getChangingConfigurations() { return 0; }
                };
            }

            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            {
                paint.setColor(tint(color, 226));
            }

            @Override public boolean isStateful() { return true; }

            @Override protected boolean onStateChange(int[] state) {
                invalidateSelf();
                return true;
            }

            @Override public void draw(Canvas canvas) {
                boolean checked = false;
                for (int state : getState()) {
                    if (state == android.R.attr.state_checked || state == android.R.attr.state_selected) {
                        checked = true;
                        break;
                    }
                }
                if (!checked) return;
                Rect bounds = getBounds();
                float half = Math.min(bounds.width(), bounds.height()) / 2f - inset;
                if (half <= 0) return;
                float x = bounds.exactCenterX();
                float y = bounds.exactCenterY();
                canvas.drawRoundRect(x-half,y-half,x+half,y+half,radius,radius,paint);
            }

            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
            @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        };
    }

    private static final class Surface extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float radius, stroke;
        private final int base, accent, edge;
        private final boolean filled, dark;
        private LinearGradient fill, rim;

        Surface(Context context, float radius, boolean filled) {
            this.radius = Ui.dp(context, radius);
            stroke = Math.max(1f, context.getResources().getDisplayMetrics().density * .7f);
            this.filled = filled;
            dark = dark(context);
            base = Ui.surface(context);
            accent = Ui.primary(context);
            edge = Ui.color(context, com.google.android.material.R.attr.colorOutlineVariant);
        }

        @Override protected void onBoundsChange(Rect bounds) {
            rect.set(bounds);
            if (!filled) rect.inset(stroke / 2, stroke / 2);
            if (rect.isEmpty()) return;
            if (filled) {
                fill = new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                        new int[]{tint(base, 235), tint(base, 194), tint(
                                androidx.core.graphics.ColorUtils.blendARGB(base, accent, .11f), 226)},
                        null, Shader.TileMode.CLAMP);
                return;
            }
            rim = new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                    new int[]{dark ? 0x93ffffff : 0xf5ffffff, tint(edge, dark ? 45 : 104),
                            tint(accent, dark ? 124 : 94)}, null, Shader.TileMode.CLAMP);
        }

        @Override public void draw(Canvas canvas) {
            if (rect.isEmpty()) return;
            if (filled) {
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(fill);
                canvas.drawRoundRect(rect, radius, radius, paint);
                return;
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setShader(rim);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
