package org.beurer.bf990;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/** A single foreground BLE session; all operations are serialized by callbacks. */
@SuppressLint("MissingPermission")
final class ScaleClient {
    interface Listener { void status(String message); void error(String message); void battery(int percent); boolean birthdayWriteStarted(int user); void birthdayConfirmed(int user,String birthday); boolean heightWriteStarted(int user); void heightConfirmed(int user,int height); void reading(Measurements.Reading r); }
    private static UUID id(String s) { return UUID.fromString("0000"+s+"-0000-1000-8000-00805f9b34fb"); }
    private final Context context; private final Listener listener; private final Handler handler=new Handler(Looper.getMainLooper());
    private final int index, code, desiredHeight; private boolean heightVerified, heightDirty; private long expectedRevision=-1; private final java.time.LocalDate expectedBirthday, desiredBirthday; private boolean birthdayDirty; private String revisionFor=""; private boolean birthdayVerified; private BluetoothGatt gatt; private BluetoothLeScanner scanner; private boolean scanning, closed;
    private final ArrayDeque<Runnable> queue=new ArrayDeque<>(); private Runnable active; private String stage="";
    private LocalDateTime clock; private long clockAt; private Measurements.Reading pending; private boolean consented, consentWriteComplete, consentSetupStarted, receivedMeasurement;
    private final UUID cccd=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    ScaleClient(Context c,Listener l,int index,int code,String birthday,String newBirthday,boolean birthDirty,int height,boolean dirty){ context=c;listener=l;this.index=index;this.code=code;this.desiredHeight=height;this.heightDirty=dirty;this.heightVerified=height==0;this.expectedBirthday=birthday.isEmpty()?null:java.time.LocalDate.parse(birthday);this.desiredBirthday=newBirthday.isEmpty()?null:java.time.LocalDate.parse(newBirthday);this.birthdayDirty=birthDirty;this.birthdayVerified=expectedBirthday==null&&desiredBirthday==null; }
    void start(){
        BluetoothManager m=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE); BluetoothAdapter a=m==null?null:m.getAdapter();
        if(a==null||!a.isEnabled()){fail(context.getString(R.string.ble_00));return;}
        scanner=a.getBluetoothLeScanner(); if(scanner==null){fail(context.getString(R.string.ble_01));return;}
        listener.status(context.getString(R.string.ble_02)); scanning=true;
        scanner.startScan(scan); handler.postDelayed(()->{if(scanning){stopScan();fail(context.getString(R.string.ble_03));}},20000);
    }
    private final ScanCallback scan=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){ BluetoothDevice d=result.getDevice(); String n=d.getName(); if(n==null&&result.getScanRecord()!=null)n=result.getScanRecord().getDeviceName(); if(!"BF990".equalsIgnoreCase(n))return; stopScan(); listener.status(context.getString(R.string.ble_04)); gatt=d.connectGatt(context,false,callbacks,BluetoothDevice.TRANSPORT_LE); }
        @Override public void onScanFailed(int code){scanning=false;fail(context.getString(R.string.ble_05)+code+context.getString(R.string.ble_06));}
    };
    void stopScan(){if(scanning&&scanner!=null)scanner.stopScan(scan);scanning=false;}
    void close(){closed=true;stopScan();handler.removeCallbacksAndMessages(null);if(gatt!=null){gatt.disconnect();gatt.close();gatt=null;}}
    private void fail(String msg){if(!closed){listener.error(msg);close();}}
    private void add(String name,Runnable task){queue.add(()->{stage=name; task.run();});}
    private void addFirst(String name,Runnable task){queue.addFirst(()->{stage=name;task.run();});}
    private void next(){active=queue.poll();if(active!=null){active.run(); Runnable operation=active; timeoutTask=operation; handler.postDelayed(()->{if(!closed&&active==operation&&timeoutTask==operation)fail(context.getString(R.string.ble_07)+stage+context.getString(R.string.ble_08));},15000);}}
    private Runnable timeoutTask;
    private void done(){active=null;timeoutTask=null;next();}
    private BluetoothGattCharacteristic chr(String service,String characteristic){BluetoothGattService s=gatt.getService(id(service));return s==null?null:s.getCharacteristic(id(characteristic));}
    private void read(String service,String c){BluetoothGattCharacteristic x=chr(service,c);if(x==null||!gatt.readCharacteristic(x))fail(context.getString(R.string.ble_09)+service+"/"+c+context.getString(R.string.ble_10));}
    private void subscribe(String service,String c,boolean indicate){BluetoothGattCharacteristic x=chr(service,c); if(x==null){fail(context.getString(R.string.ble_11)+service+"/"+c+".");return;} BluetoothGattDescriptor d=x.getDescriptor(cccd);if(d==null||!gatt.setCharacteristicNotification(x,true)){fail(context.getString(R.string.ble_12)+service+"/"+c+".");return;}d.setValue(indicate?BluetoothGattDescriptor.ENABLE_INDICATION_VALUE:BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);if(!gatt.writeDescriptor(d))fail(context.getString(R.string.ble_13)+c+".");}
    private void write(String service,String c,byte[] value,boolean response){BluetoothGattCharacteristic x=chr(service,c);if(x==null){fail(context.getString(R.string.ble_14)+service+"/"+c+".");return;}x.setWriteType(response?BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);x.setValue(value);if(!gatt.writeCharacteristic(x))fail(context.getString(R.string.ble_15)+service+"/"+c+".");}
    private void setup(){
        add(context.getString(R.string.ble_16),()->read("180a","2a24")); add(context.getString(R.string.ble_17),()->read("180f","2a19")); add(context.getString(R.string.ble_18),()->read("1805","2a2b"));
        add(context.getString(R.string.ble_19),()->subscribe("181c","2a9f",true)); add(context.getString(R.string.ble_20),()->subscribe("181d","2a9d",true)); add(context.getString(R.string.ble_21),()->subscribe("181b","2a9c",true));
        add(context.getString(R.string.ble_22),()->write("181c","2a9f",new byte[]{2,(byte)index,(byte)code,(byte)(code>>8)},true)); next();
    }
    private void maybeAfterConsent(){ if(consented && consentWriteComplete && !consentSetupStarted){consentSetupStarted=true; afterConsent();} }
    private void afterConsent(){
        add(context.getString(R.string.ble_23),()->read("181c","2a9a"));
        if(expectedBirthday!=null||desiredBirthday!=null)add(context.getString(R.string.birthday_check),()->read("181c","2a85"));
        if(desiredHeight!=0)add("height read",()->read("181c","2a8e"));
        add(context.getString(R.string.ble_24),()->subscribe("ffff","fff4",false));
        for(String c:new String[]{"fff5","fff7","fff8"}) { BluetoothGattCharacteristic x=chr("ffff",c); if(x!=null && (x.getProperties() & (BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_INDICATE))!=0) { boolean indication=(x.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0; add(context.getString(R.string.ble_25)+c,()->subscribe("ffff",c,indication)); } }
        add(context.getString(R.string.ble_26),()->write("ffff","fff4",new byte[]{0},false)); next();
        handler.postDelayed(()->{if(!closed){listener.status(receivedMeasurement ? context.getString(R.string.ble_27) : context.getString(R.string.ble_28));close();}},180000);
    }
    private void onData(UUID id,byte[] raw){
        if(closed)return;
        try {
            if(id.equals(ScaleClient.id("2a9f"))){if(raw.length<3||raw[0]!=0x20||raw[1]!=2)return;if(raw[2]!=1){fail(context.getString(R.string.ble_29)+(raw[2]&255)+context.getString(R.string.ble_30));return;}consented=true;listener.status(context.getString(R.string.ble_31));maybeAfterConsent();}
            else if(id.equals(ScaleClient.id("2a9d"))){if(!birthdayVerified||!heightVerified)return;pending=null; Measurements.Reading r=Measurements.weight(raw);if(r.user!=index)return; if(clock==null){listener.error(context.getString(R.string.ble_32));return;} LocalDateTime expected=clock.plusNanos((android.os.SystemClock.elapsedRealtime()-clockAt)*1000000L);long age=Duration.between(r.time,expected).getSeconds(); if(age< -5||age>300)return;pending=r;receivedMeasurement=true;listener.reading(r);}
            else if(id.equals(ScaleClient.id("2a9c"))&&pending!=null){Measurements.body(pending,raw);listener.reading(pending);pending=null;}
            else if(id.equals(ScaleClient.id("fff4"))&&raw.length>0&&(raw[0]&255)!=1){listener.error(context.getString(R.string.ble_33)+(raw[0]&255)+context.getString(R.string.ble_34));}
        }catch(IllegalArgumentException e){listener.error(context.getString(R.string.ble_35)+e.getMessage());}
    }
    private final BluetoothGattCallback callbacks=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){handler.post(()->{if(closed)return;if(status!=0){fail(context.getString(R.string.ble_36)+status+context.getString(R.string.ble_37));return;}if(state==BluetoothProfile.STATE_CONNECTED){listener.status(context.getString(R.string.ble_38));if(!g.discoverServices())fail(context.getString(R.string.ble_39));}else if(state==BluetoothProfile.STATE_DISCONNECTED)fail(context.getString(R.string.ble_40));});}
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){handler.post(()->{if(status!=0){fail(context.getString(R.string.ble_41)+status+").");return;}setup();});}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){handler.post(()->{if(status!=0)fail(context.getString(R.string.ble_42)+stage+context.getString(R.string.ble_43)+status+").");else done();});}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){handler.post(()->{if(status!=0){fail(context.getString(R.string.ble_44)+stage+context.getString(R.string.ble_43)+status+").");return;} if(c.getUuid().equals(id("2a9f"))){active=null;timeoutTask=null;consentWriteComplete=true;maybeAfterConsent();handler.postDelayed(()->{if(!consented&&!closed)fail(context.getString(R.string.ble_45));},35000);}else{if(c.getUuid().equals(id("fff4")))listener.status(context.getString(R.string.ble_46));done();}});}
        @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int status){byte[] v=c.getValue()==null?new byte[0]:c.getValue().clone();handler.post(()->{if(status!=0){fail(context.getString(R.string.ble_47)+stage+context.getString(R.string.ble_43)+status+").");return;}try{UUID u=c.getUuid();if(u.equals(id("2a24"))&&!new String(v,java.nio.charset.StandardCharsets.UTF_8).replace("\u0000","").equals("BF990")){fail(context.getString(R.string.ble_48));return;}if(u.equals(id("2a19"))){if(v.length!=1||(v[0]&255)>100)throw new IllegalArgumentException(context.getString(R.string.ble_49));listener.battery(v[0]&255);}if(u.equals(id("2a2b"))){if(v.length!=10)throw new IllegalArgumentException(context.getString(R.string.ble_50));clock=LocalDateTime.of(Measurements.u16(v,0),v[2]&255,v[3]&255,v[4]&255,v[5]&255,v[6]&255);clockAt=android.os.SystemClock.elapsedRealtime();}if(u.equals(id("2a85"))){if(v.length!=4)throw new IllegalArgumentException(context.getString(R.string.birthday_invalid_response));java.time.LocalDate actual=java.time.LocalDate.of(Measurements.u16(v,0),v[2]&255,v[3]&255);ProfileDateDecision.Action action=ProfileDateDecision.decide(actual,expectedBirthday,desiredBirthday,birthdayDirty,stage.equals(context.getString(R.string.birthday_check)));if(action==ProfileDateDecision.Action.VERIFIED){birthdayVerified=true;listener.birthdayConfirmed(index,actual.toString());}else if(action==ProfileDateDecision.Action.RECOVER_REVISION){revisionFor="birthday";addFirst("revision read",()->read("181c","2a99"));}else if(action==ProfileDateDecision.Action.WRITE){if(!listener.birthdayWriteStarted(index)){fail(context.getString(R.string.birthday_save_failed));return;}birthdayDirty=true;int year=desiredBirthday.getYear();byte[] target={(byte)year,(byte)(year>>8),(byte)desiredBirthday.getMonthValue(),(byte)desiredBirthday.getDayOfMonth()};addFirst("birthday verify",()->read("181c","2a85"));addFirst("birthday write",()->write("181c","2a85",target,false));}else{fail(context.getString(R.string.birthday_mismatch));return;}}if(u.equals(id("2a8e"))){if(v.length!=2)throw new IllegalArgumentException(context.getString(R.string.height_invalid_response));int actual=Measurements.u16(v,0);if(actual==desiredHeight){if(heightDirty){revisionFor="height";addFirst("revision read",()->read("181c","2a99"));}else{heightVerified=true;listener.heightConfirmed(index,desiredHeight);}}else if(stage.equals("height read")){if(!listener.heightWriteStarted(index)){fail(context.getString(R.string.height_save_failed));return;}heightDirty=true;addFirst("height verify",()->read("181c","2a8e"));addFirst("height write",()->write("181c","2a8e",new byte[]{(byte)desiredHeight,(byte)(desiredHeight>>8)},false));}else{fail(context.getString(R.string.height_mismatch));return;}}if(u.equals(id("2a99"))){if(v.length!=4)throw new IllegalArgumentException(context.getString(R.string.revision_invalid));long revision=((long)v[0]&255)|(((long)v[1]&255)<<8)|(((long)v[2]&255)<<16)|(((long)v[3]&255)<<24);if(revision==0xffffffffL)throw new IllegalArgumentException(context.getString(R.string.revision_invalid));if(stage.equals("revision read")){expectedRevision=revision>=0xfffffffeL?0:revision+1;final long next=expectedRevision;addFirst("revision verify",()->read("181c","2a99"));addFirst("revision write",()->write("181c","2a99",new byte[]{(byte)next,(byte)(next>>8),(byte)(next>>16),(byte)(next>>24)},false));}else if(revision==expectedRevision){if(revisionFor.equals("birthday")){birthdayVerified=true;birthdayDirty=false;listener.birthdayConfirmed(index,desiredBirthday.toString());}else if(revisionFor.equals("height")){heightVerified=true;heightDirty=false;listener.heightConfirmed(index,desiredHeight);}else{fail(context.getString(R.string.revision_invalid));return;}}else{fail(context.getString(R.string.revision_mismatch));return;}}if(u.equals(id("2a9a"))&&(v.length!=1||(v[0]&255)!=index)){fail(context.getString(R.string.ble_51));return;}done();}catch(RuntimeException e){fail(context.getString(R.string.ble_52)+stage+": "+e.getMessage());}});}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] v=c.getValue()==null?new byte[0]:c.getValue().clone();handler.post(()->onData(c.getUuid(),v));}
    };
}
