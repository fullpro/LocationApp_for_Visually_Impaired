package com.example.hp.virtualeye;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
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

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.provider.Settings.Secure.LOCATION_MODE;
import static android.support.v7.widget.AppCompatDrawableManager.get;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED = "shared_values";
    public static final String DEVICES_LIST = "devices_list";
    public static final String MAP_LIST = "hashmap";
    private static final int PERMISSION_REQUEST_ID = 1;
    private static Scanner_BTLE mBTLEScanner;
    RecyclerView mrecycler_view;
    RecyclerView.LayoutManager layoutManager;
    BluetoothAdapter bluetoothAdapter;
    SwipeRefreshLayout mySwipeRefreshLayout;
    ImageButton EditBtn;
    private ExampleAdapter mAdapter;
    private Map<String, BluetoothDevice> mBTDeviceHashMap = new HashMap<String, BluetoothDevice>();
    private List<MyBluetoothDevice> mDevice = new ArrayList<MyBluetoothDevice>();
    Button record;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    String result;
    public int finalPosition=99999;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        record = findViewById(R.id.record);
        SharedPreferences prefs = getSharedPreferences(SHARED, MODE_PRIVATE);
        String saved1=prefs.getString(DEVICES_LIST,null);
        if (saved1 != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<MyBluetoothDevice>>() {}.getType();
            mDevice.addAll(gson.fromJson(saved1,type));
            for (MyBluetoothDevice myBluetoothDevice : mDevice) {
                mBTDeviceHashMap.put(myBluetoothDevice.address,myBluetoothDevice.getBluetoothDevice());
            }
        }


        mrecycler_view = findViewById(R.id.list);
        mrecycler_view.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        mAdapter = new ExampleAdapter(mDevice);
        mrecycler_view.setLayoutManager(layoutManager);
        mrecycler_view.setLayoutManager(new GridLayoutManager(this, 2));
        mrecycler_view.setAdapter(mAdapter);
        mBTLEScanner = new Scanner_BTLE(this, 15000);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

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
            finalPosition=position;
            startVoiceRecognitionActivity();


        });

        mySwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mySwipeRefreshLayout.setOnRefreshListener(() -> {
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.
            startScan();
            new Handler().postDelayed(() -> mySwipeRefreshLayout.setRefreshing(false), 4000);
        });
        ;
        EditBtn = findViewById(R.id.EditBtn);

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
            if(finalPosition!=99999) {
                mDevice.get(finalPosition).setName(result);
                mAdapter.notifyDataSetChanged();
                addSavedPref(mDevice);
                finalPosition=99999;
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

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }


    public void addDevice(MyBluetoothDevice device) {
        String address = device.getAddress();
        if (!mBTDeviceHashMap.containsKey(address) && device.getName() != null) {
            if (device.getName().contains("iTAG"))
            {
                mBTDeviceHashMap.put(address, device.getBluetoothDevice());
                mDevice.add(device);
                addSavedPref(mDevice);
            }
        } else {
            mBTDeviceHashMap.get(address);
        }

        mAdapter.notifyDataSetChanged();
    }

    public  void addSavedPref(List<MyBluetoothDevice>bluetoothDevices)
    {
        Gson gson = new Gson();
        String devicesList=gson.toJson(bluetoothDevices);
        SharedPreferences.Editor editor = getSharedPreferences(SHARED, MODE_PRIVATE).edit();
        editor.putString(DEVICES_LIST,devicesList);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


}
