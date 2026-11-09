package org.beurer.bf990;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import java.util.Locale;

/** Illustrative scale, not a clinical reference range. */
final class MeasurementScale extends View {
    private final Double value; private final double min,max; private final boolean dark;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    MeasurementScale(Context context,Double value,double min,double max,boolean dark){super(context);this.value=value;this.min=min;this.max=max;this.dark=dark;}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float d=getResources().getDisplayMetrics().density, left=6*d,right=getWidth()-6*d,y=9*d;paint.setShader(new LinearGradient(left,y,right,y,new int[]{Color.rgb(49,158,220),Color.rgb(76,192,142),Color.rgb(244,199,69),Color.rgb(224,111,103)},null,Shader.TileMode.CLAMP));paint.setStrokeWidth(5*d);paint.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(left,y,right,y,paint);paint.setShader(null);if(value!=null){float x=left+(right-left)*(float)Math.max(0,Math.min(1,(value-min)/(max-min)));paint.setColor(dark?Color.WHITE:Color.rgb(33,41,53));canvas.drawCircle(x,y,5*d,paint);paint.setColor(dark?Color.rgb(39,46,61):Color.WHITE);canvas.drawCircle(x,y,2*d,paint);}paint.setColor(dark?Color.LTGRAY:Color.DKGRAY);paint.setTextSize(11*d);String lo=String.format(Locale.getDefault(),"%.0f",min),hi=String.format(Locale.getDefault(),"%.0f",max);canvas.drawText(lo,left,30*d,paint);canvas.drawText(hi,right-paint.measureText(hi),30*d,paint);}
}
