package com.example.hp.virtualeye;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;


public class Scanner_BTLE {

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

        }
    };
    private MainActivity ma;
    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private long scanPeriod;
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final int new_rssi = rssi;


                    mHandler.post(() -> ma.addDevice(device, new_rssi));
                }

            };

    public Scanner_BTLE(MainActivity mainActivity, long scanPeriod) {

        ma = mainActivity;

        mHandler = new Handler();

        this.scanPeriod = scanPeriod;


        final BluetoothManager bluetoothManager = (BluetoothManager) ma.getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public boolean ismScanning() {
        return mScanning;
    }

    public void start() {
       /* if (!bluetoothAdapter.isEnabled()) {
            ma.stopScan();
            Toast.makeText(this.ma,"Bluetooth is OFF",Toast.LENGTH_LONG).show();
        } else {*/
        scanLeDevice(true);

    }

    public void stop() {

        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Toast.makeText(this.ma, "Started Scanning", Toast.LENGTH_LONG).show();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(() -> {
                mScanning = false;
                bluetoothAdapter.stopLeScan(leScanCallback);

            }, scanPeriod);

            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            Toast.makeText(this.ma, "No Device Found", Toast.LENGTH_LONG).show();
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }

    }


}