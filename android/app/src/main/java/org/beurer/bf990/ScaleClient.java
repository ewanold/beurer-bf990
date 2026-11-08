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
    interface Listener { void status(String message); void error(String message); void battery(int percent); void reading(Measurements.Reading r); }
    private static UUID id(String s) { return UUID.fromString("0000"+s+"-0000-1000-8000-00805f9b34fb"); }
    private final Context context; private final Listener listener; private final Handler handler=new Handler(Looper.getMainLooper());
    private final int index, code; private BluetoothGatt gatt; private BluetoothLeScanner scanner; private boolean scanning, closed;
    private final ArrayDeque<Runnable> queue=new ArrayDeque<>(); private Runnable active; private String stage="";
    private LocalDateTime clock; private long clockAt; private Measurements.Reading pending; private boolean consented, consentWriteComplete, consentSetupStarted, receivedMeasurement;
    private final UUID cccd=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    ScaleClient(Context c,Listener l,int index,int code){ context=c;listener=l;this.index=index;this.code=code; }
    void start(){
        BluetoothManager m=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE); BluetoothAdapter a=m==null?null:m.getAdapter();
        if(a==null||!a.isEnabled()){fail("Bluetooth is off or unavailable. Enable Bluetooth and try again.");return;}
        scanner=a.getBluetoothLeScanner(); if(scanner==null){fail("Bluetooth LE scanning is unavailable.");return;}
        listener.status("Looking for BF990. Wake the scale."); scanning=true;
        scanner.startScan(scan); handler.postDelayed(()->{if(scanning){stopScan();fail("No BF990 was found within 20 seconds. Wake the scale and try again.");}},20000);
    }
    private final ScanCallback scan=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){ BluetoothDevice d=result.getDevice(); String n=d.getName(); if(n==null&&result.getScanRecord()!=null)n=result.getScanRecord().getDeviceName(); if(!"BF990".equalsIgnoreCase(n))return; stopScan(); listener.status("Connecting to BF990…"); gatt=d.connectGatt(context,false,callbacks,BluetoothDevice.TRANSPORT_LE); }
        @Override public void onScanFailed(int code){scanning=false;fail("Bluetooth scan failed (Android code "+code+"). Check Bluetooth permissions and try again.");}
    };
    void stopScan(){if(scanning&&scanner!=null)scanner.stopScan(scan);scanning=false;}
    void close(){closed=true;stopScan();handler.removeCallbacksAndMessages(null);if(gatt!=null){gatt.disconnect();gatt.close();gatt=null;}}
    private void fail(String msg){if(!closed){listener.error(msg);close();}}
    private void add(String name,Runnable task){queue.add(()->{stage=name; task.run();});}
    private void next(){active=queue.poll();if(active!=null){active.run(); Runnable operation=active; timeoutTask=operation; handler.postDelayed(()->{if(!closed&&active==operation&&timeoutTask==operation)fail("Timed out during "+stage+". Wake the scale and try again.");},15000);}}
    private Runnable timeoutTask;
    private void done(){active=null;timeoutTask=null;next();}
    private BluetoothGattCharacteristic chr(String service,String characteristic){BluetoothGattService s=gatt.getService(id(service));return s==null?null:s.getCharacteristic(id(characteristic));}
    private void read(String service,String c){BluetoothGattCharacteristic x=chr(service,c);if(x==null||!gatt.readCharacteristic(x))fail("Unable to read "+service+"/"+c+"; the scale did not expose or start the requested operation.");}
    private void subscribe(String service,String c,boolean indicate){BluetoothGattCharacteristic x=chr(service,c); if(x==null){fail("Missing required characteristic "+service+"/"+c+".");return;} BluetoothGattDescriptor d=x.getDescriptor(cccd);if(d==null||!gatt.setCharacteristicNotification(x,true)){fail("Cannot subscribe to "+service+"/"+c+".");return;}d.setValue(indicate?BluetoothGattDescriptor.ENABLE_INDICATION_VALUE:BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);if(!gatt.writeDescriptor(d))fail("Cannot enable indications for "+c+".");}
    private void write(String service,String c,byte[] value,boolean response){BluetoothGattCharacteristic x=chr(service,c);if(x==null){fail("Missing characteristic "+service+"/"+c+".");return;}x.setWriteType(response?BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT:BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);x.setValue(value);if(!gatt.writeCharacteristic(x))fail("Could not send command to "+service+"/"+c+".");}
    private void setup(){
        add("model identification",()->read("180a","2a24")); add("battery read",()->read("180f","2a19")); add("scale clock read",()->read("1805","2a2b"));
        add("user control indications",()->subscribe("181c","2a9f",true)); add("weight indications",()->subscribe("181d","2a9d",true)); add("body composition indications",()->subscribe("181b","2a9c",true));
        add("user authorization",()->write("181c","2a9f",new byte[]{2,(byte)index,(byte)code,(byte)(code>>8)},true)); next();
    }
    private void maybeAfterConsent(){ if(consented && consentWriteComplete && !consentSetupStarted){consentSetupStarted=true; afterConsent();} }
    private void afterConsent(){
        add("selected-user verification",()->read("181c","2a9a"));
        add("measurement status subscription",()->subscribe("ffff","fff4",false));
        for(String c:new String[]{"fff5","fff7","fff8"}) { BluetoothGattCharacteristic x=chr("ffff",c); if(x!=null && (x.getProperties() & (BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_INDICATE))!=0) { boolean indication=(x.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0; add("private data subscription "+c,()->subscribe("ffff",c,indication)); } }
        add("measurement request",()->write("ffff","fff4",new byte[]{0},false)); next();
        handler.postDelayed(()->{if(!closed){listener.status(receivedMeasurement ? "Measurement session ended. Select repeat for another reading." : "Waiting for a measurement timed out. Wake the scale and try again.");close();}},180000);
    }
    private void onData(UUID id,byte[] raw){
        try {
            if(id.equals(ScaleClient.id("2a9f"))){if(raw.length<3||raw[0]!=0x20||raw[1]!=2)return;if(raw[2]!=1){fail("User authorization failed (scale result "+(raw[2]&255)+"). Check the selected index and consent code; do not register another user automatically.");return;}consented=true;listener.status("User authorized. Preparing measurement…");maybeAfterConsent();}
            else if(id.equals(ScaleClient.id("2a9d"))){pending=null; Measurements.Reading r=Measurements.weight(raw);if(r.user!=index)return; if(clock==null){listener.error("Measurement ignored: the scale clock could not be read.");return;} LocalDateTime expected=clock.plusNanos((android.os.SystemClock.elapsedRealtime()-clockAt)*1000000L);long age=Duration.between(r.time,expected).getSeconds(); if(age< -5||age>300)return;pending=r;receivedMeasurement=true;listener.reading(r);}
            else if(id.equals(ScaleClient.id("2a9c"))&&pending!=null){Measurements.body(pending,raw);listener.reading(pending);pending=null;}
            else if(id.equals(ScaleClient.id("fff4"))&&raw.length>0&&(raw[0]&255)!=1){listener.error("Scale measurement status "+(raw[0]&255)+". Wake the scale and repeat the reading.");}
        }catch(IllegalArgumentException e){listener.error("Cannot interpret scale data: "+e.getMessage());}
    }
    private final BluetoothGattCallback callbacks=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){handler.post(()->{if(closed)return;if(status!=0){fail("Bluetooth connection failed (GATT status "+status+"). Wake the scale and retry.");return;}if(state==BluetoothProfile.STATE_CONNECTED){listener.status("Connected; discovering services…");if(!g.discoverServices())fail("Android could not start service discovery.");}else if(state==BluetoothProfile.STATE_DISCONNECTED)fail("Scale disconnected before the session completed. Try again.");});}
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){handler.post(()->{if(status!=0){fail("Service discovery failed (GATT status "+status+").");return;}setup();});}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){handler.post(()->{if(status!=0)fail("Subscription failed during "+stage+" (GATT status "+status+").");else done();});}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){handler.post(()->{if(status!=0){fail("Write failed during "+stage+" (GATT status "+status+").");return;} if(c.getUuid().equals(id("2a9f"))){active=null;timeoutTask=null;consentWriteComplete=true;maybeAfterConsent();handler.postDelayed(()->{if(!consented&&!closed)fail("The scale did not respond to user authorization within 35 seconds.");},35000);}else{if(c.getUuid().equals(id("fff4")))listener.status("Ready. Step on the scale for a new measurement.");done();}});}
        @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int status){byte[] v=c.getValue()==null?new byte[0]:c.getValue().clone();handler.post(()->{if(status!=0){fail("Read failed during "+stage+" (GATT status "+status+").");return;}try{UUID u=c.getUuid();if(u.equals(id("2a24"))&&!new String(v,java.nio.charset.StandardCharsets.UTF_8).replace("\u0000","").equals("BF990")){fail("Connected device did not identify itself as BF990.");return;}if(u.equals(id("2a19"))){if(v.length!=1||(v[0]&255)>100)throw new IllegalArgumentException("Invalid battery value");listener.battery(v[0]&255);}if(u.equals(id("2a2b"))){if(v.length!=10)throw new IllegalArgumentException("Unexpected clock length");clock=LocalDateTime.of(Measurements.u16(v,0),v[2]&255,v[3]&255,v[4]&255,v[5]&255,v[6]&255);clockAt=android.os.SystemClock.elapsedRealtime();}if(u.equals(id("2a9a"))&&(v.length!=1||(v[0]&255)!=index)){fail("Scale selected a different user than requested.");return;}done();}catch(RuntimeException e){fail("Invalid response during "+stage+": "+e.getMessage());}});}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] v=c.getValue()==null?new byte[0]:c.getValue().clone();handler.post(()->onData(c.getUuid(),v));}
    };
}
