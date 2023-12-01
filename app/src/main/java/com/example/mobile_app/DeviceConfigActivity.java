package com.example.mobile_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

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

    // Game config service UUIDs
    public static final UUID GAME_SERVICE_UUID = new UUID(0x00, 0x03);
    public static final UUID TIME_CONTROL_CHAR_UUID = new UUID(0x00, 0x01);
    public static final UUID OPPONENT_TYPE_CHAR_UUID = new UUID(0x00, 0x02);
    public static final UUID OPPONENT_USERNAME_CHAR_UUID = new UUID(0x00, 0x03);
    public static final UUID BUTTON_CHAR_UUID = new UUID(0x00, 0x04);
    private BluetoothGattService gameService = new BluetoothGattService(GAME_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    private BluetoothGattCharacteristic timeControlChar = new BluetoothGattCharacteristic(TIME_CONTROL_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
    private BluetoothGattCharacteristic opponentTypeChar = new BluetoothGattCharacteristic(OPPONENT_TYPE_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
    private BluetoothGattCharacteristic opponentUsernameChar = new BluetoothGattCharacteristic(OPPONENT_USERNAME_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
    private BluetoothGattCharacteristic buttonChar = new BluetoothGattCharacteristic(BUTTON_CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

    // BLE
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGattServer bleServer;
    private BluetoothDevice bleDevice;

    // Text input
    private EditText ssidInput;
    private EditText pwInput;
    private static EditText opponentUsernameInput;

    // Buttons
    private Button wifiConnectButton;
    private Button lichessLoginButton;

    // Text views
    private static TextView wifiConnectStatus;
    private static AutoCompleteTextView timeControl;

    // Radio group
    private static RadioGroup opponentType;

    // Buttons
    private Button makeGame;
    private Button clock;
    private Button draw;
    private Button resign;

    // UI elements
    private static String ssid = "";
    private static boolean isConnected = false;
    private static int timeControlIndex = 2;
    private static int opponentTypeIndex = 0;
    private static String opponentUsername = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_config);

        ssidInput = findViewById(R.id.ssid);
        pwInput = findViewById(R.id.pw);
        wifiConnectButton = findViewById(R.id.wifiConnectButton);
        wifiConnectStatus = findViewById(R.id.wifiConnectStatus);
        lichessLoginButton = findViewById(R.id.lichessLoginButton);
        timeControl = findViewById(R.id.timeControlSelect);
        opponentType = findViewById(R.id.opponentType);
        opponentUsernameInput = findViewById(R.id.opponentUsername);
        makeGame = findViewById(R.id.makeGame);
        clock = findViewById(R.id.clock);
        draw = findViewById(R.id.draw);
        resign = findViewById(R.id.resign);

        wifiConnectButton.setOnClickListener(connectWifiListener);
        lichessLoginButton.setOnClickListener(loginLichessListener);
        timeControl.setOnItemClickListener(timeControlClickListener);
        opponentType.setOnCheckedChangeListener(opponentTypeChangedListener);
        opponentUsernameInput.setOnFocusChangeListener(opponentUsernameListener);
        makeGame.setOnClickListener(buttonOnClickListener);
        clock.setOnClickListener(buttonOnClickListener);
        draw.setOnClickListener(buttonOnClickListener);
        resign.setOnClickListener(buttonOnClickListener);
        uiHandler.sendMessage(new Message());

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
        gameService.addCharacteristic(timeControlChar);
        gameService.addCharacteristic(opponentTypeChar);
        gameService.addCharacteristic(opponentUsernameChar);
        gameService.addCharacteristic(buttonChar);

        // Rest of the services are added in callback
        bleServer.addService(wifiService);

        bleScanner.startScan(bleScanCallback);
    }

    private static final Handler uiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (isConnected) {
                wifiConnectStatus.setText("Connected to " + ssid);
                wifiConnectStatus.setTextColor(Color.GREEN);
            }
            else {
                if (ssid.isEmpty()) {
                    wifiConnectStatus.setText("Please connect to wifi");
                }
                else {
                    wifiConnectStatus.setText("Failed connecting to " + ssid);
                }
                wifiConnectStatus.setTextColor(Color.RED);
            }

            Log.e("Donkey", "" + timeControlIndex);
            timeControl.setText(timeControl.getAdapter().getItem(timeControlIndex).toString(), false);

            if (opponentTypeIndex == 0) {
                opponentUsernameInput.setEnabled(false);
                opponentType.check(R.id.radioRandom);
            }
            else if (opponentTypeIndex == 1) {
                opponentUsernameInput.setEnabled(true);
                opponentType.check(R.id.radioSpecific);
            }
            opponentUsernameInput.setText(opponentUsername);
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        AuthorizationResponse resp = AuthorizationResponse.fromIntent(intent);
        if (resp != null) {
            // authorization completed
            AuthorizationService authService = new AuthorizationService(this);
            authService.performTokenRequest(
                    resp.createTokenExchangeRequest(),
                    new AuthorizationService.TokenResponseCallback() {
                        @Override
                        public void onTokenRequestCompleted(
                                TokenResponse resp, AuthorizationException ex) {
                            if (resp != null) {
                                // exchange succeeded
                                String bearerToken = resp.accessToken;
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
                                    if (bleDevice != null) {
                                        bleServer.notifyCharacteristicChanged(bleDevice, bearerTokenChar, false, bearerToken.getBytes());
                                    }
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Failed to authenticate", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Didn't authorize
            Toast.makeText(this, "Failed to authenticate", Toast.LENGTH_SHORT).show();
        }
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
            if (bleDevice != null) {
                bleServer.notifyCharacteristicChanged(bleDevice, ssidChar, false, ssid.getBytes());
                bleServer.notifyCharacteristicChanged(bleDevice, pwChar, false, pwInput.getText().toString().getBytes());
            }
        }
    };

    private final View.OnClickListener loginLichessListener = view -> {
        AuthorizationServiceConfiguration serviceConfig =
                new AuthorizationServiceConfiguration(
                        Uri.parse("https://lichess.org/oauth"), // authorization endpoint
                        Uri.parse("https://lichess.org/api/token")); // token endpoint

        AuthorizationRequest.Builder authRequestBuilder =
                new AuthorizationRequest.Builder(
                        serviceConfig, // the authorization service configuration
                        "2", // the client ID, typically pre-registered and static
                        ResponseTypeValues.CODE, // the response_type value: we want a code
                        Uri.parse("com.example.mobileapp:/oauth2redirect")); // the redirect URI to which the auth response is sent

        AuthorizationRequest authRequest = authRequestBuilder
                .setScope("email:read preference:read challenge:read challenge:write challenge:bulk board:play bot:play")
                .build();

        AuthorizationService authService = new AuthorizationService(DeviceConfigActivity.this);

        Intent intent = new Intent(this, DeviceConfigActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        authService.performAuthorizationRequest(
                authRequest,
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE),
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE));
    };

    private final AdapterView.OnItemClickListener timeControlClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            byte[] data = { (byte)(i) };
            timeControlIndex = i;
            if (bleDevice != null) {
                bleServer.notifyCharacteristicChanged(bleDevice, timeControlChar, false, data);
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener opponentTypeChangedListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            if (radioGroup.getCheckedRadioButtonId() == R.id.radioRandom) {
                opponentTypeIndex = 0;
            }
            else if (radioGroup.getCheckedRadioButtonId() == R.id.radioSpecific) {
                opponentTypeIndex = 1;
            }

            uiHandler.sendMessage(new Message());
            byte[] data = { (byte)(opponentTypeIndex)};

            if (bleDevice != null) {
                bleServer.notifyCharacteristicChanged(bleDevice, opponentTypeChar, false, data);
            }

            if (opponentTypeIndex == 1 && bleDevice != null) {
                bleServer.notifyCharacteristicChanged(bleDevice, opponentUsernameChar, false, opponentUsernameInput.getText().toString().getBytes());
            }
        }
    };

    private View.OnClickListener buttonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int val;
            if (view == makeGame) {
                val = 1;
            }
            else if (view == clock) {
                val = 2;
            }
            else if (view == draw) {
                val = 3;
            }
            else if (view == resign) {
                val = 4;
            }
            else {
                return;
            }

            byte []data = {(byte)(val)};
            bleServer.notifyCharacteristicChanged(bleDevice, buttonChar, false, data);
        }
    };

    private View.OnFocusChangeListener opponentUsernameListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if (!b) {
                opponentUsername = opponentUsernameInput.getText().toString();
                bleServer.notifyCharacteristicChanged(bleDevice, opponentUsernameChar, false, opponentUsername.getBytes());
            }
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
            else if (service.getUuid() == LICHESS_SERVICE_UUID) {
                bleServer.addService(gameService);
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
                }
                else {
                    ssid = value.toString();
                }
            }
            else if (characteristic == connectedChar) {
                isConnected = (value[0] != 0);
            }
            else if (characteristic == timeControlChar) {
                timeControlIndex = value[0];
            }
            else if (characteristic == opponentTypeChar) {
                opponentTypeIndex = value[0];
            }
            else if (characteristic == opponentUsernameChar) {
                opponentUsername = value[0] == 0 ? "" : value.toString();
            }

            uiHandler.sendMessage(new Message());
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