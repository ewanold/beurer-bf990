package org.beurer.bf990;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Backup archive: manifest.json (metadata), raw.json (raw scale packets, the only part used on import)
 * and measurements.csv (decoded values for reading on a PC). Contains no credentials or settings.
 */
final class Backup {
    static final String FORMAT="bf990-backup";
    static final int VERSION=1;
    private static final int MAX_ENTRY=8*1024*1024;

    static String fileName(LocalDate day){return "bf990-backup-"+day+".zip";}

    static void write(OutputStream out,List<HistoryStore.Raw> rows,String appVersion) throws IOException {
        TreeSet<Integer> users=new TreeSet<>();
        JSONArray readings=new JSONArray();
        StringBuilder csv=new StringBuilder("user,time,weight_kg,bmi,fat_pct,water_pct,muscle_pct\n");
        try{
            for(HistoryStore.Raw row:rows){
                Measurements.Reading r=decode(row.weight,row.body);
                users.add(row.user);
                JSONObject j=new JSONObject();
                j.put("user",row.user);j.put("day",row.day);j.put("weight",hex(row.weight));j.put("body",row.body==null?JSONObject.NULL:hex(row.body));
                readings.put(j);
                csv.append(row.user).append(',').append(r.time).append(',').append(String.format(Locale.ROOT,"%.3f,%.1f",r.weight,r.bmi))
                   .append(',').append(num(r.fat)).append(',').append(num(r.water)).append(',').append(num(r.muscle)).append('\n');
            }
            JSONObject manifest=new JSONObject();
            manifest.put("format",FORMAT);manifest.put("version",VERSION);manifest.put("created",OffsetDateTime.now().toString());
            manifest.put("app_version",appVersion);manifest.put("readings",rows.size());manifest.put("users",new JSONArray(users));
            JSONObject raw=new JSONObject();raw.put("readings",readings);
            try(ZipOutputStream zip=new ZipOutputStream(out)){
                put(zip,"manifest.json",manifest.toString(2));
                put(zip,"raw.json",raw.toString(1));
                put(zip,"measurements.csv",csv.toString());
            }
        }catch(JSONException e){throw new IOException(e);}
    }

    /** Reads and decodes all readings; throws IllegalArgumentException for an unusable archive. */
    static List<Measurements.Reading> read(InputStream in) throws IOException {
        String manifest=null,raw=null;
        try(ZipInputStream zip=new ZipInputStream(in)){
            for(ZipEntry e;(e=zip.getNextEntry())!=null;){
                if(e.getName().equals("manifest.json"))manifest=readEntry(zip);
                else if(e.getName().equals("raw.json"))raw=readEntry(zip);
            }
        }
        if(manifest==null||raw==null)throw new IllegalArgumentException("not a BF990 backup");
        try{
            JSONObject m=new JSONObject(manifest);
            if(!FORMAT.equals(m.optString("format")))throw new IllegalArgumentException("not a BF990 backup");
            if(m.optInt("version",-1)!=VERSION)throw new IllegalArgumentException("unsupported backup version "+m.opt("version"));
            JSONArray rows=new JSONObject(raw).getJSONArray("readings");
            List<Measurements.Reading> result=new ArrayList<>();
            for(int i=0;i<rows.length();i++){
                JSONObject j=rows.getJSONObject(i);
                Measurements.Reading r=decode(unhex(j.getString("weight")),j.isNull("body")?null:unhex(j.getString("body")));
                if(r.user!=j.getInt("user")||!r.time.toLocalDate().toString().equals(j.getString("day")))
                    throw new IllegalArgumentException("inconsistent entry "+(i+1));
                result.add(r);
            }
            return result;
        }catch(JSONException e){throw new IllegalArgumentException("damaged backup: "+e.getMessage(),e);}
    }

    static Measurements.Reading decode(byte[] weight,byte[] body){
        Measurements.Reading r=Measurements.weight(weight);
        if(body!=null)Measurements.body(r,body);
        return r;
    }
    private static String num(Double d){return d==null?"":String.format(Locale.ROOT,"%.1f",d);}
    private static void put(ZipOutputStream zip,String name,String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));zip.write(text.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
    private static String readEntry(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];
        for(int n;(n=zip.read(buf))>0;){if(out.size()+n>MAX_ENTRY)throw new IllegalArgumentException("backup entry too large");out.write(buf,0,n);}
        return out.toString("UTF-8");
    }
    static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
    static byte[] unhex(String s){
        if(s.length()%2!=0||!s.matches("[0-9a-fA-F]*"))throw new IllegalArgumentException("invalid hex data");
        byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(2*i,2*i+2),16);return b;
    }
}
