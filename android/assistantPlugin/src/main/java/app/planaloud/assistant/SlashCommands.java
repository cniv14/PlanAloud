package app.planaloud.assistant;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.material.button.MaterialButton;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

final class SlashCommands {
    private final EditText input;
    private final LinearLayout hints;
    private final ChatEngine engine;
    private JSONArray commands = new JSONArray(), pages = new JSONArray();

    SlashCommands(MainActivity activity, EditText input, LinearLayout root, LinearLayout compose, ChatEngine engine) {
        this.input = input;
        this.engine = engine;
        hints = new LinearLayout(activity);
        hints.setOrientation(LinearLayout.VERTICAL);
        hints.setVisibility(View.GONE);
        root.addView(hints, root.indexOfChild(compose));
        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void afterTextChanged(Editable s){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){update();}
        });
    }

    void load(MainActivity activity) throws Exception {
        JSONArray list=call("help",new JSONObject()).getJSONArray("commands");
        JSONArray screens=call("screen.list",new JSONObject()).getJSONArray("pages");
        activity.runOnUiThread(()->{commands=list;pages=screens;update();});
    }

    private void update() {
        String value=input.getText().toString();
        hints.removeAllViews();
        if(!value.startsWith("/")||value.contains("{")){
            hints.setVisibility(View.GONE);
            return;
        }
        String query=value.substring(1).toLowerCase(Locale.ROOT);
        for(int i=0,added=0;i<pages.length()&&added<2;i++){
            JSONObject page=pages.optJSONObject(i);
            String key=page.optString("page"), title=page.optString("title");
            if((key+" "+title).toLowerCase(Locale.ROOT).contains(query)){
                add("/"+key+"  "+title,"/"+key);
                added++;
            }
        }
        for(int i=0;i<commands.length()&&hints.getChildCount()<4;i++){
            JSONObject command=commands.optJSONObject(i);
            String key=command.optString("name"), title=command.optString("description");
            if((key+" "+title).toLowerCase(Locale.ROOT).contains(query)){
                JSONObject example=command.optJSONObject("arguments");
                add("/"+key+"  "+title,"/"+key+(example==null||example.length()==0?"":" "+example));
            }
        }
        hints.setVisibility(hints.getChildCount()==0?View.GONE:View.VISIBLE);
    }

    private void add(String label,String value) {
        MaterialButton button=new MaterialButton(input.getContext(),null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(label);
        button.setSingleLine(true);
        button.setAllCaps(false);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setOnClickListener(v->{input.setText(value);input.setSelection(input.length());});
        hints.addView(button,new LinearLayout.LayoutParams(-1,-2));
    }

    JSONObject run(String value) throws Exception {
        String command=value.substring(1).trim();
        if(command.isEmpty())return new JSONObject().put("message","输入 /calendar 打开日程，或使用 /help 查看指令。");
        int split=command.indexOf(' ');
        String name=split<0?command:command.substring(0,split);
        String args=split<0?"":command.substring(split+1).trim();
        if(args.isEmpty()){
            for(int i=0;i<pages.length();i++){
                if(name.equals(pages.getJSONObject(i).optString("page")))
                    return call("screen.open",new JSONObject().put("page",name));
            }
        }
        if(name.equals("sync.login")||name.equals("sync.connect-taskflow"))
            return new JSONObject().put("screen",name.equals("sync.login")?"account":"taskflow");
        return call(name,args.isEmpty()?new JSONObject():new JSONObject(args));
    }

    private JSONObject call(String name,JSONObject args) throws Exception {
        return engine.call("assistant_local_command",new JSONObject().put("command",name).put("args",args));
    }
}
