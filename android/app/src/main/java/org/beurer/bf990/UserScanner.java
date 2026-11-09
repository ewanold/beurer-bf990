package org.beurer.bf990;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.time.LocalDate;
import java.util.UUID;

/** Reads the first eight indices accessible with the configured shared code; the device limit remains unverified. */
@SuppressLint("MissingPermission")
final class UserScanner {
    interface Listener { void found(int index, LocalDate birthday, int height); void finished(int inaccessible); void failed(String message); }
    private final Context context; private final Listener listener; private final int code;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private BluetoothLeScanner scanner; private BluetoothGatt gatt; private boolean scanning,closed,writeDone,responseDone; private int response,index=1,inaccessible; private String stage=""; private LocalDate birthday;
    UserScanner(Context context,Listener listener,int code){this.context=context;this.listener=listener;this.code=code;}
    private static UUID id(String shortId){return UUID.fromString("0000"+shortId+"-0000-1000-8000-00805f9b34fb");}
    private BluetoothGattCharacteristic characteristic(String service,String characteristic){BluetoothGattService s=gatt.getService(id(service));return s==null?null:s.getCharacteristic(id(characteristic));}
    void start(){BluetoothManager manager=(BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();if(adapter==null||!adapter.isEnabled()){fail(context.getString(R.string.ble_00));return;}scanner=adapter.getBluetoothLeScanner();if(scanner==null){fail(context.getString(R.string.ble_01));return;}scanning=true;scanner.startScan(scan);timeout("scan");}
    void close(){if(closed)return;closed=true;handler.removeCallbacksAndMessages(null);if(scanning&&scanner!=null)scanner.stopScan(scan);scanning=false;if(gatt!=null){gatt.disconnect();gatt.close();gatt=null;}}
    private void fail(String reason){if(!closed){close();listener.failed(reason);}}
    private void timeout(String operation){stage=operation;handler.removeCallbacksAndMessages(null);handler.postDelayed(()->fail(context.getString(R.string.user_scan_timeout,operation)),20000);}
    private final ScanCallback scan=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult result){BluetoothDevice device=result.getDevice();String name=device.getName();if(name==null&&result.getScanRecord()!=null)name=result.getScanRecord().getDeviceName();if(!"BF990".equalsIgnoreCase(name))return;if(scanning){scanner.stopScan(this);scanning=false;timeout("connect");gatt=device.connectGatt(context,false,callbacks,BluetoothDevice.TRANSPORT_LE);}}
        @Override public void onScanFailed(int result){fail(context.getString(R.string.ble_05)+result+context.getString(R.string.ble_06));}
    };
    private void read(String shortId){BluetoothGattCharacteristic c=characteristic(shortId.equals("2a24")?"180a":"181c",shortId);timeout("read "+shortId);if(c==null||!gatt.readCharacteristic(c))fail(context.getString(R.string.user_scan_read_failed,shortId));}
    private void subscribe(){BluetoothGattCharacteristic c=characteristic("181c","2a9f");if(c==null){fail(context.getString(R.string.ble_11)+"181c/2a9f");return;}BluetoothGattDescriptor d=c.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));if(d==null||!gatt.setCharacteristicNotification(c,true)){fail(context.getString(R.string.ble_12)+"181c/2a9f");return;}d.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);timeout("subscribe");if(!gatt.writeDescriptor(d))fail(context.getString(R.string.ble_13)+"2a9f");}
    private void nextUser(){if(index>8){int count=inaccessible;close();listener.finished(count);return;}BluetoothGattCharacteristic c=characteristic("181c","2a9f");if(c==null){fail(context.getString(R.string.ble_14)+"181c/2a9f");return;}writeDone=false;responseDone=false;c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);c.setValue(new byte[]{2,(byte)index,(byte)code,(byte)(code>>8)});timeout("user "+index);if(!gatt.writeCharacteristic(c))fail(context.getString(R.string.ble_15)+"181c/2a9f");}
    private void consentComplete(){if(!writeDone||!responseDone||closed)return;if(response!=1){if(response!=5){fail(context.getString(R.string.user_scan_result,index,response));return;}inaccessible++;index++;nextUser();}else read("2a9a");}
    private final BluetoothGattCallback callbacks=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){handler.post(()->{if(closed)return;if(status!=0||state==BluetoothProfile.STATE_DISCONNECTED){fail(context.getString(R.string.ble_36)+status+context.getString(R.string.ble_37));return;}if(state==BluetoothProfile.STATE_CONNECTED){timeout("services");if(!g.discoverServices())fail(context.getString(R.string.ble_39));}});}
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){handler.post(()->{if(closed)return;if(status!=0)fail(context.getString(R.string.ble_41)+status+").");else read("2a24");});}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){handler.post(()->{if(closed)return;if(status!=0)fail(context.getString(R.string.user_scan_result,index,status));else nextUser();});}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){handler.post(()->{if(closed)return;if(status!=0){fail(context.getString(R.string.user_scan_result,index,status));return;}writeDone=true;consentComplete();});}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){byte[] value=c.getValue()==null?new byte[0]:c.getValue().clone();handler.post(()->{if(closed||!c.getUuid().equals(id("2a9f"))||value.length<3||value[0]!=0x20||value[1]!=2)return;response=value[2]&255;responseDone=true;consentComplete();});}
        @Override public void onCharacteristicRead(BluetoothGatt g,BluetoothGattCharacteristic c,int status){byte[] value=c.getValue()==null?new byte[0]:c.getValue().clone();handler.post(()->{if(closed)return;if(status!=0){fail(context.getString(R.string.user_scan_result,index,status));return;}try{UUID u=c.getUuid();if(u.equals(id("2a24"))){if(!new String(value,java.nio.charset.StandardCharsets.UTF_8).replace("\u0000","").equals("BF990")){fail(context.getString(R.string.ble_48));return;}subscribe();}else if(u.equals(id("2a9a"))){if(value.length!=1||(value[0]&255)!=index){fail(context.getString(R.string.ble_51));return;}read("2a85");}else if(u.equals(id("2a85"))){if(value.length!=4)throw new IllegalArgumentException();birthday=LocalDate.of(Measurements.u16(value,0),value[2]&255,value[3]&255);read("2a8e");}else if(u.equals(id("2a8e"))){if(value.length!=2)throw new IllegalArgumentException();int height=Measurements.u16(value,0);if(height<50||height>250)throw new IllegalArgumentException();listener.found(index,birthday,height);index++;nextUser();}}catch(RuntimeException ex){fail(context.getString(R.string.birthday_invalid_response));}});}
    };
}
