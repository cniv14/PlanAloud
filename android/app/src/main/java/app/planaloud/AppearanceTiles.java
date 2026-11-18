package app.planaloud;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.google.android.material.card.MaterialCardView;

final class AppearanceTiles {
    static void add(MainActivity a,LinearLayout page){
        LinearLayout row=Ui.row(a);row.setGravity(Gravity.TOP);page.addView(row);
        ImageView icon=new ImageView(a);icon.setImageResource(ProviderAppearance.current(a.prefs).icon);icon.setContentDescription("当前应用图标预览 · "+ProviderAppearance.current(a.prefs).label);
        Ui.cell(a,row,tile(a,"应用图标",ProviderAppearance.current(a.prefs).label,icon,()->ProviderAppearance.show(a)));
        Ui.cell(a,row,tile(a,"桌面小组件","布局与背景",new MiniWidget(a),()->new SettingsPanels(a).widget(()->new SettingsPanels(a).appearance())));
    }
    private static MaterialCardView tile(MainActivity a,String title,String detail,View preview,Runnable action){
        MaterialCardView card=new MaterialCardView(a){@Override protected void onMeasure(int w,int h){super.onMeasure(w,MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(w),MeasureSpec.EXACTLY));}};
        card.setRadius(Ui.dp(a,20));card.setCardElevation(0);card.setStrokeWidth(Ui.dp(a,1));card.setStrokeColor(Ui.color(a,com.google.android.material.R.attr.colorOutlineVariant));card.setCardBackgroundColor(LiquidGlass.tint(Ui.color(a,com.google.android.material.R.attr.colorSurfaceContainerLow),185));
        LinearLayout body=Ui.column(a);body.setGravity(Gravity.CENTER);body.setPadding(Ui.dp(a,8),Ui.dp(a,8),Ui.dp(a,8),Ui.dp(a,8));body.addView(preview,new LinearLayout.LayoutParams(Ui.dp(a,58),Ui.dp(a,58)));
        TextView name=Ui.text(a,title,16,true);name.setGravity(Gravity.CENTER);body.addView(name);TextView caption=Ui.help(a,detail);caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.setGravity(Gravity.CENTER);body.addView(caption);card.addView(body,new android.widget.FrameLayout.LayoutParams(-1,-1));card.setContentDescription(title+"，点击打开设置");card.setOnClickListener(v->action.run());return card;
    }
    private static final class MiniWidget extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MiniWidget(Context c){super(c);setContentDescription("小组件布局预览");}
        @Override protected void onDraw(Canvas canvas){
            float w=getWidth(),h=getHeight();paint.setColor(LiquidGlass.tint(Ui.primary(getContext()),55));canvas.drawRoundRect(0,3,w,h-3,8,8,paint);
            paint.setColor(Ui.primary(getContext()));canvas.drawRoundRect(w*.12f,h*.18f,w*.6f,h*.23f,2,2,paint);canvas.drawRoundRect(w*.12f,h*.32f,w*.83f,h*.36f,2,2,paint);
            boolean grid="grid".equals(getContext().getSharedPreferences("settings",0).getString("widget_layout","grid"));paint.setColor(LiquidGlass.tint(Ui.primary(getContext()),100));
            for(int y=0;y<2;y++)for(int x=0;x<(grid?2:1);x++){float l=w*(.12f+(grid?.4f:0)*x),r=l+w*(grid?.34f:.74f),t=h*(.48f+y*.19f);canvas.drawRoundRect(l,t,r,t+h*.13f,2,2,paint);}
        }
    }
}
