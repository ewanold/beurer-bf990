package org.beurer.bf990;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

final class HistoryChart extends View {
    private final boolean dark;
    private final List<HistoryStore.Point> points;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

    HistoryChart(Context c,List<HistoryStore.Point> points,boolean dark){super(c);this.points=points;this.dark=dark;}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float d=getResources().getDisplayMetrics().density;
        float left=58*d,right=getWidth()-18*d,top=16*d,bottom=getHeight()-35*d;
        p.setTextSize(12*d);
        p.setColor(dark?Color.LTGRAY:Color.DKGRAY);
        if(points.isEmpty()){c.drawText(getContext().getString(R.string.no_readings),left,top+30*d,p);return;}

        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(HistoryStore.Point v:points){min=Math.min(min,v.value);max=Math.max(max,v.value);}
        ChartRange axis=ChartRange.forValues(min,max);
        min=axis.min;max=axis.max;
        int divisions=6, decimals=axis.decimals;
        double step=axis.step;
        int grid=dark?Color.rgb(80,90,105):Color.LTGRAY;
        int ink=dark?Color.LTGRAY:Color.DKGRAY;
        for(int i=0;i<=divisions;i++){
            double tick=min+i*step;
            float y=bottom-(bottom-top)*i/divisions;
            p.setColor(grid);p.setStrokeWidth(d);c.drawLine(left,y,right,y,p);
            p.setColor(ink);String label=String.format(Locale.getDefault(),"%."+decimals+"f",tick);
            c.drawText(label,left-10*d-p.measureText(label),y+4*d,p);
        }
        long first=day(points.get(0).date),last=day(points.get(points.size()-1).date);
        float px=0,py=0;p.setColor(dark?Color.rgb(100,190,235):Color.rgb(32,111,155));p.setStrokeWidth(2*d);
        for(int i=0;i<points.size();i++){
            HistoryStore.Point v=points.get(i);long day=day(v.date);
            float x=first==last?(left+right)/2:left+(right-left)*(day-first)/(float)(last-first);
            float y=(float)(bottom-(v.value-min)/(max-min)*(bottom-top));
            if(i>0)c.drawLine(px,py,x,y,p);c.drawCircle(x,y,3*d,p);px=x;py=y;
        }
        p.setColor(ink);String start=points.get(0).date,end=points.get(points.size()-1).date;
        c.drawText(start,left,bottom+22*d,p);
        if(first!=last && right-p.measureText(end)>left+p.measureText(start)+8*d)c.drawText(end,right-p.measureText(end),bottom+22*d,p);
    }
    private static long day(String date){return LocalDate.parse(date.length()==7?date+"-01":date).toEpochDay();}
}
