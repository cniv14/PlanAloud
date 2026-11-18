package app.planaloud.voice;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.JSONObject;

/** Official non-streaming ASR: https://api-docs.deepseek.com/zh-cn/api/asr */
final class DeepSeekAsr {
    static void multipart(OutputStream out,String boundary,File wav)throws IOException{
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        try(InputStream in=new FileInputStream(wav)){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);}
        out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.US_ASCII));
    }
    static String transcribe(File wav,String token)throws Exception{
        if(token.isEmpty())throw new IOException("请配置 DeepSeek API Key");
        if(wav.length()>8L*1024*1024)throw new IOException("DeepSeek ASR 音频不能超过8MiB");
        if(wav.length()<=44)throw new IOException("录音为空，请重试");
        String boundary="PlanAloud-"+UUID.randomUUID();
        HttpURLConnection connection=(HttpURLConnection)new URL("https://api.deepseek.com/v1/tools/asr").openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(120000);
        connection.setRequestMethod("POST");connection.setDoOutput(true);
        connection.setRequestProperty("Authorization","Bearer "+token);
        connection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
        connection.setChunkedStreamingMode(8192);
        try{
            try(OutputStream out=connection.getOutputStream()){multipart(out,boundary,wav);}
            int code=connection.getResponseCode();
            if(code!=200)throw new IOException("DeepSeek ASR HTTP "+code+(code==401?"，请检查密钥":code==429?"，请稍后重试":"，请检查音频或服务状态"));
            try(InputStream in=connection.getInputStream()){
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;
                while((n=in.read(block))!=-1){if(bytes.size()+n>1024*1024)throw new IOException("识别响应过大");bytes.write(block,0,n);}
                return new JSONObject(bytes.toString("UTF-8")).getString("text");
            }
        }finally{connection.disconnect();}
    }
}
