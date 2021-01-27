package com.arlab.blindnav.android.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.arlab.blindnav.R;
import com.arlab.blindnav.android.util.ArchitectJavaScriptListener;
import com.arlab.blindnav.android.util.CameraConfig;
import com.arlab.blindnav.android.util.IndoorLocation.Point;
import com.arlab.blindnav.android.util.IndoorLocation.Trilateration;
import com.arlab.blindnav.android.util.location.BaseLocationStrategy;
import com.arlab.blindnav.android.util.location.LocationChangesListener;
import com.arlab.blindnav.android.util.location.LocationUtils;
import com.arlab.blindnav.data.DataProvider;
import com.indooratlas.android.sdk._internal.i3;
import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArActivity extends AppCompatActivity{

  private static final String TAG = ArActivity.class.getSimpleName();
  private static final String AR_EXPERIENCE = "index.html";
  private static final double ALTITUDE_CONST = 0.0;
  protected ArchitectView architectView;
  private BaseLocationStrategy baseLocationStrategy;
  private ArchitectJavaScriptListener javaScriptListener;

// BLE location
  String TAG_1 = "ScanActivity";
  BluetoothAdapter mBluetoothAdapter;
  BluetoothGatt mBluetoothGatt;
  BluetoothLeScanner scanner;
  ScanSettings scanSettings;
  private List<String> scannedDevicesList;
  private ArrayAdapter<String> adapter;
  private TextView lat_val, lon_val;
  private double[] rssiArray = {0, 0, 0};
  private HashMap<String, ArrayList<Double>> coordinatesMap = new HashMap<String, ArrayList<Double>>();

  /**
   * The ArchitectView.SensorAccuracyChangeListener notifies of changes in the accuracy of the compass.
   * This can be used to notify the user that the sensors need to be recalibrated.
   * This listener has to be registered after onCreate and unregistered before onDestroy in the ArchitectView.
   */
  private final ArchitectView.SensorAccuracyChangeListener sensorAccuracyChangeListener = new ArchitectView.SensorAccuracyChangeListener() {
    @Override
    public void onCompassAccuracyChanged(int accuracy) {
      if(accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) { // UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3
        Toast.makeText(ArActivity.this, R.string.compass_accuracy_low, Toast.LENGTH_LONG).show();
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
    scannedDevicesList = new ArrayList<>();

    WebView.setWebContentsDebuggingEnabled(true);
//    setupLocation();

    final CameraConfig cameraConfig = new CameraConfig();
    final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration();
    config.setLicenseKey(getString(R.string.wikitude_license));
    config.setCameraPosition(cameraConfig.getCameraPosition());
    config.setCameraResolution(cameraConfig.getCameraResolution());
    config.setCameraFocusMode(cameraConfig.getCameraFocusMode());
    config.setCamera2Enabled(cameraConfig.isCamera2Enabled());

    architectView = new ArchitectView(this);
    architectView.onCreate(config);
    setContentView(architectView);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    javaScriptListener = new ArchitectJavaScriptListener(this, architectView);
    javaScriptListener.onCreate();

    //init Bluetooth adapter
    initBT();
    //Start scan of bluetooth devices
    startLeScan(true);
  }

  private void initBT() {
    final BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();
    ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
    scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
    scanSettings = scanSettingsBuilder.build();
    scanner = mBluetoothAdapter.getBluetoothLeScanner();
  }

  private void startLeScan(boolean endis) {
    if (endis) {
      scanner.startScan(null, scanSettings, mScanCallback);
    } else {
      scanner.stopScan(mScanCallback);
    }
  }

  private ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      super.onScanResult(callbackType, result);
      String advertisingString = byteArrayToHex(result.getScanRecord().getBytes());
//      Log.i(TAG, result.getDevice().getAddress() + " - RSSI: " + result.getRssi() + "\t - " + advertisingString + " - " + result.getDevice().getName());

      boolean contains = false;
      for (int i = 0; i < scannedDevicesList.size(); i++) {
        if (scannedDevicesList.get(i).contains(result.getDevice().getAddress())) {
          contains = true;
          rssiArray[i] = getDistance(-57, result.getRssi());
          scannedDevicesList.set(i, result.getRssi() + "  " + result.getDevice().getName() + "\n       (" + result.getDevice().getAddress() + ")");
          coordinatesMap.get(result.getDevice().getAddress()).set(2, (getDistance(-57, result.getRssi())));
          calculateLocation();
          break;
        }
      }

      if (!contains) {
        scannedDevicesList.add(result.getRssi() + "  " + result.getDevice().getName() + "\n (" + result.getDevice().getAddress() + ")");
        coordinatesMap.get(result.getDevice().getAddress()).add(getDistance(-57, result.getRssi()));
      }
//      runOnUiThread(new Runnable() {
//        @Override
//        public void run() {
//          adapter.notifyDataSetChanged();
//        }
//      });
    }
  };

  private void calculateLocation() {
    if (coordinatesMap.size() >= 3) {
      ArrayList<Double> p_1 = coordinatesMap.get("D3:FC:9B:90:18:13");
      ArrayList<Double> p_2 = coordinatesMap.get("ED:0F:E2:55:4F:F2");
      ArrayList<Double> p_3 = coordinatesMap.get("FB:A7:68:D0:2B:B1");

      Point p1 = new Point(p_1.get(0), p_1.get(1), p_1.get(2));
      Point p2 = new Point(p_2.get(0), p_2.get(1), p_2.get(2));
      Point p3 = new Point(p_3.get(0), p_3.get(1), p_3.get(2));
      if (p1.gr() != 0 && p2.gr() != 0 && p3.gr() != 0) {
        double[] a = Trilateration.Compute(p1, p2, p3);
        architectView.setLocation(a[0], a[1], ALTITUDE_CONST, 100);
      }
    }
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

  private String byteArrayToHex(byte[] a) {
    StringBuilder sb = new StringBuilder(a.length * 2);
    for (byte b : a)
      sb.append(String.format("%02x", b & 0xff));
    return sb.toString();
  }

  private void setupLocation() {
    if(baseLocationStrategy != null) {
      baseLocationStrategy.stopListeningForLocationChanges();
    }
    baseLocationStrategy = LocationUtils.getLocationStatergy(this.getApplicationContext());
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    architectView.onPostCreate();

    try {
      architectView.load(AR_EXPERIENCE);
    } catch(IOException e) {
      Toast.makeText(this, getString(R.string.error_loading_ar_experience), Toast.LENGTH_SHORT).show();
      Log.e(TAG, "Exception while loading arExperience " + AR_EXPERIENCE + ".", e);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    architectView.onResume();
//    baseLocationStrategy.startListeningForLocationChanges(this);
//    architectView.registerSensorAccuracyChangeListener(sensorAccuracyChangeListener);
    architectView.callJavascript("World.onOfferDetailScreenDestroyed()");
  }

  @Override
  protected void onPause() {
    architectView.onPause();
//    baseLocationStrategy.stopListeningForLocationChanges();
    architectView.unregisterSensorAccuracyChangeListener(sensorAccuracyChangeListener);
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    javaScriptListener.onDestroy();
    architectView.clearCache();
    architectView.onDestroy();
    super.onDestroy();
  }

//  @Override
//  public void onLocationChanged(Location location) {
//    DataProvider.setUserLocation(location);
//    float accuracy = location.hasAccuracy() ? location.getAccuracy() : 200;
//    Log.e("Location update", String.format("Location[ %.6f, %.6f ]", location.getLatitude(), location.getLongitude()));
////    architectView.setLocation(-19.6685, 60.1942, ALTITUDE_CONST, accuracy);
//  }
//
//  /**
//   * implement these methods in production app
//   */
//  @Override
//  public void onLocationProviderEnabled(String provider) {
//  }
//
//  @Override
//  public void onLocationProviderStatusChanged(String provider, int status) {
//
//  }
//
//  @Override
//  public void onLocationProviderDisabled(String provider) {
//  }
}
