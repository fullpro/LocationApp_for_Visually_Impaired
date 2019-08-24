package com.example.hp.virtualeye;

import android.bluetooth.BluetoothDevice;

public class MyBluetoothDevice {
    BluetoothDevice bluetoothDevice;
    String name;
    String address;
    public int count;

    public MyBluetoothDevice() {
        count = 0;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
