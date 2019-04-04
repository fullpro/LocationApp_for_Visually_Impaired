package com.example.hp.virtualeye;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.provider.Settings.Secure.LOCATION_MODE;

public class MainActivity extends AppCompatActivity {


    private static final int PERMISSION_REQUEST_ID = 1;
    private static final String LOG_TAG = "hi";
    RecyclerView list;
    private Context context;

    private TreeMap<String, BTLE_Device> mBTDeviceHashMap = null;
     SortedSet<BTLE_Device> sortedSet;

    private RecyclerView mrecycler_view;
    private ExampleAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<BTLE_Device> mDevice;
    BluetoothAdapter bluetoothAdapter;
    SwipeRefreshLayout mySwipeRefreshLayout;


    private Scanner_BTLE mBTLEScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Hide status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mDevice = new ArrayList<>();
        SortedSet<BTLE_Device> sortedSet = new TreeSet<>();


        mrecycler_view = findViewById(R.id.list);
        mrecycler_view.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        mAdapter = new ExampleAdapter(mDevice);

        mrecycler_view.setLayoutManager(layoutManager);
        mrecycler_view.setLayoutManager(new GridLayoutManager(this, 2));
        mrecycler_view.setAdapter(mAdapter);

        mBTLEScanner = new Scanner_BTLE(this, 10000);

        mAdapter.setOnItemClickListener(new ExampleAdapter.onItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String a = mDevice.get(position).getName();
                Toast.makeText(MainActivity.this, a + " Clicked", Toast.LENGTH_SHORT).show();
            }
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


    }


    public void startScan(){
       mDevice.clear();

       //mBTDeviceHashMap.clear();
        mAdapter.notifyDataSetChanged();

        mBTLEScanner.start();
    }


    public  void stopScan(){
        mBTLEScanner.stop();
    }



    @Override
    protected void onResume(){
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopScan();
    }


    //Called When an Item is Clicked
    public  void onItemClick(AdapterView<?> parent, View view, int position, long id){

    }

    public  void addDevice(BluetoothDevice device, int new_rssi) {
        String address = device.getAddress();


        //if (mBTDeviceHashMap == null) {
                BTLE_Device btle_device = new BTLE_Device(device);
                btle_device.setRSSI(new_rssi);

              //  mBTDeviceHashMap.put(address, btle_device);
                mDevice.add(btle_device);
            //} else {
              //  Objects.requireNonNull(mBTDeviceHashMap.get(address)).setRSSI(new_rssi);

        mAdapter.notifyDataSetChanged();

    }





    @Override
    protected void onDestroy(){
        super.onDestroy();

    }
}
