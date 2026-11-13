package app.planaloud;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Local USB/ADB bridge only. Binder identity is checked before clearing it. */
public final class CommandProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private Context requireShell(){Context c=getContext();int uid=Binder.getCallingUid();if(uid!=2000&&uid!=0)throw new SecurityException("只接受 ADB shell");if(c==null||!DayCommands.prefs(c).getBoolean("command_adb",false))throw new SecurityException("请先在序言指令台开启 ADB 指令");return c;}
    public Bundle call(String method,String arg,Bundle extras){Context c=requireShell();long token=Binder.clearCallingIdentity();JSONObject response;
        try{
            if(!"run".equals(method)||arg==null||arg.length()>160000)throw new IOException("无效命令请求；大文件使用上传入口");
            String json;if(arg.startsWith("@"))try(InputStream in=new FileInputStream(staged(c,arg.substring(1)))){json=Network.read(in,1024*1024);}else json=new String(Base64.decode(arg,Base64.URL_SAFE|Base64.NO_WRAP),StandardCharsets.UTF_8);JSONObject req=new JSONObject(json);JSONObject params=req.optJSONObject("args");
            JSONObject result=DayCommands.execute(c,req.getString("command"),params==null?new JSONObject():params,new DayCommands.Access("adb"));response=new JSONObject().put("ok",true).put("result",result);
        }catch(Exception e){response=error(e.getMessage());}finally{Binder.restoreCallingIdentity(token);}
        byte[] bytes=response.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>256*1024)bytes=error("返回内容过大，请分页或使用文件导出").toString().getBytes(StandardCharsets.UTF_8);
        Bundle out=new Bundle();out.putString("reply",Base64.encodeToString(bytes,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));return out;
    }
    private static JSONObject error(String message){try{return new JSONObject().put("ok",false).put("error",message==null?"命令未完成":message);}catch(JSONException e){return new JSONObject();}}
    static File staged(Context c,String id)throws IOException{if(!id.matches("[a-f0-9-]{36}"))throw new IOException("无效的临时文件标识");return new File(c.getCacheDir(),"command-input/"+id);}
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        Context c=requireShell();long token=Binder.clearCallingIdentity();File temporary=null;
        try{
            List<String> parts=uri.getPathSegments();String kind=parts.isEmpty()?"":parts.get(0);
            if("w".equals(mode)&&kind.equals("input")&&parts.size()==2){File target=staged(c,parts.get(1));File dir=target.getParentFile();if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法准备上传目录");File[] files=dir.listFiles();if(files!=null)for(File f:files)if(f.lastModified()<System.currentTimeMillis()-86400000L)f.delete();return ParcelFileDescriptor.open(target,ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE|ParcelFileDescriptor.MODE_WRITE_ONLY);}
            if(!"r".equals(mode))throw new IOException("不支持此文件模式");
            temporary=File.createTempFile("command-export-",".data",c.getCacheDir());
            try(OutputStream out=new FileOutputStream(temporary)){
                if(kind.equals("backup")&&parts.size()==1)Data.get(c).writeExport(out);
                else if(kind.equals("notes")&&parts.size()==1)Data.get(c).exportMarkdownZip(out);
                else if(kind.equals("record")&&parts.size()==2&&parts.get(1).matches("[a-f0-9-]{36}")){JSONObject r=Data.get(c).find(parts.get(1));if(r==null||r.optBoolean("deleted"))throw new IOException("事项不存在");out.write(r.toString().getBytes(StandardCharsets.UTF_8));}
                else throw new IOException("未知导出路径");
            }
            return ParcelFileDescriptor.open(temporary,ParcelFileDescriptor.MODE_READ_ONLY);
        }catch(Exception e){throw new FileNotFoundException(e.getMessage());}finally{if(temporary!=null)temporary.delete();Binder.restoreCallingIdentity(token);}
    }
    public int delete(Uri uri,String s,String[] args){Context c=requireShell();List<String> p=uri.getPathSegments();try{return p.size()==2&&p.get(0).equals("input")&&staged(c,p.get(1)).delete()?1:0;}catch(IOException e){return 0;}}
    public Cursor query(Uri uri,String[] p,String s,String[] a,String order){throw new UnsupportedOperationException();}
    public String getType(Uri uri){return "application/octet-stream";}
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues values,String s,String[] args){throw new UnsupportedOperationException();}
}
