package com.example.hp.virtualeye;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static android.content.ContentValues.TAG;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.provider.Settings.Secure.LOCATION_MODE;
import static android.support.v7.widget.AppCompatDrawableManager.get;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED = "shared_values";
    public static final String DEVICES_LIST = "devices_list";
    public static final String MAP_LIST = "hashmap";
    private static final String TAG = "BlutoothGattAtivity";
    private ProgressDialog mProgress;
    private static final int PERMISSION_REQUEST_ID = 1;
    private static Scanner_BTLE mBTLEScanner;
    RecyclerView mrecycler_view;
    RecyclerView.LayoutManager layoutManager;
    BluetoothAdapter bluetoothAdapter;
    SwipeRefreshLayout mySwipeRefreshLayout;
    ImageButton EditBtn;
    private ExampleAdapter mAdapter;
    private OnBoardActivity onBoardActivity;
    private Map<String, BluetoothDevice> mBTDeviceHashMap = new HashMap<String, BluetoothDevice>();
    private List<MyBluetoothDevice> mDevice = new ArrayList<MyBluetoothDevice>();
    Button record;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    String result;
    public int finalPosition = 99999;
    private BluetoothGatt mGatt;
    private static final int NO_ALERT = 0x00;
    //public static final int MEDIUM_ALERT = 0x01;
    private static final int HIGH_ALERT = 0x02;

    private static final UUID IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FIND_ME_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID LINK_LOSS_SERVICE = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID GENERIC_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        record = findViewById(R.id.record);
        SharedPreferences prefs = getSharedPreferences(SHARED, MODE_PRIVATE);
        String saved1 = prefs.getString(DEVICES_LIST, null);




        if (saved1 != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<MyBluetoothDevice>>() {
            }.getType();
            mDevice.addAll(gson.fromJson(saved1, type));

            for (MyBluetoothDevice myBluetoothDevice : mDevice) {
                mBTDeviceHashMap.put(myBluetoothDevice.address, myBluetoothDevice.getBluetoothDevice());
            }
        }

        mrecycler_view = findViewById(R.id.list);
        mrecycler_view.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        Comparator c = Collections.reverseOrder(new sortByCount());
        Collections.sort(mDevice, c);
        mAdapter = new ExampleAdapter(mDevice);
        mrecycler_view.setLayoutManager(layoutManager);
        mrecycler_view.setLayoutManager(new GridLayoutManager(this, 2));
        mrecycler_view.setAdapter(mAdapter);
        mBTLEScanner = new Scanner_BTLE(this, 15000);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionActivity();
            }
        });

        //Permission For Location
        if (Build.VERSION.SDK_INT > 23) {
            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                ActivityCompat.requestPermissions(this, new String[]{ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_ID);
            }
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        //Enable Bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        if (!mBTLEScanner.ismScanning()) {
            startScan();
        } else {
            stopScan();
        }

        mAdapter.setOnItemClickListener(position -> {
            finalPosition = position;
            startVoiceRecognitionActivity();
        });

        mySwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mySwipeRefreshLayout.setOnRefreshListener(() -> {
            startScan();
            new Handler().postDelayed(() -> mySwipeRefreshLayout.setRefreshing(false), 4000);
        });
        ;
        EditBtn = findViewById(R.id.EditBtn);
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Gson gson = new Gson();
        String save = gson.toJson(mDevice);
        String hashmap = gson.toJson(mBTDeviceHashMap);
        outState.putString(DEVICES_LIST, save);
        outState.putString(MAP_LIST, hashmap);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            result = matches.get(0);
            if (finalPosition != 99999) {
                mDevice.get(finalPosition).setName(result);
                mAdapter.notifyDataSetChanged();
                addSavedPref(mDevice);
                finalPosition = 99999;
            } else {
                for (MyBluetoothDevice myBluetoothDevice : mDevice) {
                    if (myBluetoothDevice.getName().equals(result)) {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(myBluetoothDevice.getAddress());
                        myBluetoothDevice.count++;
                        connectToDevice(device);
                        addSavedPref(mDevice);
                    }
                }
            }
        }
    }

    public void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }


    public void startScan() {
        mAdapter.notifyDataSetChanged();
        mBTLEScanner.start();
    }

    public void stopScan() {
        mBTLEScanner.stop();
    }


    public void addDevice(MyBluetoothDevice device) {
        String address = device.getAddress();
        if (!mBTDeviceHashMap.containsKey(address) && device.getName() != null) {
            if (device.getName().contains("iTAG")) {
                mBTDeviceHashMap.put(address, device.getBluetoothDevice());
                mDevice.add(device);
                addSavedPref(mDevice);
            }
        } else {
            mBTDeviceHashMap.get(address);
        }

        mAdapter.notifyDataSetChanged();
    }

    public void addSavedPref(List<MyBluetoothDevice> bluetoothDevices) {
        Gson gson = new Gson();
        String devicesList = gson.toJson(bluetoothDevices);
        SharedPreferences.Editor editor = getSharedPreferences(SHARED, MODE_PRIVATE).edit();
        editor.putString(DEVICES_LIST, devicesList);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            //mBTLEScanner.stop();// will stop after first device detection
            mGatt = null;
        }
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        private int mState = 0;

        private void reset() {
            mState = 0;
        }

        private void advance() {
            mState++;
        }

        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Enabling find me service ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    characteristic.setValue(new byte[]{0x02});
                    break;
                case 1:
                    Log.d(TAG, "Enabling find me service2 ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    characteristic.setValue(new byte[]{0x01});
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }
            gatt.writeCharacteristic(characteristic);
        }


        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Enabling find me service ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    break;
                case 1:
                    Log.d(TAG, "Enabling find me service2 ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    break;
                default:
                    return;
            }
            gatt.readCharacteristic(characteristic);
        }

        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "set notify find me service ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    break;
                case 1:
                    Log.d(TAG, "set notify find me service2 ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    return;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG, "All sensors enabled");
                    return;
            }
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must discover all services on the device
                 * before we can read and wright their characteristics
                 */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering services..."));

            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.sendEmptyMessage(MSG_CLEAR);

            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
            }
        }
