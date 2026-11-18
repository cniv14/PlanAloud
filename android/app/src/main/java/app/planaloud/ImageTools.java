package app.planaloud;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.canhub.cropper.CropImageView;
import java.io.*;
import java.util.function.Consumer;

/** Bounded, EXIF-aware crop input; the app owns output encoding and content URI delivery. */
final class ImageTools {
    static void crop(MainActivity a,Uri input,Consumer<Uri> next){crop(a,input,"裁剪截图",next);}
    static void crop(MainActivity a,Uri input,String title,Consumer<Uri> next){
        crop(a,input,title,next,a::home);
    }
    static void crop(MainActivity a,Uri input,String title,Consumer<Uri> next,Runnable back){
        LinearLayout page=a.editorPage(title,back);TextView help=Ui.help(a,"拖动边框调整范围，双指缩放图片；也可以旋转或使用整图。");help.setPadding(Ui.dp(a,16),Ui.dp(a,12),Ui.dp(a,16),0);page.addView(help);
        FrameLayout area=new FrameLayout(a);CropImageView crop=new CropImageView(a);area.addView(crop,new FrameLayout.LayoutParams(-1,-1));
        View blocker=new View(a);blocker.setOnTouchListener((v,event)->true);area.addView(blocker,new FrameLayout.LayoutParams(-1,-1));page.addView(area,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout buttons=Ui.row(a);page.addView(buttons);boolean[] working={true},ready={false};Bitmap[] source={null};Uri[] output={null};
        buttons.addView(Ui.button(a,"旋转",false,()->{if(ready[0]&&!working[0])crop.rotateImage(90);}),new LinearLayout.LayoutParams(0,-2,1));
        buttons.addView(Ui.button(a,"使用整图",false,()->{if(!working[0])deliver(a,input,next);}),new LinearLayout.LayoutParams(0,-2,1));
        buttons.addView(Ui.button(a,"完成裁剪",true,()->{
            if(!ready[0]||working[0])return;working[0]=true;blocker.setVisibility(View.VISIBLE);help.setText("正在保存裁剪结果…");
            a.job(()->{
                try{
                    Bitmap result=crop.getCroppedImage(1600,1600,CropImageView.RequestSizeOptions.RESIZE_INSIDE);
                    if(result==null)throw new IOException("无法获得裁剪范围，请调整后重试");
                    // A crop can return the displayed bitmap itself. Do not recycle a bitmap still owned by the view.
                    output[0]=saveCrop(a,result);return "裁剪已保存";
                }catch(OutOfMemoryError memory){throw new IOException("图片占用内存过大，请缩小范围后重试",memory);}
            },()->{working[0]=false;blocker.setVisibility(View.GONE);if(crop.isAttachedToWindow())deliver(a,output[0],next);},error->{working[0]=false;blocker.setVisibility(View.GONE);help.setText(error+"；也可以使用整图。");});
        }),new LinearLayout.LayoutParams(0,-2,1));
        a.job(()->{try{source[0]=WallpaperImages.decode(a,input,1800);return "图片已就绪";}catch(OutOfMemoryError memory){throw new IOException("图片过大，请换用较小图片",memory);}},()->{
            working[0]=false;crop.setImageBitmap(source[0]);ready[0]=true;blocker.setVisibility(View.GONE);
        },error->{working[0]=false;blocker.setVisibility(View.GONE);help.setText("无法载入裁剪预览："+error+"。可尝试使用整图。");});
    }
    static Uri saveCrop(MainActivity a,Bitmap image)throws IOException{
        File dir=new File(a.getCacheDir(),"shared-images");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建裁剪缓存");
        File[] stale=dir.listFiles((parent,name)->name.startsWith("crop-")&&name.endsWith(".jpg"));if(stale!=null)for(File old:stale)if(System.currentTimeMillis()-old.lastModified()>86400000L)old.delete();
        File file=File.createTempFile("crop-",".jpg",dir);
        try(OutputStream out=new FileOutputStream(file)){if(!image.compress(Bitmap.CompressFormat.JPEG,90,out))throw new IOException("裁剪图片保存失败");}
        catch(IOException failure){file.delete();throw failure;}
        return FileProvider.getUriForFile(a,"app.planaloud.files",file);
    }
    private static void deliver(MainActivity a,Uri result,Consumer<Uri> next){try{if(result==null)throw new IllegalStateException("裁剪结果不可用");next.accept(result);}catch(RuntimeException failure){Diagnostics.record(a,"image-crop-result",failure);a.message("无法应用裁剪结果，请重试："+failure.getMessage());}}
}
