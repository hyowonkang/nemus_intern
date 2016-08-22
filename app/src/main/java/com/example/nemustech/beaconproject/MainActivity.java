package com.example.nemustech.beaconproject;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    private ArrayList<Integer> arr;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        arr = new ArrayList<>();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }
//
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }


    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {

            BeaconRecord beacon = new BeaconRecord(result);

                peripheralTextView.append("Device Name: " + result.getDevice().getName() + "\nrssi: " + result.getRssi() + "\nAddress: " + result.getDevice().getAddress() +
                        "\nUUID: " + beacon.getUuid() + "\nMajor: " + beacon.getMajor() + "\nMinor: " + beacon.getMinor() + "\nTemperature : " + beacon.getTemper() +
                        "\nLight : " + beacon.getLight() + "\nHumidity : " + beacon.getHum()+"\nBattery level :"+beacon.getBattery()+"\n\n");
                final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
                if (scrollAmount > 0)
                    peripheralTextView.scrollTo(0, scrollAmount);


        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }



    static class BeaconRecord {
        private String uuid;
        private int minor;
        private int major;
        private int rssi;
        private float temper;
        private int light;
        private int battery;
        private float batteryValue;
        private int hum;
        private String name;
        private String address;



        public BeaconRecord(ScanResult result) {
            int startByte = 2;
            boolean patternFound = false;
            byte[] scanRecord = result.getScanRecord().getBytes();

            while (startByte <= 5) {
                if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && ((int) scanRecord[startByte + 3] & 0xff) == 0x15) {
                    patternFound = true;
                    break;
                }
                startByte++;
            }


            if (patternFound) {
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);


                uuid = hexString.substring(0, 8) + "-" +
                        hexString.substring(8, 12) + "-" +
                        hexString.substring(12, 16) + "-" +
                        hexString.substring(16, 20) + "-" +
                        hexString.substring(20, 32);

                //Here is your Major value
                major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

                //Here is your Minor value
                minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);


                if((scanRecord.length > startByte) && (scanRecord[startByte + 25] & 0xff) == 0x06){
                    startByte += 26;

                    while(((scanRecord[startByte] & 0xff) != 0x20) && ((scanRecord[startByte + 1] & 0xff) != 0x18)){

                        startByte++;

                        if(startByte > 60){
                            patternFound = false;
                            break;
                        }
                    }

                    if(patternFound){
                        light = (scanRecord[startByte + 2] & 0xff);
                        temper = (scanRecord[startByte + 3] & 0xff);
                        hum = (scanRecord[startByte + 4] & 0xff);
                        batteryValue = (scanRecord[startByte+5] &0xff);
                        Log.d("testt", scanRecord[startByte+2]+""+scanRecord[startByte+3]+""+scanRecord[startByte+4]+"");
                    }
                }
            }

                battery = (int)convertToBatteryLevel(batteryValue);
                this.rssi = result.getRssi();
                this.name = result.getDevice().getName();
                this.address = result.getDevice().getAddress();



        }
        public String getUuid(){return uuid;}
        public int getMinor(){return minor;}
        public int getMajor(){return major;}
        public float getTemper(){return temper;}
        public int getLight(){return light;}
        public int getHum(){return hum;}
        public float getBattery(){return battery;}



        public static float convertToBatteryLevel(float sensorValue) {
            if (0xAC < sensorValue) return 100;
            else if (0xAA < sensorValue) return 95;
            else if (0xA8 < sensorValue) return 90;
            else if (0xA7 < sensorValue) return 85;
            else if (0xA5 < sensorValue) return 80;
            else if (0xA3 < sensorValue) return 75;
            else if (0xA2 < sensorValue) return 70;
            else if (0xA0 < sensorValue) return 65;
            else if (0x9F < sensorValue) return 60;
            else if (0x9E < sensorValue) return 55;
            else if (0x9C < sensorValue) return 50;
            else if (0x9B < sensorValue) return 45;
            else if (0x9A < sensorValue) return 40;
            else if (0x96 < sensorValue) return 35;
            else if (0x8C < sensorValue) return 30;
            else if (0x83 < sensorValue) return 25;
            else if (0x7E < sensorValue) return 20;
            else if (0x79 < sensorValue) return 15;
            else if (0x72 < sensorValue) return 10;
            else if (0x68 < sensorValue) return 5;
            else return 0;
        }


    }


        static final char[] hexArray = "0123456789ABCDEF".toCharArray();
        private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}

