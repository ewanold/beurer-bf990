package org.beurer.bf990;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.graphics.Typeface;
import android.widget.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MainActivity extends Activity implements ScaleClient.Listener {
    private LinearLayout content; private int page=0; private String[] pages;
    private static final int SHARED_USER_CODE=6907;
    private SharedPreferences prefs; private ScaleClient scale; private UserScanner userScanner; private boolean pendingUserScan, userScanError; private String userScanMessage=""; private final java.util.Map<Integer,Integer> discoveredHeights=new java.util.HashMap<>(); private final java.util.Map<Integer,java.time.LocalDate> discoveredUsers=new java.util.LinkedHashMap<>(); private String message="", error=""; private boolean dark; private Integer battery; private Measurements.Reading latest;
    private float downX, downY; private boolean simulation, showSamples; private int historyRange; private HistoryStore history; private final java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final int REQUEST_SAVE_BACKUP=43, REQUEST_OPEN_BACKUP=44;
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("scale",MODE_PRIVATE);if(state!=null){page=state.getInt("page",0);historyRange=state.getInt("range",0);}applyAppearance(); pages=new String[]{getString(R.string.nav_current),getString(R.string.nav_timeline),getString(R.string.settings)};message=getString(R.string.setup_message);history=new HistoryStore(this);simulation=Build.FINGERPRINT.contains("generic")||Build.FINGERPRINT.contains("emulator")||Build.MODEL.contains("sdk_gphone");restoreLatest();render();if(prefs.getInt("index",0)>0&&!simulation)connect();if(state==null)importIntent(getIntent());}
    @Override protected void onNewIntent(android.content.Intent intent){super.onNewIntent(intent);importIntent(intent);}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("page",page);out.putInt("range",historyRange);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){if(scale!=null)scale.close();if(userScanner!=null)userScanner.close();io.shutdown();if(history!=null)history.close();super.onDestroy();}
    private void applyAppearance(){String lang=prefs.getString("language","system"); Configuration config=new Configuration(getResources().getConfiguration());config.setLocale(lang.equals("system")?android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0):Locale.forLanguageTag(lang));getResources().updateConfiguration(config,getResources().getDisplayMetrics()); String theme=prefs.getString("theme","system");dark=theme.equals("dark")||(theme.equals("system")&&(getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);getWindow().setStatusBarColor(dark?Color.rgb(20,24,34):Color.rgb(244,246,250));getWindow().setNavigationBarColor(dark?Color.rgb(20,24,34):Color.rgb(244,246,250));getWindow().getDecorView().setSystemUiVisibility(dark?0:android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){float dx=event.getX()-downX,dy=event.getY()-downY;if(Math.abs(dx)>dp(80)&&Math.abs(dx)>Math.abs(dy)*1.5f){int next=page+(dx<0?1:-1);if(next>=0&&next<pages.length){MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();page=next;render();return true;}}}
        return super.dispatchTouchEvent(event);
    }
    private int fg(){return dark?Color.rgb(237,240,248):Color.rgb(35,42,55);} private int surface(){return dark?Color.rgb(39,46,61):Color.WHITE;} private int background(){return dark?Color.rgb(20,24,34):Color.rgb(244,246,250);}
    private int labelColor(){return dark?Color.rgb(190,200,215):Color.rgb(101,111,124);}
    private GradientDrawable shape(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String v,int size){TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(fg());t.setPadding(0,dp(8),0,dp(8));return t;}
    private Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(fg());b.setBackground(shape(dark?Color.rgb(57,67,85):Color.rgb(220,228,239),0));b.setOnClickListener(v->action.run());return b;}
    private void addSettingsButton(LinearLayout parent,String label,Runnable action){Button b=button(label,action);b.setBackground(shape(dark?Color.rgb(57,67,85):Color.rgb(220,228,239),dark?Color.rgb(93,105,125):Color.rgb(160,175,191)));b.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(5),0,dp(5));parent.addView(b,lp);}
    private void render(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(background());root.setOnApplyWindowInsetsListener((view,insets)->{int left,top,right,bottom;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;}else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}view.setPadding(left,top,right,bottom);return insets;});TextView title=text(getString(R.string.app_title),24);title.setPadding(dp(20),dp(16),dp(20),dp(8));root.addView(title);LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(12),0,dp(12),0);for(int i=0;i<pages.length;i++){final int target=i;Button b=button(pages[i],()->{page=target;render();});b.setBackground(shape(dark?Color.rgb(57,67,85):Color.rgb(220,228,239),dark?Color.rgb(93,105,125):Color.rgb(160,175,191)));b.setAlpha(i==page?1f:0.8f);LinearLayout.LayoutParams tabParams=new LinearLayout.LayoutParams(0,dp(52),1);tabParams.setMargins(dp(3),0,dp(3),0);tabs.addView(b,tabParams);}root.addView(tabs);ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(12),dp(20),dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);if(page==0)current();else if(page==1)diagrams();else settings();}
    private void current(){String name=prefs.getString("name","");TextView heading=text(name.isEmpty()?getString(R.string.current_values):getString(R.string.current_values_for,name),22);heading.setSingleLine(true);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);content.addView(heading);if(name.isEmpty())content.addView(text(getString(R.string.no_user),18));content.addView(text(message,15));if(!error.isEmpty()){TextView e=text(error,15);e.setTextColor(Color.rgb(155,38,32));content.addView(e);}if(battery!=null)content.addView(text(getString(R.string.battery)+battery+"%",15));if(latest==null)content.addView(text(getString(R.string.no_measurement),16));else{content.addView(text(getString(R.string.measured)+latest.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),15));card(getString(R.string.weight),String.format(Locale.getDefault(),"%.2f kg",latest.weight),latest.weight,30,200);card(getString(R.string.bmi),String.format(Locale.getDefault(),"%.1f",latest.bmi),latest.bmi,10,45);card(getString(R.string.body_fat),pct(latest.fat),latest.fat,0,60);card(getString(R.string.body_water),pct(latest.water),latest.water,0,80);card(getString(R.string.muscle),pct(latest.muscle),latest.muscle,0,80);}content.addView(button(getString(R.string.connect),this::connect));if(simulation){content.addView(text(getString(R.string.emulator_info),14));content.addView(button(getString(R.string.simulate),this::simulate));}}
    private String pct(Double d){return d==null?"—":String.format(Locale.getDefault(),"%.1f %%",d);}
    private void card(String name,String value,Double number,double min,double max){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(8),dp(16),dp(12));box.setBackground(shape(surface(),dark?Color.rgb(70,80,99):Color.rgb(218,225,233)));TextView label=text(name,19);label.setTextColor(labelColor());label.setTypeface(null,Typeface.BOLD);label.setGravity(Gravity.CENTER);box.addView(label);TextView amount=text(value,25);amount.setGravity(Gravity.CENTER);box.addView(amount);box.addView(new MeasurementScale(this,number,min,max,dark),new LinearLayout.LayoutParams(-1,dp(38)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));content.addView(box,lp);}
    private void diagrams(){
        content.addView(text(getString(R.string.diagrams),22));
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels={getString(R.string.days),getString(R.string.months),getString(R.string.all_time)};
        for(int i=0;i<labels.length;i++){final int selected=i;Button b=button(labels[i],()->{historyRange=selected;render();});b.setBackground(shape(dark?Color.rgb(57,67,85):Color.rgb(220,228,239),dark?Color.rgb(93,105,125):Color.rgb(160,175,191)));b.setAlpha(i==historyRange?1f:0.8f);LinearLayout.LayoutParams rangeParams=new LinearLayout.LayoutParams(0,dp(52),1);rangeParams.setMargins(dp(3),0,dp(3),0);controls.addView(b,rangeParams);}
        content.addView(controls);
        if(showSamples)content.addView(text(getString(R.string.sample_data),14));
        String[] titles={getString(R.string.weight_kg),getString(R.string.bmi),getString(R.string.fat_pct),getString(R.string.water_pct),getString(R.string.muscle_pct)};
        try{
            int user=prefs.getInt("index",0);if(simulation&&(showSamples||user==0))user=255;
            for(int i=0;i<titles.length;i++){
                java.util.List<HistoryStore.Point> points=history.points(user,i,historyRange);
                LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(6),dp(8),dp(6));box.setBackground(shape(surface(),dark?Color.rgb(90,102,122):Color.rgb(190,201,213)));
                TextView label=text(titles[i],19);label.setGravity(Gravity.CENTER);label.setTextColor(labelColor());label.setTypeface(null,Typeface.BOLD);box.addView(label);
                box.addView(new HistoryChart(this,points,dark,i),new LinearLayout.LayoutParams(-1,dp(180)));
                LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);cardParams.setMargins(0,dp(10),0,0);content.addView(box,cardParams);
            }
            content.addView(text(getString(R.string.averaged),14));
        }catch(RuntimeException e){content.addView(text(getString(R.string.history_failed)+e.getMessage(),15));}
        if(simulation)content.addView(button(getString(R.string.show_samples),()->{try{addSampleHistory();showSamples=true;render();}catch(RuntimeException e){Toast.makeText(this,getString(R.string.sample_failed)+e.getMessage(),Toast.LENGTH_LONG).show();}}));
        if(simulation&&showSamples)content.addView(button(getString(R.string.show_readings),()->{showSamples=false;render();}));
    }
    private void addSampleHistory(){
        history.clearSampleHistory();
        for(int i=0;i<240;i+=3){LocalDateTime d=LocalDateTime.now().minusDays(i);int weight=14000+i*4,bmi=235+i/12;byte[] w={14,(byte)weight,(byte)(weight>>8),(byte)d.getYear(),(byte)(d.getYear()>>8),(byte)d.getMonthValue(),(byte)d.getDayOfMonth(),(byte)d.getHour(),(byte)d.getMinute(),(byte)d.getSecond(),(byte)255,(byte)bmi,(byte)(bmi>>8),0,0};Measurements.Reading r=Measurements.weight(w);byte[] b={(byte)0x98,3,(byte)190,0,0,0,(byte)150,1,0,0,0x20,0x20,0,0};Measurements.body(r,b);history.save(r);}
    }
    private LinearLayout settingsCard(String label){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(5),dp(16),dp(9));box.setBackground(shape(surface(),dark?Color.rgb(90,102,122):Color.rgb(190,201,213)));TextView title=text(label,16);title.setTypeface(null,Typeface.BOLD);title.setTextColor(labelColor());box.addView(title);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));content.addView(box,lp);return box;}
    private EditText input(String hint,String value,int type){LinearLayout box=settingsCard(hint);EditText e=new EditText(this);e.setTextColor(fg());e.setHintTextColor(dark?Color.LTGRAY:Color.DKGRAY);e.setHint(hint);e.setInputType(type);e.setText(value);box.addView(e);return e;}
    private void selector(String label,String[] entries,String[] values,String key){LinearLayout box=settingsCard(label);Spinner spinner=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,entries);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);String saved=prefs.getString(key,"system");int selected=0;for(int i=0;i<values.length;i++)if(values[i].equals(saved))selected=i;spinner.setSelection(selected);final int initial=selected;spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){boolean ready=false;public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int position,long id){if(v instanceof TextView)((TextView)v).setTextColor(fg());if(!ready){ready=true;if(position==initial)return;}if(!values[position].equals(prefs.getString(key,"system"))){prefs.edit().putString(key,values[position]).apply();recreate();}}});box.addView(spinner);}
    private void settings(){content.addView(text(getString(R.string.settings),22));content.addView(text(getString(R.string.existing_user),19));addSettingsButton(content,getString(R.string.user_scan_button),this::scanUsers);if(!userScanMessage.isEmpty()){TextView scanStatus=text(userScanMessage,15);if(userScanError)scanStatus.setTextColor(Color.rgb(155,38,32));content.addView(scanStatus);}LinearLayout userResults=new LinearLayout(this);userResults.setOrientation(LinearLayout.VERTICAL);content.addView(userResults);EditText name=input(getString(R.string.name_hint),prefs.getString("name",""),1);EditText index=input(getString(R.string.index_hint),prefs.getInt("index",0)==0?"":String.valueOf(prefs.getInt("index",0)),2);EditText birthday=input(getString(R.string.birthday),prefs.getString("birthdate_"+prefs.getInt("index",0),""),1);EditText height=input(getString(R.string.height_cm),prefs.getInt("height_"+prefs.getInt("index",0),0)==0?"":String.valueOf(prefs.getInt("height_"+prefs.getInt("index",0),0)),2);for(java.util.Map.Entry<Integer,java.time.LocalDate> user:discoveredUsers.entrySet()){final int selected=user.getKey();final java.time.LocalDate date=user.getValue();addSettingsButton(userResults,getString(R.string.user_scan_entry,selected,date.toString()),()->{index.setText(String.valueOf(selected));birthday.setText(date.toString());height.setText(String.valueOf(discoveredHeights.get(selected)));});}index.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence value,int start,int count,int after){}public void onTextChanged(CharSequence value,int start,int before,int count){try{int chosen=Integer.parseInt(value.toString());birthday.setText(prefs.getString("birthdate_"+chosen,""));int h=prefs.getInt("height_"+chosen,0);height.setText(h==0?"":String.valueOf(h));}catch(NumberFormatException e){birthday.setText("");height.setText("");}}public void afterTextChanged(android.text.Editable value){}});content.addView(button(getString(R.string.save_user),()->{try{String n=name.getText().toString().trim();int i=Integer.parseInt(index.getText().toString());String birth=birthday.getText().toString().trim();String heightText=height.getText().toString().trim();int h=heightText.isEmpty()?0:Integer.parseInt(heightText);if(h!=0&&(h<50||h>250))throw new IllegalArgumentException();if(h==0&&prefs.getBoolean("height_dirty_"+i,false))throw new IllegalArgumentException();if(!birth.isEmpty()&&(java.time.LocalDate.parse(birth).isAfter(java.time.LocalDate.now())||java.time.LocalDate.parse(birth).isBefore(java.time.LocalDate.of(1900,1,1))))throw new IllegalArgumentException();if(n.isEmpty()||i<1||i>254)throw new IllegalArgumentException();if(i!=prefs.getInt("index",0)){latest=null;}String baseline=discoveredUsers.containsKey(i)?discoveredUsers.get(i).toString():prefs.getString("verified_birth_"+i,"");boolean dirtyBirth=prefs.getBoolean("birth_dirty_"+i,false);if(dirtyBirth&&!birth.equals(prefs.getString("birthdate_"+i,"")))throw new IllegalArgumentException();boolean changeBirth=dirtyBirth||(!birth.isEmpty()&&!baseline.isEmpty()&&!birth.equals(baseline));if(!prefs.edit().putString("name",n).putInt("index",i).putString("birthdate_"+i,birth).putString("birth_baseline_"+i,baseline).putBoolean("birth_pending_"+i,changeBirth).putInt("height_"+i,h).putBoolean("height_pending_"+i,h!=0).commit())throw new IllegalStateException();if(userScanner!=null){userScanner.close();userScanner=null;}discoveredUsers.clear();discoveredHeights.clear();userScanMessage="";userScanError=false;restoreLatest();error="";message=getString(R.string.save_wake_scale);page=0;Toast.makeText(this,getString(R.string.save_wake_scale),Toast.LENGTH_LONG).show();render();connect();}catch(Exception ex){Toast.makeText(this,getString(R.string.user_invalid),Toast.LENGTH_LONG).show();}}));selector(getString(R.string.language),new String[]{getString(R.string.system_default),"English","Deutsch"},new String[]{"system","en","de"},"language");selector(getString(R.string.appearance),new String[]{getString(R.string.system_default),getString(R.string.light),getString(R.string.dark)},new String[]{"system","light","dark"},"theme");backupSection();TextView credit=text(getString(R.string.icon_credit),14);credit.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);credit.setTextIsSelectable(true);content.addView(credit);}
    private boolean permissions(){if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},42);return false;}}else if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},42);return false;}return true;}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==42){if(g.length>0){for(int x:g)if(x!=PackageManager.PERMISSION_GRANTED){if(pendingUserScan){pendingUserScan=false;userScanError=true;userScanMessage=getString(R.string.permission_denied);}else error=getString(R.string.permission_denied);render();return;}if(pendingUserScan){pendingUserScan=false;scanUsers();}else connect();}}}
    private void scanUsers(){pendingUserScan=true;if(!permissions())return;pendingUserScan=false;if(scale!=null){scale.close();scale=null;}if(userScanner!=null)userScanner.close();discoveredUsers.clear();discoveredHeights.clear();userScanError=false;userScanMessage=getString(R.string.user_scan_running);userScanner=new UserScanner(this,new UserScanner.Listener(){public void found(int index,java.time.LocalDate birthday,int height){discoveredUsers.put(index,birthday);discoveredHeights.put(index,height);}public void finished(int inaccessible){userScanMessage=discoveredUsers.isEmpty()?getString(R.string.user_scan_none):"";userScanner=null;render();}public void failed(String message){userScanError=true;userScanMessage=discoveredUsers.isEmpty()?getString(R.string.user_scan_failed,message):getString(R.string.user_scan_partial);userScanner=null;render();}},SHARED_USER_CODE);userScanner.start();render();}
    private void connect(){if(prefs.getInt("index",0)==0){error=getString(R.string.setup_first);render();return;}if(!permissions())return;if(userScanner!=null){userScanner.close();userScanner=null;}if(scale!=null)scale.close();error="";battery=null;scale=new ScaleClient(this,this,prefs.getInt("index",0),SHARED_USER_CODE,prefs.getBoolean("birth_pending_"+prefs.getInt("index",0),false)?prefs.getString("birth_baseline_"+prefs.getInt("index",0),""):prefs.getString("birthdate_"+prefs.getInt("index",0),""),prefs.getBoolean("birth_pending_"+prefs.getInt("index",0),false)?prefs.getString("birthdate_"+prefs.getInt("index",0),""):"",prefs.getBoolean("birth_dirty_"+prefs.getInt("index",0),false),prefs.getBoolean("height_pending_"+prefs.getInt("index",0),false)?prefs.getInt("height_"+prefs.getInt("index",0),0):0,prefs.getBoolean("height_dirty_"+prefs.getInt("index",0),false));scale.start();render();}
    private void simulate(){error="";message=getString(R.string.simulated);battery=87;LocalDateTime d=LocalDateTime.now();byte[] w={14,0,0,(byte)d.getYear(),(byte)(d.getYear()>>8),(byte)d.getMonthValue(),(byte)d.getDayOfMonth(),(byte)d.getHour(),(byte)d.getMinute(),(byte)d.getSecond(),(byte)255,0,0,0,0};int weight=14000,bmi=235;w[1]=(byte)weight;w[2]=(byte)(weight>>8);w[11]=(byte)bmi;w[12]=(byte)(bmi>>8);byte[] b={(byte)0x98,3,(byte)190,0,0,0,(byte)150,1,0,0,(byte)0x20,0x20,0,0};try{latest=Measurements.weight(w);Measurements.body(latest,b);history.save(latest);render();}catch(IllegalArgumentException ex){error=ex.getMessage();render();}}
    @Override public void status(String v){message=v;render();}
    @Override public void error(String v){error=v;render();}
    @Override public void battery(int v){battery=v;render();}
    @Override public boolean birthdayWriteStarted(int user){return prefs.edit().putBoolean("birth_dirty_"+user,true).commit();}
    @Override public void birthdayConfirmed(int user,String birthday){prefs.edit().putString("verified_birth_"+user,birthday).putString("birthdate_"+user,birthday).putBoolean("birth_pending_"+user,false).putBoolean("birth_dirty_"+user,false).apply();}
    @Override public boolean heightWriteStarted(int user){return prefs.edit().putBoolean("height_dirty_"+user,true).commit();}
    @Override public void heightConfirmed(int user,int height){prefs.edit().putInt("height_"+user,height).putBoolean("height_pending_"+user,false).putBoolean("height_dirty_"+user,false).apply();}
    private void restoreLatest(){
        latest=null; String raw=prefs.getString("reading_"+prefs.getInt("index",0),null); if(raw==null)return;
        try{org.json.JSONObject j=new org.json.JSONObject(raw);Measurements.Reading r=Measurements.weight(android.util.Base64.decode(j.getString("weight"),android.util.Base64.NO_WRAP));if(j.has("body"))Measurements.body(r,android.util.Base64.decode(j.getString("body"),android.util.Base64.NO_WRAP));latest=r;message=getString(R.string.saved_measurement);}catch(Exception ex){error=getString(R.string.saved_read_failed)+ex.getMessage();}
    }
    @Override public void reading(Measurements.Reading v){if(latest!=null&&v.time.isBefore(latest.time))return;try{history.save(v);}catch(RuntimeException e){error=getString(R.string.store_failed)+e.getMessage();render();return;}latest=v;message=getString(R.string.measurement_received);error="";try{storeLatest(v);}catch(org.json.JSONException e){error=getString(R.string.save_failed)+e.getMessage();}render();}
    private void storeLatest(Measurements.Reading v) throws org.json.JSONException {org.json.JSONObject j=new org.json.JSONObject();j.put("weight",android.util.Base64.encodeToString(v.weightRaw,android.util.Base64.NO_WRAP));if(v.bodyRaw!=null)j.put("body",android.util.Base64.encodeToString(v.bodyRaw,android.util.Base64.NO_WRAP));prefs.edit().putString("reading_"+v.user,j.toString()).apply();}
    // ---- Backup and restore ----
    private void backupSection(){
        LinearLayout box=settingsCard(getString(R.string.backup));
        TextView help=text(getString(R.string.backup_help),14);help.setTextColor(labelColor());box.addView(help);
        addSettingsButton(box,getString(R.string.backup_share),this::shareBackup);
        addSettingsButton(box,getString(R.string.backup_save),()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);i.addCategory(android.content.Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(android.content.Intent.EXTRA_TITLE,Backup.fileName(java.time.LocalDate.now()));startPicker(i,REQUEST_SAVE_BACKUP);});
        addSettingsButton(box,getString(R.string.backup_restore),()->{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);i.addCategory(android.content.Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(android.content.Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/octet-stream"});startPicker(i,REQUEST_OPEN_BACKUP);});
    }
    private void startPicker(android.content.Intent i,int request){try{startActivityForResult(i,request);}catch(android.content.ActivityNotFoundException e){toast(getString(R.string.backup_no_picker));}}
    private void toast(String v){Toast.makeText(this,v,Toast.LENGTH_LONG).show();}
    private String appVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(PackageManager.NameNotFoundException e){return "?";}}
    /** Writes the archive or returns false when there is nothing to back up. */
    private boolean writeBackup(java.io.OutputStream out) throws java.io.IOException {java.util.List<HistoryStore.Raw> rows=history.rawArchive();if(rows.isEmpty())return false;Backup.write(out,rows,appVersion());return true;}
    private void shareBackup(){
        io.execute(()->{
            try{
                java.io.File dir=BackupProvider.dir(this);dir.mkdirs();java.io.File[] old=dir.listFiles();if(old!=null)for(java.io.File f:old)f.delete();
                java.io.File file=new java.io.File(dir,Backup.fileName(java.time.LocalDate.now()));boolean written;
                try(java.io.OutputStream out=new java.io.FileOutputStream(file)){written=writeBackup(out);}
                if(!written){file.delete();runOnUiThread(()->toast(getString(R.string.backup_empty)));return;}
                android.net.Uri uri=BackupProvider.uri(file.getName());
                runOnUiThread(()->{android.content.Intent send=new android.content.Intent(android.content.Intent.ACTION_SEND);send.setType("application/zip");send.putExtra(android.content.Intent.EXTRA_STREAM,uri);send.putExtra(android.content.Intent.EXTRA_SUBJECT,file.getName());send.setClipData(android.content.ClipData.newRawUri(file.getName(),uri));send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(android.content.Intent.createChooser(send,getString(R.string.backup_share)));});
            }catch(Exception e){runOnUiThread(()->toast(getString(R.string.backup_failed,String.valueOf(e.getMessage()))));}
        });
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        android.net.Uri uri=data.getData();
        if(request==REQUEST_SAVE_BACKUP)io.execute(()->{
            boolean written=false;
            try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("cannot open target");written=writeBackup(out);}
            catch(Exception e){runOnUiThread(()->toast(getString(R.string.backup_failed,String.valueOf(e.getMessage()))));return;}
            if(!written){try{android.provider.DocumentsContract.deleteDocument(getContentResolver(),uri);}catch(Exception ignored){}runOnUiThread(()->toast(getString(R.string.backup_empty)));return;}
            runOnUiThread(()->toast(getString(R.string.backup_saved)));
        });
        else if(request==REQUEST_OPEN_BACKUP)importFrom(uri);
    }
    @SuppressWarnings("deprecation")
    private void importIntent(android.content.Intent intent){
        if(intent==null)return;android.net.Uri uri=null;
        if(android.content.Intent.ACTION_VIEW.equals(intent.getAction()))uri=intent.getData();
        else if(android.content.Intent.ACTION_SEND.equals(intent.getAction()))uri=(android.net.Uri)intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM);
        if(uri!=null){page=2;render();importFrom(uri);}
    }
    private void importFrom(android.net.Uri uri){
        io.execute(()->{
            try{
                java.util.List<Measurements.Reading> readings;
                try(java.io.InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new java.io.IOException("cannot open file");readings=Backup.read(in);}
                HistoryStore.ImportPlan plan=history.plan(readings);
                runOnUiThread(()->confirmImport(plan));
            }catch(Exception e){runOnUiThread(()->toast(getString(R.string.restore_failed,String.valueOf(e.getMessage()))));}
        });
    }
    private void confirmImport(HistoryStore.ImportPlan plan){
        if(plan.apply.isEmpty()){toast(getString(R.string.restore_nothing,plan.total()));return;}
        StringBuilder users=new StringBuilder();for(int u:plan.users){if(users.length()>0)users.append(", ");users.append(u);}
        new android.app.AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle(R.string.backup_restore)
            .setMessage(getString(R.string.restore_confirm,plan.total(),users.toString(),plan.added,plan.newer,plan.skipped,plan.rejected))
            .setNegativeButton(android.R.string.cancel,null)
            .setPositiveButton(R.string.restore_import,(d,w)->io.execute(()->{
                try{
                    history.apply(plan);
                    java.util.Map<Integer,Measurements.Reading> newest=new java.util.HashMap<>();
                    for(Measurements.Reading r:plan.apply.values()){Measurements.Reading n=newest.get(r.user);if(n==null||r.time.isAfter(n.time))newest.put(r.user,r);}
                    runOnUiThread(()->{
                        for(Measurements.Reading r:newest.values()){Measurements.Reading stored=storedReading(r.user);if(stored==null||r.time.isAfter(stored.time)){try{storeLatest(r);}catch(org.json.JSONException ignored){}}}
                        restoreLatest();toast(getString(R.string.restore_done,plan.added+plan.newer));render();
                    });
                }catch(Exception e){runOnUiThread(()->toast(getString(R.string.restore_failed,String.valueOf(e.getMessage()))));}
            })).show();
    }
    private Measurements.Reading storedReading(int user){
        String raw=prefs.getString("reading_"+user,null);if(raw==null)return null;
        try{return Measurements.weight(android.util.Base64.decode(new org.json.JSONObject(raw).getString("weight"),android.util.Base64.NO_WRAP));}catch(Exception e){return null;}
    }
}
