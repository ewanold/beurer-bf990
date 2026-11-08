package org.beurer.bf990;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.List;
import java.util.Locale;

final class HistoryChart extends View {
    private final List<HistoryStore.Point> points; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    HistoryChart(Context c,List<HistoryStore.Point> points){super(c);this.points=points;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float density=getResources().getDisplayMetrics().density,left=55*density,right=getWidth()-18*density,top=25*density,bottom=getHeight()-45*density;p.setTextSize(12*density);p.setColor(Color.DKGRAY);
        if(points.isEmpty()){c.drawText("No readings in this range",left,top+30*density,p);return;}
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(HistoryStore.Point v:points){min=Math.min(min,v.value);max=Math.max(max,v.value);}double pad=Math.max((max-min)*0.15,1);min-=pad;max+=pad;
        p.setColor(Color.LTGRAY);for(int i=0;i<=4;i++){float y=top+(bottom-top)*i/4;c.drawLine(left,y,right,y,p);p.setColor(Color.DKGRAY);c.drawText(String.format(Locale.getDefault(),"%.1f",max-(max-min)*i/4),2*density,y,p);p.setColor(Color.LTGRAY);}
        long first=java.time.LocalDate.parse(points.get(0).date.length()==7?points.get(0).date+"-01":points.get(0).date).toEpochDay(), last=java.time.LocalDate.parse(points.get(points.size()-1).date.length()==7?points.get(points.size()-1).date+"-01":points.get(points.size()-1).date).toEpochDay();float px=0,py=0;p.setColor(Color.rgb(32,111,155));p.setStrokeWidth(2*density);
        for(int i=0;i<points.size();i++){HistoryStore.Point v=points.get(i);long day=java.time.LocalDate.parse(v.date.length()==7?v.date+"-01":v.date).toEpochDay();float x=first==last?(left+right)/2:left+(right-left)*(day-first)/(float)(last-first);float y=(float)(bottom-(v.value-min)/(max-min)*(bottom-top));if(i>0)c.drawLine(px,py,x,y,p);c.drawCircle(x,y,3*density,p);px=x;py=y;}
        p.setColor(Color.DKGRAY);c.drawText(points.get(0).date,left,bottom+22*density,p);String end=points.get(points.size()-1).date;c.drawText(end,right-p.measureText(end),bottom+22*density,p);
    }
}
