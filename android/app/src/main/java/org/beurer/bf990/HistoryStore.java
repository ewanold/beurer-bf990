package org.beurer.bf990;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Per-user daily values, compressed into weighted weekly and monthly averages. */
final class HistoryStore extends SQLiteOpenHelper {
    static final String[] METRICS={"weight","bmi","fat","water","muscle"};
    static final class Point { final String date; final double value; Point(String d,double v){date=d;value=v;} }
    HistoryStore(Context context){super(context,"measurements.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE daily(user INTEGER NOT NULL, day TEXT NOT NULL, weight REAL NOT NULL, bmi REAL NOT NULL, fat REAL, water REAL, muscle REAL, weight_raw BLOB NOT NULL, body_raw BLOB, PRIMARY KEY(user,day))");
        db.execSQL("CREATE TABLE raw_archive(user INTEGER NOT NULL, day TEXT NOT NULL, weight_raw BLOB NOT NULL, body_raw BLOB, PRIMARY KEY(user,day))");
        String columns="user INTEGER NOT NULL, bucket TEXT NOT NULL, month TEXT NOT NULL, last_day TEXT NOT NULL, samples INTEGER NOT NULL, weight_sum REAL NOT NULL, weight_n INTEGER NOT NULL, bmi_sum REAL NOT NULL, bmi_n INTEGER NOT NULL, fat_sum REAL NOT NULL, fat_n INTEGER NOT NULL, water_sum REAL NOT NULL, water_n INTEGER NOT NULL, muscle_sum REAL NOT NULL, muscle_n INTEGER NOT NULL";
        db.execSQL("CREATE TABLE weekly("+columns+", PRIMARY KEY(user,bucket,month))");
        db.execSQL("CREATE TABLE monthly("+columns+", PRIMARY KEY(user,bucket,month))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("Unsupported database migration");}
    void save(Measurements.Reading r){
        SQLiteDatabase db=getWritableDatabase(); String day=r.time.toLocalDate().toString(); db.beginTransaction();
        try{
            if(r.time.toLocalDate().isBefore(LocalDate.now().minusDays(27))){try(Cursor prior=db.rawQuery("SELECT 1 FROM raw_archive WHERE user=? AND day=?",new String[]{String.valueOf(r.user),day})){if(prior.moveToFirst()){db.setTransactionSuccessful();return;}}}
            db.execSQL("INSERT OR REPLACE INTO daily VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{r.user,day,r.weight,r.bmi,r.fat,r.water,r.muscle,r.weightRaw,r.bodyRaw});
            db.execSQL("INSERT OR REPLACE INTO raw_archive VALUES(?,?,?,?)",new Object[]{r.user,day,r.weightRaw,r.bodyRaw});
            compact(db,LocalDate.now()); db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    void clearSampleHistory(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{for(String table:new String[]{"daily","weekly","monthly","raw_archive"})db.delete(table,"user=?",new String[]{"255"});db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static void merge(SQLiteDatabase db,String table,int user,String bucket,String month,String last,long samples,double[] sums,long[] counts){
        db.execSQL("INSERT OR IGNORE INTO "+table+" VALUES(?,?,?,?,0,0,0,0,0,0,0,0,0,0,0)",new Object[]{user,bucket,month,last});
        db.execSQL("UPDATE "+table+" SET last_day=MAX(last_day,?), samples=samples+?, weight_sum=weight_sum+?,weight_n=weight_n+?,bmi_sum=bmi_sum+?,bmi_n=bmi_n+?,fat_sum=fat_sum+?,fat_n=fat_n+?,water_sum=water_sum+?,water_n=water_n+?,muscle_sum=muscle_sum+?,muscle_n=muscle_n+? WHERE user=? AND bucket=? AND month=?",new Object[]{last,samples,sums[0],counts[0],sums[1],counts[1],sums[2],counts[2],sums[3],counts[3],sums[4],counts[4],user,bucket,month});
    }
    private static void compact(SQLiteDatabase db,LocalDate today){
        String dailyCut=today.minusDays(27).toString();
        try(Cursor c=db.rawQuery("SELECT user,day,weight,bmi,fat,water,muscle FROM daily WHERE day < ? ORDER BY day",new String[]{dailyCut})){
            while(c.moveToNext()){
                int user=c.getInt(0);String day=c.getString(1);LocalDate d=LocalDate.parse(day);String week=d.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString(),month=day.substring(0,7);double[] sums=new double[5];long[] counts=new long[5];for(int i=0;i<5;i++)if(!c.isNull(i+2)){sums[i]=c.getDouble(i+2);counts[i]=1;}merge(db,"weekly",user,week,month,day,1,sums,counts);
            }
        }
        db.delete("daily","day < ?",new String[]{dailyCut});
        String weeklyCut=today.minusMonths(6).toString();
        try(Cursor c=db.rawQuery("SELECT user,month,last_day,samples,weight_sum,weight_n,bmi_sum,bmi_n,fat_sum,fat_n,water_sum,water_n,muscle_sum,muscle_n FROM weekly WHERE last_day < ?",new String[]{weeklyCut})){
            while(c.moveToNext()){double[] sums=new double[5];long[] counts=new long[5];for(int i=0;i<5;i++){sums[i]=c.getDouble(4+2*i);counts[i]=c.getLong(5+2*i);}merge(db,"monthly",c.getInt(0),c.getString(1),c.getString(1),c.getString(2),c.getLong(3),sums,counts);}
        }
        db.delete("weekly","last_day < ?",new String[]{weeklyCut});
    }
    List<Point> points(int user,int metric,int range){
        if(metric<0||metric>=METRICS.length)throw new IllegalArgumentException("Unknown measurement");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{compact(db,LocalDate.now());db.setTransactionSuccessful();}finally{db.endTransaction();}
        String m=METRICS[metric];List<Point> points=new ArrayList<>();
        if(range==2)append(db,points,"SELECT bucket, "+m+"_sum/"+m+"_n FROM monthly WHERE user=? AND "+m+"_n>0",user);
        if(range>=1)append(db,points,"SELECT bucket, "+m+"_sum/"+m+"_n FROM weekly WHERE user=? AND "+m+"_n>0",user);
        append(db,points,"SELECT day, "+m+" FROM daily WHERE user=? AND "+m+" IS NOT NULL",user);
        points.sort((a,b)->a.date.compareTo(b.date));return points;
    }
    private static void append(SQLiteDatabase db,List<Point> into,String sql,int user){try(Cursor c=db.rawQuery(sql,new String[]{String.valueOf(user)})){while(c.moveToNext())into.add(new Point(c.getString(0),c.getDouble(1)));}}
}
