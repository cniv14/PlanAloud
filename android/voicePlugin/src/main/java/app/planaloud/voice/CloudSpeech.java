package app.planaloud.voice;
import android.content.*;import android.util.Base64;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import org.json.*;
final class CloudSpeech {
 private final Context context;CloudSpeech(Context c){context=c;}
 private SharedPreferences prefs(){return context.getSharedPreferences("settings",0);}
 String transcribe(File wav,int mode)throws Exception{return mode==3?deepseek(wav):cloud(wav);}
 private String secret(String name)throws Exception{return (String)Class.forName("app.planaloud.Secrets").getMethod("get",Context.class,String.class).invoke(null,context,name);}
 private String deepseek(File wav)throws Exception{
  String token=secret("deepseek_voice_api_key");
  if(token.isEmpty()){
   URI agent=new URI(prefs().getString("api_url",""));
   if("https".equalsIgnoreCase(agent.getScheme())&&"api.deepseek.com".equalsIgnoreCase(agent.getHost()))token=secret("api_key");
  }
  return DeepSeekAsr.transcribe(wav,token);
 }
 private String cloud(File wav)throws Exception{String token=secret("voice_api_key");if(token.isEmpty())token=secret("api_key");if(token.isEmpty())throw new IOException("请配置百炼语音API Key");String data="data:audio/wav;base64,"+Base64.encodeToString(java.nio.file.Files.readAllBytes(wav.toPath()),Base64.NO_WRAP);JSONObject body=new JSONObject().put("model",prefs().getString("voice_model","qwen3-asr-flash")).put("stream",false).put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",new JSONArray().put(new JSONObject().put("type","input_audio").put("input_audio",new JSONObject().put("data",data)))))).put("asr_options",new JSONObject().put("language","zh"));HttpURLConnection connection=(HttpURLConnection)new URL(prefs().getString("voice_endpoint","")+"/chat/completions").openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(120000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Authorization","Bearer "+token);connection.setRequestProperty("Content-Type","application/json");try{try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}if(connection.getResponseCode()!=200)throw new IOException("百炼语音 HTTP "+connection.getResponseCode()+"，请检查地域、模型及密钥");try(InputStream in=connection.getInputStream()){ByteArrayOutputStream response=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(response.size()+n>1024*1024)throw new IOException("识别响应过大");response.write(buffer,0,n);}return new JSONObject(response.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");}}finally{connection.disconnect();}}
}
