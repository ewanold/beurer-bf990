package org.beurer.bf990;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import java.util.Locale;

/**
 * Illustrative scale, not a diagnosis. The green band marks the healthy range from the
 * official BF990 manual (see {@link #healthy}); blue lies below it, yellow/red above it
 * (only yellow for values where high is not harmful).
 */
final class MeasurementScale extends View {
    private static final int BLUE=Color.rgb(49,158,220), GREEN=Color.rgb(76,192,142), YELLOW=Color.rgb(244,199,69), RED=Color.rgb(224,111,103);
    private final Double value; private final double min,max; private final double[] range; private final boolean dark, endYellow;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

    /** endYellow: the scale ends in yellow instead of red (values where high is not harmful). */
    MeasurementScale(Context context,Double value,double min,double max,double[] range,boolean endYellow,boolean dark){super(context);this.value=value;this.min=min;this.max=max;this.range=range;this.endYellow=endYellow;this.dark=dark;}

    /** Healthy ranges, BF990 manual (rev. 2026-10-15, section "Measuring"). sex: "female", "male", or anything else = unknown (union of both). */
    static double[] healthy(String metric,String sex){
        boolean f="female".equals(sex), m="male".equals(sex);
        switch(metric){
            case "bmi": return new double[]{18.5,25};
            case "fat": return f?new double[]{16,28}:m?new double[]{11,23}:new double[]{11,28};
            case "water": return f?new double[]{45,60}:m?new double[]{50,65}:new double[]{45,65};
            case "muscle": return f?new double[]{27,43}:m?new double[]{37,57}:new double[]{27,57};
            default: return null;
        }
    }

    /** Healthy weight for the healthy BMI range at the given height. */
    static double[] healthyWeight(double heightCm){if(heightCm<=0)return null;double h=heightCm/100;return new double[]{18.5*h*h,25*h*h};}

    private float frac(double v){return (float)Math.max(0,Math.min(1,(v-min)/(max-min)));}

    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density, left=6*d,right=getWidth()-6*d,y=9*d;
        int[] colors; float[] stops;
        if(range==null){colors=endYellow?new int[]{BLUE,GREEN,YELLOW}:new int[]{BLUE,GREEN,YELLOW,RED};stops=null;}
        else if(endYellow){float lo=frac(range[0]),hi=Math.max(lo,frac(range[1])),fade=0.04f;
            colors=new int[]{BLUE,BLUE,GREEN,GREEN,YELLOW};
            stops=new float[]{0,Math.max(0,lo-fade),lo,hi,1};
            for(int i=1;i<stops.length;i++)stops[i]=Math.max(stops[i],stops[i-1]);}
        else{float lo=frac(range[0]),hi=Math.max(lo,frac(range[1])),fade=0.04f;
            colors=new int[]{BLUE,BLUE,GREEN,GREEN,YELLOW,RED};
            stops=new float[]{0,Math.max(0,lo-fade),lo,hi,Math.min(1,hi+fade+(1-hi)*0.35f),1};
            for(int i=1;i<stops.length;i++)stops[i]=Math.max(stops[i],stops[i-1]);}
        paint.setShader(new LinearGradient(left,y,right,y,colors,stops,Shader.TileMode.CLAMP));paint.setStrokeWidth(5*d);paint.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(left,y,right,y,paint);paint.setShader(null);
        if(value!=null){float x=left+(right-left)*frac(value);paint.setColor(dark?Color.WHITE:Color.rgb(33,41,53));canvas.drawCircle(x,y,5*d,paint);paint.setColor(dark?Color.rgb(39,46,61):Color.WHITE);canvas.drawCircle(x,y,2*d,paint);}
        paint.setColor(dark?Color.LTGRAY:Color.DKGRAY);paint.setTextSize(11*d);
        String lo=fmt(min),hi=fmt(max);float loW=paint.measureText(lo),hiW=paint.measureText(hi);
        canvas.drawText(lo,left,30*d,paint);canvas.drawText(hi,right-hiW,30*d,paint);
        if(range!=null){float gap=6*d,prevEnd=left+loW+gap,limit=right-hiW-gap;
            for(double r:range){String s=fmt(r);float w=paint.measureText(s),x=left+(right-left)*frac(r)-w/2;
                if(x>=prevEnd&&x+w<=limit){canvas.drawText(s,x,30*d,paint);prevEnd=x+w+gap;}}}
    }

    private static String fmt(double v){return v==Math.rint(v)?String.format(Locale.getDefault(),"%.0f",v):String.format(Locale.getDefault(),"%.1f",v);}
}
