package com.example.hp.virtualeye;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private  static  final String TAG = "BlutoothGattAtivity";

    private ProgressDialog mProgress;

    private static  final String FILE_NAME="example.txt";
    private static final int PERMISSION_REQUEST_ID = 1;
    private static Scanner_BTLE mBTLEScanner;


    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager layoutManager;
    BluetoothAdapter bluetoothAdapter;
    ImageButton EditBtn;
    Button record;


    private SwipeRefreshLayout mySwipeRefreshLayout;
    private BluetoothGatt mGatt;
    private BluetoothDevice bluetoothDevice;

   //  Handler mHandler;

    private ExampleAdapter mAdapter;
    private HashMap<String, BTLE_Device> mBTDeviceHashMap;
    private ArrayList<BTLE_Device> mDevice;

    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    String result;




    private static final int NO_ALERT = 0x00;
    //public static final int MEDIUM_ALERT = 0x01;
    private static final int HIGH_ALERT = 0x02;

    private static final UUID IMMEDIATE_ALERT_SERVICE =
            UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    private static final UUID FIND_ME_SERVICE =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID LINK_LOSS_SERVICE =
            UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_SERVICE =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID GENERIC_SERVICE =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID ALERT_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);



        //Hide status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        record=findViewById(R.id.record);
        mDevice=new ArrayList<>();
        mBTDeviceHashMap=new HashMap<>();

        load();


        mRecyclerView = findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        mAdapter = new ExampleAdapter(mDevice);

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        mRecyclerView.setAdapter(mAdapter);

        record.setOnClickListener(v -> startVoiceRecognitionActivity());

        mBTLEScanner = new Scanner_BTLE(this, 15000);

        mAdapter.setOnItemClickListener(position -> {
            String a = mDevice.get(position).getName();
            bluetoothDevice = mDevice.get(position).getDevice();
           // startVoiceRecognitionActivity();
            connectToDevice(bluetoothDevice);


        });


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Permission For Location
        if (Build.VERSION.SDK_INT > 23) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                ActivityCompat.requestPermissions(this, new String[]{ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_ID);
            }
        }


        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        //Enable Bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        startScan();


        mySwipeRefreshLayout = findViewById(R.id.swiperefresh);
        /*
         * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
         * performs a swipe-to-refresh gesture.
         */
        mySwipeRefreshLayout.setOnRefreshListener(
                () -> {


                    // This method performs the actual data-refresh operation.
                    // The method calls setRefreshing(false) when it's finished.
                    startScan();
                    new Handler().postDelayed(() -> mySwipeRefreshLayout.setRefreshing(false), 4000);
                }
        );

        EditBtn = findViewById(R.id.EditBtn);

        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK)
        {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            result=matches.get(0);

            for(BTLE_Device btle_device:mDevice)
            {
                if (matches.get(0).equalsIgnoreCase(btle_device.getName()))
                {
                    Toast.makeText(MainActivity.this,btle_device.getName()+" selected",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }



    public void save(){

        /*Toast.makeText(MainActivity.this,"In show",Toast.LENGTH_SHORT).show();
        SharedPreferences appSharedPrefs = getSharedPreferences("shared preferences",MODE_PRIVATE);
       SharedPreferences.Editor prefsEditor = appSharedPrefs.edit();
       Gson gson=new Gson();
       String json=gson.toJson(mDevice);
       prefsEditor.putString("my list",json);
       prefsEditor.apply();*/

       /* try
        {
            FileOutputStream fos = context.openFileOutput("YourInfomration.ser", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(mBTDeviceHashMap);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } */


    }



    public void load(){
        /*SharedPreferences appSharedPrefs = getSharedPreferences("shared preferences",MODE_PRIVATE);
        Gson gson = new Gson();
        String json = appSharedPrefs.getString("my list", null);
            Type type = new TypeToken<ArrayList<BTLE_Device>>(){}.getType();


            if (mDevice==null)
            {
                mDevice=new ArrayList<>();
            }
            else {
                if (json!=null) {
                    mDevice.add(gson.fromJson(json, type));

                }

            }*/



       /* try
        {
            FileInputStream fileInputStream = new FileInputStream(context.getFilesDir()+"/FenceInformation.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Map myHashMap = (Map)objectInputStream.readObject();

        }
        catch(ClassNotFoundException | IOException | ClassCastException e) {
            e.printStackTrace();
        }*/

    }



    public void startScan() {
        mDevice.clear();
        mBTDeviceHashMap.clear();
        mAdapter.notifyDataSetChanged();
        bluetoothAdapter.startLeScan(leScanCallback);
        setProgressBarIndeterminateVisibility(false);
        mHandler.postDelayed(mStopRunnable,2500);


       // mBTLEScanner.start();
    }


    public void stopScan() {
        //mBTLEScanner.stop();

        bluetoothAdapter.stopLeScan(leScanCallback);
        setProgressBarIndeterminateVisibility(false);


    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {



                    mHandler.post(() -> addDevice(device));
                }

            };


    @Override
    protected void onResume() {
        super.onResume();
        load();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mProgress.dismiss();
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        bluetoothAdapter.stopLeScan(leScanCallback);

    }

    @Override
    public void onStop(){
        super.onStop();
        if (mGatt != null){
            mGatt.disconnect();
            mGatt = null;
        }
    }

    private Runnable mStopRunnable = this::stopScan;
    private Runnable mStartRunnable = this::stopScan;



    public void addDevice(BluetoothDevice device) {

        String address = device.getAddress();
        String name = device.getName();
        // Toast.makeText(this,"NAme: "+ name,Toast.LENGTH_LONG).show();

        if (!mBTDeviceHashMap.containsKey(address) && address.startsWith("FF:FF:") ) {
            BTLE_Device btle_device = new BTLE_Device(device);

            {
                mBTDeviceHashMap.put(address, btle_device);
                mDevice.add(btle_device);

            }


        } else {
            mBTDeviceHashMap.get(address);
        }

        mAdapter.notifyDataSetChanged();
    }



    public void connectToDevice(BluetoothDevice device ) {
        if(mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            //mBTLEScanner.stop();// will stop after first device detection
        }
    }




    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        private int mState = 0;

        private void reset() {mState = 0;}

        private void advance() {mState++;}

        private void enableNextSensor(BluetoothGatt gatt){
            BluetoothGattCharacteristic characteristic;
            switch (mState){
                case  0:
                    Log.d(TAG, "Enabling find me service ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    characteristic.setValue(new byte[] {0x02});
                    break;
                case 1:
                    Log.d(TAG, "Enabling find me service2 ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    characteristic.setValue(new byte[] {0x01});
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }
            gatt.writeCharacteristic(characteristic);
        }


        private void readNextSensor(BluetoothGatt gatt){
            BluetoothGattCharacteristic characteristic;
            switch (mState){
                case  0:
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

        private void setNotifyNextSensor(BluetoothGatt gatt){
            BluetoothGattCharacteristic characteristic;
            switch (mState){
                case  0:
                    Log.d(TAG, "set notify find me service ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    break;
                case 1:
                    Log.d(TAG, "set notify find me service2 ");
                    characteristic = gatt.getService(FIND_ME_SERVICE).getCharacteristic(ALERT_LEVEL_CHARACTERISTIC);
                    return;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG,"All sensors enabled");
                    return;
            }
            gatt.setCharacteristicNotification(characteristic,true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onConnectionStateChange (BluetoothGatt gatt, int status , int newState){
            Log.d(TAG, "Connection State change: " +status+ " -> " + connectionState(newState));
            if(status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must discover all services on the device
                 * before we can read and wright their characteristics
                 */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering services..."));

            }else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED){
                mHandler.sendEmptyMessage(MSG_CLEAR);

            }else if (status != BluetoothGatt.GATT_SUCCESS){
                gatt.disconnect();
            }
        }


        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            Log.d(TAG, "Services Discovered: " +status);
            mHandler.sendMessage(Message.obtain(null,MSG_PROGRESS,"Enabling Sensors..."));
            mProgress.dismiss();
            gatt.disconnect();
            mHandler.removeCallbacks(mStopRunnable);
            mHandler.removeCallbacks(mStartRunnable);
            bluetoothAdapter.stopLeScan(leScanCallback);
            gatt.discoverServices();

            reset();
            enableNextSensor(gatt);
        }


        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if(ALERT_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_ALERT,characteristic));
            }
            setNotifyNextSensor(gatt);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){

            readNextSensor(gatt);
        }



        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){

            if (FIND_ME_SERVICE.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_SERVICE, characteristic));
            }
        }



        @Override
        public void onDescriptorWrite (BluetoothGatt gatt,BluetoothGattDescriptor descriptor, int status){
            advance();
            enableNextSensor(gatt);
        }
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            Log.d(TAG,"Remote rssi: " +rssi);
        }

        private String connectionState(int status){
            switch (status){
                case BluetoothProfile.STATE_CONNECTED:
                    return  "Connected";
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
    private  Handler mHandler = new Handler() {
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

    private void updateAlertValues(BluetoothGattCharacteristic characteristic){

    }

    private void updateServiceValues(BluetoothGattCharacteristic characteristic){

    }



}
