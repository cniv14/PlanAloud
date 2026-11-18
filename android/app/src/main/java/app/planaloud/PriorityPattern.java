package app.planaloud;
import android.graphics.*;
import org.json.JSONObject;
/** Priority is a small shape in a reserved left gutter, never a card outline. */
final class PriorityPattern {
    static boolean present(JSONObject r){return !r.optString("priority").isEmpty();}
    static int color(JSONObject r){switch(r.optString("priority")){case "urgent":return 0xffe45139;case "high":return 0xff9654c7;case "normal":return 0xff3686c9;case "low":return 0xff38976b;default:return 0;}}
    static String label(JSONObject r){switch(r.optString("priority")){case "urgent":return "▲ 紧急";case "high":return "✦ 重要";case "normal":return "◆ 普通";case "low":return "● 低优先";default:return "未设定";}}
    static void draw(Canvas c,JSONObject r,RectF area){if(!present(r)||area.width()<18||area.height()<10)return;float size=Math.min(12,area.height()-4),x=area.left+9,y=area.centerY(),half=size/2;Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color(r));Path path=new Path();switch(r.optString("priority")){case "urgent":path.moveTo(x,y-half);path.lineTo(x+half,y+half);path.lineTo(x-half,y+half);path.close();c.drawPath(path,p);p.setColor(Color.WHITE);p.setStrokeWidth(1.2f);c.drawLine(x,y-half*.35f,x,y+half*.32f,p);c.drawCircle(x,y+half*.65f,.7f,p);break;case "high":for(int i=0;i<8;i++){double a=-Math.PI/2+i*Math.PI/4;float radius=i%2==0?half:half*.38f;float px=x+(float)Math.cos(a)*radius,py=y+(float)Math.sin(a)*radius;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();c.drawPath(path,p);break;case "normal":path.moveTo(x,y-half);path.lineTo(x+half,y);path.lineTo(x,y+half);path.lineTo(x-half,y);path.close();c.drawPath(path,p);break;case "low":c.drawCircle(x,y,half*.65f,p);break;default:break;}}
    static Bitmap bitmap(JSONObject r){Bitmap result=Bitmap.createBitmap(54,54,Bitmap.Config.ARGB_8888);result.setDensity(Bitmap.DENSITY_NONE);Canvas c=new Canvas(result);c.scale(3,3);draw(c,r,new RectF(0,0,18,18));return result;}
    static void drawScaled(Canvas c,JSONObject r,RectF area,float density){c.save();c.translate(area.left,area.top);c.scale(density,density);draw(c,r,new RectF(0,0,area.width()/density,area.height()/density));c.restore();}
}
