package com.arlab.blindnav.android.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.arlab.blindnav.R;
import com.arlab.blindnav.android.util.IndoorLocation.Point;
import com.arlab.blindnav.android.util.IndoorLocation.Trilateration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocationDebuggerActivity extends AppCompatActivity {

    String TAG = "ScanActivity";
    BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt mBluetoothGatt;
    BluetoothLeScanner scanner;
    ScanSettings scanSettings;

    private List<String> scannedDeivcesList;
    private ArrayAdapter<String> adapter;
    private TextView lat_val, lon_val;
    private double[] rssiArray = {0, 0, 0};
    private HashMap<String, ArrayList<Double>> coordinatesMap = new HashMap<String, ArrayList<Double>>();

    //DEFINE LAYOUT
    ListView devicesList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_debugger);

        ArrayList<Double> coordinates001 = new ArrayList<Double>() {{
            add(-19.6685);
            add(-69.1942);
        }};
        ArrayList<Double> coordinates002 = new ArrayList<Double>() {{
            add(-20.2705);
            add(-70.1311);
        }};
        ArrayList<Double> coordinates003 = new ArrayList<Double>() {{
            add(-20.5656);
            add(-70.1807);
        }};
        coordinatesMap.put("D3:FC:9B:90:18:13", coordinates001);
        coordinatesMap.put("ED:0F:E2:55:4F:F2", coordinates002);
        coordinatesMap.put("FB:A7:68:D0:2B:B1", coordinates003);

        devicesList = (ListView) findViewById(R.id.devicesList);

        //Setup list on device click listener
        setupListClickListener();

        //Initialize de devices list
        scannedDeivcesList = new ArrayList<>();

        //Initialize the list adapter for the listview with params: Context / Layout file / TextView ID in layout file / Devices list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, scannedDeivcesList);

        //Set the adapter to the listview
        devicesList.setAdapter(adapter);

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        init Bluetooth adapter
        initBT();
        //Start scan of bluetooth devices
        startLeScan(true);
