package app.planaloud;

/** A captured drag owns all remaining events, including reversals and cancellation. */
final class PageSwipe {
    private final float slop,activation,commit;
    private float x,y;
    private boolean eligible,captured,cancelled,forward,backward;
    PageSwipe(float density,float touchSlop){slop=touchSlop;activation=Math.max(touchSlop*2,24*density);commit=64*density;}
    void begin(float x,float y,boolean eligible,boolean forward,boolean backward){this.x=x;this.y=y;this.eligible=eligible;this.forward=forward;this.backward=backward;captured=false;cancelled=false;}
    boolean move(float x,float y,int pointers){
        if(pointers!=1){eligible=false;cancelled=true;return captured;}
        if(captured||!eligible)return captured;
        float dx=x-this.x,dy=y-this.y;
        if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)*1.3f){eligible=false;return false;}
        if(Math.abs(dx)>activation&&Math.abs(dx)>Math.abs(dy)*1.7f&&allowed(dx))captured=true;
        return captured;
    }
    boolean captured(){return captured;}
    float delta(float x){return cancelled?0:x-this.x;}
    private boolean allowed(float dx){return dx<0?forward:backward;}
    int finish(float x,float y){float dx=x-this.x,dy=y-this.y;int result=captured&&!cancelled&&Math.abs(dx)>=commit&&Math.abs(dx)>Math.abs(dy)*1.7f&&allowed(dx)?(dx<0?1:-1):0;reset();return result;}
    void reset(){eligible=false;captured=false;cancelled=false;}
}
