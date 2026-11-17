package app.planaloud;

import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.*;
import com.google.android.material.checkbox.MaterialCheckBox;

/** Hands-on onboarding practice using the same icon, task and time visuals as the app. */
final class GuidePractice {
    static void add(MainActivity a,LinearLayout page){
        com.google.android.material.card.MaterialCardView card=Ui.card(a);LinearLayout body=Ui.padded(a);card.addView(body);page.addView(card);
        body.addView(Ui.text(a,"先动手试一试",20,true));body.addView(Ui.help(a,"点选图标切换示例，再勾选待办，体验完成与恢复。"));
        LinearLayout nav=Ui.row(a);body.addView(nav);TextView panel=Ui.text(a,"今天：用新建待办记录要做的事",15,true);String[] names={"今天","日程","设置"},samples={"今天：用新建待办记录要做的事","日程：点击日期查看安排，周表可以向任意方向拖动","设置：更改外观、开启同步、查找更多功能"};int[] indices={0,2,4};ImageButton[] icons=new ImageButton[3];
        for(int i=0;i<3;i++){
            int selected=i;LinearLayout cell=Ui.column(a);cell.setGravity(Gravity.CENTER);ImageButton icon=new ImageButton(a);icon.setImageDrawable(NavigationIcons.icon(a,indices[i]));icon.setBackgroundColor(android.graphics.Color.TRANSPARENT);icon.setContentDescription("练习切换到"+names[i]);icons[i]=icon;cell.addView(icon,new LinearLayout.LayoutParams(Ui.dp(a,48),Ui.dp(a,48)));cell.addView(Ui.help(a,names[i]));nav.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
            icon.setOnClickListener(v->{for(int j=0;j<icons.length;j++)if(icons[j]!=null){icons[j].setSelected(j==selected);icons[j].setImageTintList(ColorStateList.valueOf(j==selected?Ui.primary(a):Ui.muted(a)));}panel.setText(samples[selected]);});
        }
        icons[0].performClick();body.addView(panel);body.addView(new DayTimeline(a),new LinearLayout.LayoutParams(-1,Ui.dp(a,92)));
        MaterialCheckBox check=new MaterialCheckBox(a);check.setText("练习：完成一项待办");body.addView(check);TextView result=Ui.help(a,"勾选后会看到完成反馈");body.addView(result);check.setOnCheckedChangeListener((button,on)->result.setText(on?"示例待办已完成 ✓ 再点一次可以恢复":"示例待办已恢复，可以再次勾选"));
    }
}