//
        lat_val = (TextView) findViewById(R.id.lat_val);
        lon_val = (TextView) findViewById(R.id.lon_val);

    }

    //Connection callback
    BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        //Device connected, start discovering services
                        Log.i(TAG, "DEVICE CONNECTED. DISCOVERING SERVICES...");
                        mBluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        //Device disconnected
                        Log.i(TAG, "DEVICE DISCONNECTED");
                    }
                }

                // On discover services method
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //Services discovered successfully. Start parsing services and characteristics
                        Log.i(TAG, "SERVICES DISCOVERED. PARSING...");
                        displayGattServices(gatt.getServices());
                    } else {
                        //Failed to discover services
                        Log.i(TAG, "FAILED TO DISCOVER SERVICES");
                    }
                }

                //When reading a characteristic, here you receive the task result and the value
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //READ WAS SUCCESSFUL
                        Log.i(TAG, "ON CHARACTERISTIC READ SUCCESSFUL");
                    } else {
                        Log.i(TAG, "ERROR READING CHARACTERISTIC");
                    }
                }

                //When writing, here you can check whether the task was completed successfully or not
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "ON CHARACTERISTIC WRITE SUCCESSFUL");
                    } else {
                        Log.i(TAG, "ERROR WRITING CHARACTERISTIC");
                    }
                }

                //In this method you can read the new values from a received notification
                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    Log.i(TAG, "NEW NOTIFICATION RECEIVED");
                }

                //RSSI values from the connection with the remote device are received here
                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    Log.i(TAG, "NEW RSSI VALUE RECEIVED");
                }
            };

    //Method which parses all services and characteristics from the GATT table.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        //Check if there is any gatt services. If not, return.
        if (gattServices == null) return;

        // Loop through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.i(TAG, "SERVICE FOUND: " + gattService.getUuid().toString());
            //Loop through available characteristics for each service
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                Log.i(TAG, "  CHAR. FOUND: " + gattCharacteristic.getUuid().toString());
            }
        }

        //****************************************
        // CONNECTION PROCESS FINISHED!
        //****************************************
        Log.i(TAG, "*************************************");
        Log.i(TAG, "CONNECTION COMPLETED SUCCESFULLY");
        Log.i(TAG, "*************************************");

    }

    private void startLeScan(boolean endis) {
        if (endis) {
            //********************
            //START THE BLE SCAN
            //********************
            //Scanning parameters FILTER / SETTINGS / RESULT CALLBACK. Filter are used to define a particular
            //device to scan for. The Callback is defined above as a method.
            scanner.startScan(null, scanSettings, mScanCallback);
        } else {
            //Stop scan
            scanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            //Here will be received all the detected BLE devices around. "result" contains the device
            //address and name as a BLEPeripheral, the advertising content as a ScanRecord, the Rx RSSI
            //and the timestamp when received. Type result.get... to see all the available methods you can call.

            //Convert advertising bytes to string for a easier parsing. GetBytes may return a NullPointerException. Treat it right(try/catch).
            String advertisingString = byteArrayToHex(result.getScanRecord().getBytes());
            //Print the advertising String in the LOG with other device info (ADDRESS - RSSI - ADVERTISING - NAME)
            Log.i(TAG, result.getDevice().getAddress() + " - RSSI: " + result.getRssi() + "\t - " + advertisingString + " - " + result.getDevice().getName());

            //Check if scanned device is already in the list by mac address
            boolean contains = false;
            for (int i = 0; i < scannedDeivcesList.size(); i++) {
                if (scannedDeivcesList.get(i).contains(result.getDevice().getAddress())) {
                    //Device already added
                    contains = true;
                    rssiArray[i] = getDistance(-57, result.getRssi());
//                    lat_val.setText(" " + getDistance(-57, result.getRssi()));
                    //Replace the device with updated values in that position
                    scannedDeivcesList.set(i, result.getRssi() + "  " + result.getDevice().getName() + "\n       (" + result.getDevice().getAddress() + ")");
                    coordinatesMap.get(result.getDevice().getAddress()).set(2, (getDistance(-57, result.getRssi())));
                    calculateLocation();
                    break;
                }
            }

            if (!contains) {
                //Scanned device not found in the list. NEW => add to list
                scannedDeivcesList.add(result.getRssi() + "  " + result.getDevice().getName() + "\n (" + result.getDevice().getAddress() + ")");
                coordinatesMap.get(result.getDevice().getAddress()).add(getDistance(-57, result.getRssi()));
            }

            //After modify the list, notify the adapter that changes have been made so it updates the UI.
            //UI changes must be done in the main thread

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });

        }
    };

    private void calculateLocation() {
        ArrayList<Double> p_1 = coordinatesMap.get("D3:FC:9B:90:18:13");
        ArrayList<Double> p_2 = coordinatesMap.get("ED:0F:E2:55:4F:F2");
        ArrayList<Double> p_3 = coordinatesMap.get("FB:A7:68:D0:2B:B1");
        Point p1 = new Point(p_1.get(0), p_1.get(1), p_1.get(2));
        Point p2 = new Point(p_2.get(0), p_2.get(1), p_2.get(2));
        Point p3 = new Point(p_3.get(0), p_3.get(1), p_3.get(2));
        if (p1.gr() != 0 && p2.gr() != 0 && p3.gr() != 0) {
            double[] a = Trilateration.Compute(p1, p2, p3);
            if (a != null) {
                lat_val.setText(" " + a[0]);
                lon_val.setText(" " + a[1]);
            }

        }
//        ArrayList<Double> list = trilateration(rssiArray[0], rssiArray[1], rssiArray[2], -19.6685, -69.1942,
//                20.2705, -70.1311,
//                -20.5656, -70.1807 );
//        lat_val.setText(" "+list.get(0));
//        lon_val.setText(" "+list.get(1));

//        lon_val.setText("Lon: "+rssiArray[1]);
//        if(rssiArray[0]>0 && rssiArray[0]>0 && rssiArray[0]>0) {
//
//        }
    }

    private double getDistance(double txPower, double rssi) {
        if (rssi == 0) {
            return -1; // if we cannot determine accuracy, return -1.
        }
        double ratio = rssi * 1 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    //Method to convert a byte array to a HEX. string.
    private String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private void initBT() {
        final BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //Create the scan settings
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        //Set scan latency mode. Lower latency, faster device detection/more battery and resources consumption
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        //Wrap settings together and save on a settings var (declared globally).
        scanSettings = scanSettingsBuilder.build();
        //Get the BLE scanner from the BT adapter (var declared globally)
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    void setupListClickListener() {
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Stop the scan
                Log.i(TAG, "SCAN STOPED");
                scanner.stopScan(mScanCallback);

                //Get the string from the item clicked
                String fullString = scannedDeivcesList.get(position);
                //Get only the address from the previous string. Substring from '(' to ')'
                String address = fullString.substring(fullString.indexOf("(") + 1, fullString.indexOf(")"));
                //Get BLE device with address
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                //******************************
                //START CONNECTION WITH DEVICE AND DECLARE GATT
                //******************************
                Log.i(TAG, "*************************************************");
                Log.i(TAG, "CONNECTION STARTED TO DEVICE " + address);
                Log.i(TAG, "*************************************************");

                //ConnectGatt parameters are CONTEXT / AUTOCONNECT to connect the next time it is scanned / GATT CALLBACK to receive GATT notifications and data
                // Note: On Samsung devices, the connection must be done on main thread
                mBluetoothGatt = device.connectGatt(LocationDebuggerActivity.this, false, mGattCallback);

                /*
                There is also another simplest way to connect to a device. If you already stored
                the device in a list (List<BluetoothDevice>) you can retrieve it directly and
                connect to it:

                mBluetoothGatt = mList.get(position).connectGatt(MainActivity.this, false, mGattCallback);
                 */
            }
        });
    }
}