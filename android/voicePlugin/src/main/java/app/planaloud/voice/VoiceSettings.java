package app.planaloud.voice;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

/** Removes obsolete model/engine files without touching recordings, notes or account credentials. */
public final class VoiceSettings {
    public static void migrate(Context context){
        SharedPreferences prefs=context.getSharedPreferences("settings",0);
        if(!prefs.getBoolean("voice_default_152",false)){
            int provider=prefs.getInt("voice_provider",3);
            boolean official=false;
            try{java.net.URI endpoint=new java.net.URI(prefs.getString("api_url",""));official="https".equalsIgnoreCase(endpoint.getScheme())&&"api.deepseek.com".equalsIgnoreCase(endpoint.getHost());}catch(java.net.URISyntaxException ignored){}
            if(provider==2||(provider==0&&official))provider=3;
            prefs.edit().putInt("voice_provider",provider).putBoolean("voice_default_152",true).apply();
        }
        if(prefs.getBoolean("voice_local_removed_152",false))return;
        File directory=new File(context.getFilesDir(),"voice");boolean removed=true;File[] files=directory.listFiles();
        if(files!=null)for(File file:files)if(file.isFile()&&file.getName().matches("ggml-model\\.bin|model\\.part|download\\.part|engine\\.json(?:\\.bak|\\.new)?|engine-[0-9]+\\.so|engine\\.part|engine-library\\.part"))removed&=file.delete();
        if(removed){if(files!=null)directory.delete();prefs.edit().remove("voice_engine_highwater").putBoolean("voice_local_removed_152",true).apply();}
    }
    private VoiceSettings(){}
}
