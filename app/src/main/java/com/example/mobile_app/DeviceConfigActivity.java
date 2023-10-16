package com.example.mobile_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class DeviceConfigActivity extends AppCompatActivity {
    // Wifi service UUIDs
    private static final UUID WIFI_SERVICE_UUID = new UUID(0x00, 0x01);
    private static final UUID SSID_CHAR_UUID = new UUID(0x00, 0x01);
    private static final UUID PW_CHAR_UUID = new UUID(0x00, 0x02);
    private static final UUID CONNECTED_UUID = new UUID(0x00, 0x03);
    private BluetoothGattService wifiService = new BluetoothGattService(WIFI_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    private BluetoothGattCharacteristic ssidChar = new BluetoothGattCharacteristic(SSID_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
    private BluetoothGattCharacteristic pwChar = new BluetoothGattCharacteristic(PW_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
    private BluetoothGattCharacteristic connectedChar = new BluetoothGattCharacteristic(CONNECTED_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

    // Lichess service UUIDs
    private static final UUID LICHESS_SERVICE_UUID = new UUID(0x00, 0x02);
    private static final UUID BEARER_TOKEN_CHAR_UUID = new UUID(0x00, 0x01);
    private BluetoothGattService lichessService = new BluetoothGattService(LICHESS_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    private BluetoothGattCharacteristic bearerTokenChar = new BluetoothGattCharacteristic(BEARER_TOKEN_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

    // BLE
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGattServer bleServer;
    private BluetoothDevice bleDevice;

    // Text input
    private EditText ssidInput;
    private EditText pwInput;

    // Buttons
    private Button connectButton;

    // Text views
    private TextView connectStatus;

    private String ssid;
    boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_config);

        ssidInput = findViewById(R.id.ssid);
        pwInput = findViewById(R.id.pw);
        connectButton = findViewById(R.id.connectButton);
        connectStatus = findViewById(R.id.connectStatus);
        connectButton.setOnClickListener(connectWifiListener);

        btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        bleScanner = btAdapter.getBluetoothLeScanner();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        bleServer = btManager.openGattServer(this, bleServerCallback);
        wifiService.addCharacteristic(ssidChar);
        wifiService.addCharacteristic(pwChar);
        wifiService.addCharacteristic(connectedChar);
        lichessService.addCharacteristic(bearerTokenChar);
        bleServer.addService(wifiService);

        bleScanner.startScan(bleScanCallback);
    }

    private final View.OnClickListener connectWifiListener = view -> {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ssid = ssidInput.getText().toString();
            bleServer.notifyCharacteristicChanged(bleDevice, ssidChar, false, ssid.getBytes());
            bleServer.notifyCharacteristicChanged(bleDevice, pwChar, false, pwInput.getText().toString().getBytes());
        }
    };

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            ScanRecord record = result.getScanRecord();
            if (record.getDeviceName() != null && record.getDeviceName().trim().equals("ESP32-S3")) {
                Log.d("donkey", result.getDevice().getName());
                bleScanner.stopScan(this);
                bleDevice = result.getDevice();
                bleServer.connect(bleDevice, false);
            }
        }
    };

    private BluetoothGattServerCallback bleServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d("donkey", "permissions not set correctly");
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                BluetoothGattService service = bleServer.getService(WIFI_SERVICE_UUID);
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(SSID_CHAR_UUID);
                Toast.makeText(getApplicationContext(), "Paired with board", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (service.getUuid() == WIFI_SERVICE_UUID) {
                bleServer.addService(lichessService);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bleServer.sendResponse(device, requestId, BluetoothStatusCodes.SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bleServer.sendResponse(device, requestId, BluetoothStatusCodes.SUCCESS, offset, value);

            if (characteristic == ssidChar) {
                if (value[0] == 0) {
                    ssid = "";
                    connectStatus.setText("Please connect to wifi");
                    connectStatus.setTextColor(Color.RED);
                }
                else {
                    ssid = value.toString();
                    connectStatus.setText("Connected to " + ssid);
                    isConnected = true;
                    connectStatus.setTextColor(Color.GREEN);
                }
            }
            else if (characteristic == connectedChar) {
                isConnected = value[0] != 0;
                if (isConnected) {
                    connectStatus.setText("Connected to " + ssid);
                    connectStatus.setTextColor(Color.GREEN);
                }
                else {
                    if (ssid.isEmpty()) {
                        connectStatus.setText("Please connect to wifi");
                    }
                    else {
                        connectStatus.setText("Failed connecting to " + ssid);
                    }
                    connectStatus.setTextColor(Color.RED);
                }
            }
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }
    };
}