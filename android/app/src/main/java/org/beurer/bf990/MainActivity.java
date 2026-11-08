package org.beurer.bf990;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MainActivity extends Activity implements ScaleClient.Listener {
    private LinearLayout content; private int page=0; private final String[] pages={"Current values","Diagrams","Settings"};
    private SharedPreferences prefs; private ScaleClient scale; private String message="Set up your scale user in Settings to begin.", error=""; private Integer battery; private Measurements.Reading latest;
    private boolean simulation, showSamples; private int historyRange; private HistoryStore history;
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("scale",MODE_PRIVATE);history=new HistoryStore(this);simulation=Build.FINGERPRINT.contains("generic")||Build.FINGERPRINT.contains("emulator")||Build.MODEL.contains("sdk_gphone");restoreLatest();render();if(prefs.getInt("index",0)>0&&!simulation)connect();}
    @Override protected void onDestroy(){if(scale!=null)scale.close();if(history!=null)history.close();super.onDestroy();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String v,int size){TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(Color.rgb(33,41,48));t.setPadding(0,dp(8),0,dp(8));return t;}
    private Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(247,249,250));root.setOnApplyWindowInsetsListener((view,insets)->{int left,top,right,bottom;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;}else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}view.setPadding(left,top,right,bottom);return insets;});TextView title=text("BF990",24);title.setPadding(dp(20),dp(16),dp(20),dp(8));root.addView(title);LinearLayout tabs=new LinearLayout(this);for(int i=0;i<pages.length;i++){final int target=i;Button b=button(pages[i],()->{page=target;render();});b.setEnabled(i!=page);tabs.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}root.addView(tabs);ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(12),dp(20),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);if(page==0)current();else if(page==1)diagrams();else settings();}
    private void current(){content.addView(text("Current values",22));String name=prefs.getString("name","");content.addView(text(name.isEmpty()?"No user selected":name,18));content.addView(text(message,15));if(!error.isEmpty()){TextView e=text(error,15);e.setTextColor(Color.rgb(155,38,32));content.addView(e);}if(battery!=null)content.addView(text("Scale battery: "+battery+"%",15));if(latest==null)content.addView(text("No current measurement yet.",16));else{content.addView(text("Measured "+latest.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),15));row("Weight",String.format(Locale.getDefault(),"%.2f kg",latest.weight));row("BMI",String.format(Locale.getDefault(),"%.1f",latest.bmi));row("Body fat",pct(latest.fat));row("Body water",pct(latest.water));row("Muscle",pct(latest.muscle));}content.addView(button("Connect / repeat measurement",this::connect));if(simulation){content.addView(text("Emulator test mode uses synthetic packets.",14));content.addView(button("Simulate measurement",this::simulate));}}
    private String pct(Double d){return d==null?"—":String.format(Locale.getDefault(),"%.1f %%",d);}
    private void row(String name,String value){content.addView(text(name+"    "+value,18));}
    private void diagrams(){
        content.addView(text("Diagrams",22));
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels={"28 days","6 months","All time"};
        for(int i=0;i<labels.length;i++){final int selected=i;Button b=button(labels[i],()->{historyRange=selected;render();});b.setEnabled(i!=historyRange);controls.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}
        content.addView(controls);
        if(showSamples)content.addView(text("SAMPLE DATA",14));
        String[] titles={"Weight (kg)","BMI","Body fat (%)","Body water (%)","Muscle (%)"};
        try{
            int user=prefs.getInt("index",0);if(simulation&&(showSamples||user==0))user=255;
            for(int i=0;i<titles.length;i++){
                content.addView(text(titles[i],19));
                java.util.List<HistoryStore.Point> points=history.points(user,i,historyRange);
                content.addView(new HistoryChart(this,points),new LinearLayout.LayoutParams(-1,dp(180)));
            }
            content.addView(text("Older values are averaged.",14));
        }catch(RuntimeException e){content.addView(text("Could not load history: "+e.getMessage(),15));}
        if(simulation)content.addView(button("Show sample history",()->{try{addSampleHistory();showSamples=true;render();}catch(RuntimeException e){Toast.makeText(this,"Sample history failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}}));
        if(simulation&&showSamples)content.addView(button("Show my readings",()->{showSamples=false;render();}));
    }
    private void addSampleHistory(){
        history.clearSampleHistory();
        for(int i=0;i<240;i+=3){LocalDateTime d=LocalDateTime.now().minusDays(i);int weight=14000+i*4,bmi=235+i/12;byte[] w={14,(byte)weight,(byte)(weight>>8),(byte)d.getYear(),(byte)(d.getYear()>>8),(byte)d.getMonthValue(),(byte)d.getDayOfMonth(),(byte)d.getHour(),(byte)d.getMinute(),(byte)d.getSecond(),(byte)255,(byte)bmi,(byte)(bmi>>8),0,0};Measurements.Reading r=Measurements.weight(w);byte[] b={(byte)0x98,3,(byte)190,0,0,0,(byte)150,1,0,0,0x20,0x20,0,0};Measurements.body(r,b);history.save(r);}
    }
    private EditText input(String hint,String value,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);e.setText(value);content.addView(e);return e;}
    private void settings(){content.addView(text("Settings",22));content.addView(text("Existing scale user",19));content.addView(text("Enter the user index and the consent code saved when this user was registered. User discovery and creating a user on Android are not available yet.",15));EditText name=input("Name on this phone",prefs.getString("name",""),1);EditText index=input("Scale user index",prefs.getInt("index",0)==0?"":String.valueOf(prefs.getInt("index",0)),2);EditText code=input("Consent code",prefs.contains("code")?String.valueOf(prefs.getInt("code",0)):"",2|128);content.addView(button("Save user",()->{try{String n=name.getText().toString().trim();int i=Integer.parseInt(index.getText().toString());int c=Integer.parseInt(code.getText().toString());if(n.isEmpty()||i<1||i>254||c<0||c>9999)throw new IllegalArgumentException();if(i!=prefs.getInt("index",0)){latest=null;}prefs.edit().putString("name",n).putInt("index",i).putInt("code",c).apply();restoreLatest();error="";message="User saved. Connect to the scale.";page=0;render();}catch(Exception ex){Toast.makeText(this,"Enter a name, an index from 1 to 254, and a consent code from 0 to 9999.",Toast.LENGTH_LONG).show();}}));content.addView(text("Backup and restore are not available yet.",15));}
    private boolean permissions(){if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},42);return false;}}else if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},42);return false;}return true;}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==42){if(g.length>0){for(int x:g)if(x!=PackageManager.PERMISSION_GRANTED){error="Bluetooth permission was denied. Allow nearby devices in Android settings.";render();return;}connect();}}}
    private void connect(){if(prefs.getInt("index",0)==0||!prefs.contains("code")){error="Set up an existing user and consent code in Settings first.";render();return;}if(!permissions())return;if(scale!=null)scale.close();error="";battery=null;scale=new ScaleClient(this,this,prefs.getInt("index",0),prefs.getInt("code",0));scale.start();render();}
    private void simulate(){error="";message="Simulated measurement received.";battery=87;LocalDateTime d=LocalDateTime.now();byte[] w={14,0,0,(byte)d.getYear(),(byte)(d.getYear()>>8),(byte)d.getMonthValue(),(byte)d.getDayOfMonth(),(byte)d.getHour(),(byte)d.getMinute(),(byte)d.getSecond(),(byte)255,0,0,0,0};int weight=14000,bmi=235;w[1]=(byte)weight;w[2]=(byte)(weight>>8);w[11]=(byte)bmi;w[12]=(byte)(bmi>>8);byte[] b={(byte)0x98,3,(byte)190,0,0,0,(byte)150,1,0,0,(byte)0x20,0x20,0,0};try{latest=Measurements.weight(w);Measurements.body(latest,b);history.save(latest);render();}catch(IllegalArgumentException ex){error=ex.getMessage();render();}}
    @Override public void status(String v){message=v;render();}
    @Override public void error(String v){error=v;render();}
    @Override public void battery(int v){battery=v;render();}
    private void restoreLatest(){
        latest=null; String raw=prefs.getString("reading_"+prefs.getInt("index",0),null); if(raw==null)return;
        try{org.json.JSONObject j=new org.json.JSONObject(raw);Measurements.Reading r=Measurements.weight(android.util.Base64.decode(j.getString("weight"),android.util.Base64.NO_WRAP));if(j.has("body"))Measurements.body(r,android.util.Base64.decode(j.getString("body"),android.util.Base64.NO_WRAP));latest=r;message="Last saved measurement.";}catch(Exception ex){error="The saved measurement could not be read: "+ex.getMessage();}
    }
    @Override public void reading(Measurements.Reading v){if(latest!=null&&v.time.isBefore(latest.time))return;try{history.save(v);}catch(RuntimeException e){error="Could not store reading: "+e.getMessage();render();return;}latest=v;message="Measurement received.";error="";try{org.json.JSONObject j=new org.json.JSONObject();j.put("weight",android.util.Base64.encodeToString(v.weightRaw,android.util.Base64.NO_WRAP));if(v.bodyRaw!=null)j.put("body",android.util.Base64.encodeToString(v.bodyRaw,android.util.Base64.NO_WRAP));prefs.edit().putString("reading_"+v.user,j.toString()).apply();}catch(org.json.JSONException e){error="Could not save measurement: "+e.getMessage();}render();}
}
