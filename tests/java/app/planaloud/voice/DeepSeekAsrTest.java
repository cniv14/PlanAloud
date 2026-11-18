package app.planaloud.voice;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public final class DeepSeekAsrTest {
    public static void main(String[] args)throws Exception{
        File wav=File.createTempFile("asr-test-",".wav");
        byte[] audio=new byte[]{'R','I','F','F',0,1,2,3,(byte)255};Files.write(wav.toPath(),audio);
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();DeepSeekAsr.multipart(out,"test-boundary",wav);
            byte[] actual=out.toByteArray();
            String header="--test-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n";
            int start=header.getBytes(StandardCharsets.US_ASCII).length;
            if(!new String(actual,0,start,StandardCharsets.US_ASCII).equals(header))throw new AssertionError("Multipart header");
            if(!Arrays.equals(Arrays.copyOfRange(actual,start,start+audio.length),audio))throw new AssertionError("Audio bytes changed");
            if(!new String(actual,start+audio.length,actual.length-start-audio.length,StandardCharsets.US_ASCII).equals("\r\n--test-boundary--\r\n"))throw new AssertionError("Multipart terminator");
            System.out.println("PASS 3 DeepSeek multipart checks");
        }finally{wav.delete();}
    }
}
