package com.example.hp.virtualeye;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static  final String FILE_NAME="example.txt";
    private static final int PERMISSION_REQUEST_ID = 1;
    private static Scanner_BTLE mBTLEScanner;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager layoutManager;
    BluetoothAdapter bluetoothAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    ImageButton EditBtn;
    private BluetoothGatt mGatt;
    private BluetoothDevice bluetoothDevice;
    BTLE_Device btle_device;

    private ExampleAdapter mAdapter;
    private HashMap<String, BTLE_Device> mBTDeviceHashMap;
    private ArrayList<BTLE_Device> mDevice;
    Button record;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    String result;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        if (!mBTLEScanner.ismScanning()) {
            startScan();
        } else {
            stopScan();
        }


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

        mBTLEScanner.start();
    }


    public void stopScan() {
        mBTLEScanner.stop();



    }


    @Override
    protected void onResume() {
        super.onResume();
        load();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }


    //Called When an Item is Clicked
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    public void addDevice(BluetoothDevice device) {

        String address = device.getAddress();
        String name = device.getName();
        // Toast.makeText(this,"NAme: "+ name,Toast.LENGTH_LONG).show();

        if (!mBTDeviceHashMap.containsKey(address) ) {
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



    public void connectToDevice(BluetoothDevice device) {
        if(mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            //mBTLEScanner.stop();// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    Toast.makeText(MainActivity.this,"connected to " + btle_device.getName(),Toast.LENGTH_LONG ).show();
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };



    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();

    }
}