//        private Runnable mStopRunnable = this::stopScan;
//        private Runnable mStartRunnable = this::stopScan;


        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: " + status);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            mProgress.dismiss();
            gatt.disconnect();
//            mHandler.removeCallbacks(mStopRunnable);
//            mHandler.removeCallbacks(mStartRunnable);
//            bluetoothAdapter.stopLeScan(leScanCallback);
            gatt.discoverServices();

            reset();
            enableNextSensor(gatt);
        }


        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (ALERT_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_ALERT, characteristic));
            }
            setNotifyNextSensor(gatt);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            readNextSensor(gatt);
        }


        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (FIND_ME_SERVICE.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_SERVICE, characteristic));
            }
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            advance();
            enableNextSensor(gatt);
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote rssi: " + rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };

    private static final int MSG_DISMISS = 202;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_CLEAR = 301;
    private static final int MSG_SERVICE = 101;
    private static final int MSG_ALERT = 102;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_ALERT:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.d(TAG, "Error obtaining alert value");
                        return;
                    }
                    updateAlertValues(characteristic);
                    break;
                case MSG_SERVICE:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.d(TAG, "Error obtaining service value");
                        return;
                    }
                    updateServiceValues(characteristic);
                    break;
                case MSG_PROGRESS:
                    mProgress.setMessage((String) msg.obj);
                    if (!mProgress.isShowing()) {
                        mProgress.show();
                    }
                    break;
                case MSG_DISMISS:
                    mProgress.hide();
                    break;
                case MSG_CLEAR:
                    // clearDisplayValues();
                    break;
            }
        }
    };

    private void updateAlertValues(BluetoothGattCharacteristic characteristic) {

    }

    private void updateServiceValues(BluetoothGattCharacteristic characteristic) {

    }


}

class sortByCount implements Comparator<MyBluetoothDevice> {
    // Used for sorting in ascending order of
    // roll number
    public int compare(MyBluetoothDevice a, MyBluetoothDevice b) {
        return a.count - b.count;
    }
}
