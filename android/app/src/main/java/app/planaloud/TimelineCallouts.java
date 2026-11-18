package app.planaloud;

import android.text.*;
import java.util.*;

/** Packs nearby leader labels into two rows above and two below the full-width clock. */
final class TimelineCallouts {
    static final class Label {
        final DayEvents.Segment event;final int index,side,row;final float anchor,left,right;final String text;
        Label(DayEvents.Segment event,int index,int side,int row,float anchor,float left,float right,String text){this.event=event;this.index=index;this.side=side;this.row=row;this.anchor=anchor;this.left=left;this.right=right;this.text=text;}
    }
    static final class Layout {final List<Label> labels=new ArrayList<>();int next,above,below;}
    static Layout layout(List<DayEvents.Segment> events,int start,float width,TextPaint paint,float pad,float gap,float maxName){
        Layout out=new Layout();out.next=start;List<List<Label>> occupied=new ArrayList<>();for(int i=0;i<4;i++)occupied.add(new ArrayList<>());float available=Math.max(1,width-2*pad);
        for(int i=start;i<events.size();i++){
            DayEvents.Segment event=events.get(i);float anchor=pad+available*(event.start+event.end)/2880f;
            String title=TextUtils.ellipsize(event.title.replace('\n',' '),paint,Math.min(maxName,available*.42f),TextUtils.TruncateAt.END).toString();float length=Math.min(available,Math.max(gap*3,paint.measureText(title)+gap));float wanted=Math.min(width-pad-length,Math.max(pad,anchor+gap*2));
            float best=Float.MAX_VALUE,bestX=0;int chosen=-1;
            for(int track=0;track<4;track++){
                List<Float> candidates=new ArrayList<>(Arrays.asList(wanted,pad,width-pad-length));for(Label old:occupied.get(track)){candidates.add(old.right+gap);candidates.add(old.left-gap-length);}
                for(float x:candidates){if(x<pad||x+length>width-pad+.1f)continue;boolean free=true;for(Label old:occupied.get(track))if(x<old.right+gap&&x+length+gap>old.left){free=false;break;}if(!free)continue;
                    float cost=Math.abs(x-wanted)+(track/2)*gap*3+((track%2)!=(i%2)?gap/2:0);if(cost<best){best=cost;bestX=x;chosen=track;}
                }
            }
            if(chosen<0)break;
            Label label=new Label(event,i,chosen%2==0?-1:1,chosen/2,anchor,bestX,bestX+length,title);occupied.get(chosen).add(label);out.labels.add(label);out.next=i+1;if(label.side<0)out.above=Math.max(out.above,label.row+1);else out.below=Math.max(out.below,label.row+1);
        }return out;
    }
}
