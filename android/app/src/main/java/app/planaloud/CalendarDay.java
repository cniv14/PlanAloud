package app.planaloud;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

final class CalendarDay {
    private static SharedPreferences annotations(MainActivity a) {
        return a.getSharedPreferences("calendar_annotations", 0);
    }

    static String annotation(MainActivity a, LocalDate date) {
        return annotations(a).getString(date.toString(), "");
    }

    static void edit(MainActivity a, LocalDate date, Runnable refresh) {
        LinearLayout form = Ui.padded(a);
        TextInputEditText input = Ui.field(a, form, "标注，例如小林的生日", annotation(a,date), false);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        new MaterialAlertDialogBuilder(a)
                .setTitle(date + " · 日历标注").setView(form)
                .setNegativeButton("取消", null)
                .setNeutralButton("清除", (dialog, which) -> {
                    annotations(a).edit().remove(date.toString()).apply();
                    refresh.run();
                })
                .setPositiveButton("保存", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    SharedPreferences.Editor editor = annotations(a).edit();
                    if (text.isEmpty()) editor.remove(date.toString());
                    else editor.putString(date.toString(), text);
                    editor.apply();
                    refresh.run();
                }).show();
    }

    static List<String> labels(MainActivity a, LocalDate date) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        String annotation = annotation(a,date);
        if (!annotation.isEmpty()) labels.add(annotation);
        labels.addAll(ChineseDates.festivals(date));
        String holiday = Holidays.name(date), status = Holidays.label(date);
        if (!holiday.isEmpty() && !labels.contains(holiday)) labels.add(holiday);
        if (!status.isEmpty()) labels.add(status);
        return new ArrayList<>(labels);
    }

    static void render(MainActivity a, LinearLayout cell, LocalDate date, boolean selected, int count,
                       Runnable choose, Runnable refresh) {
        List<String> labels = labels(a,date);
        String lunar = ChineseDates.dayLabel(date);
        String holidayMarker = Holidays.label(date);
        String number = Integer.toString(date.getDayOfMonth());
        TextView day = line(a, number, 15, selected);
        cell.addView(day);
        cell.addView(line(a, lunar, 10, false));
        String annotation = annotation(a,date);
        String marker = "";
        for (String label : labels) {
            if (!label.equals(annotation)) {marker=label;break;}
        }
        if (!holidayMarker.isEmpty() && !holidayMarker.equals(marker))
            marker = holidayMarker + (marker.isEmpty() ? "" : " " + marker);
        if (count > 0) marker += marker.isEmpty() ? "•" : " •";
        TextView special = line(a, marker.isEmpty() ? " " : marker, 9, true);
        boolean dark = AgendaWidget.dark(a);
        special.setTextColor(!marker.isEmpty() && !marker.equals("•") ? (dark ? 0xffffada6 : 0xffa12c34) : Ui.primary(a));
        cell.addView(special);
        TextView custom = line(a, annotation.isEmpty() ? " " : annotation, 9, true);
        custom.setTextColor(dark ? 0xffc7adff : 0xff673ea0);
        cell.addView(custom);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(Ui.dp(a, 10));
        bg.setColor(selected ? Ui.color(a, com.google.android.material.R.attr.colorPrimaryContainer) : android.graphics.Color.TRANSPARENT);
        if (date.equals(LocalDate.now())) bg.setStroke(Ui.dp(a, 1), Ui.primary(a));
        cell.setBackground(bg);
        cell.setContentDescription(date + "，农历" + lunar + (labels.isEmpty() ? "" : "，" + String.join("，", labels))
                + (count == 0 ? "" : "，" + count + "项安排"));
        cell.setOnClickListener(v -> choose.run());
        if (refresh != null) cell.setOnLongClickListener(v -> {edit(a,date,refresh);return true;});
    }

    private static TextView line(MainActivity a, String value, int size, boolean bold) {
        TextView view = Ui.text(a, value, size, bold);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine();
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return view;
    }
}
