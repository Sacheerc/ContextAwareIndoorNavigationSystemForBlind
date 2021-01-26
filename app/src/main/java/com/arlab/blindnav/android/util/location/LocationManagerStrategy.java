package com.arlab.blindnav.android.util.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationManagerStrategy implements BaseLocationStrategy, LocationListener {

  private Context mAppContext;
  private LocationManager mLocationManager;
  private Location mLastLocation;
  private LocationChangesListener mLocationListener;
  private boolean mUpdatePeriodically = false;
  private boolean isGPSEnabled = false;
  private boolean isNetworkEnabled = false;
  private static LocationManagerStrategy INSTANCE;
  private static long UPDATE_INTERVAL = 0; // milliseconds
  private static long FASTEST_INTERVAL = 0; // milliseconds
  private static long DISPLACEMENT = 0; // meters
  private static final String TAG = "LocationManagerStrategy";

  private LocationManagerStrategy(Context context) {
    this.mAppContext = context;
  }

  public static LocationManagerStrategy getInstance(Context context) {
    if(INSTANCE == null) {
      INSTANCE = new LocationManagerStrategy(context);
      INSTANCE.initLocationClient();
    }
    return INSTANCE;
  }

  @Override
  public void startListeningForLocationChanges(LocationChangesListener locationListener) {
    mLocationListener = locationListener;
    startLocationUpdates();
  }

  @Override
  public void stopListeningForLocationChanges() {
    try {
      mLocationManager.removeUpdates(this);
    } catch(SecurityException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void setPeriodicalUpdateEnabled(boolean enable) {
    mUpdatePeriodically = enable;
  }

  @Override
  public void setPeriodicalUpdateInterval(long time) {
    UPDATE_INTERVAL = time;
    if(time < FASTEST_INTERVAL) {
      FASTEST_INTERVAL = time / 2;
    }
  }

  @Override
  public void setPeriodicalUpdateFastestInterval(long time) {
    FASTEST_INTERVAL = time;
    if(time > UPDATE_INTERVAL) {
      UPDATE_INTERVAL = time * 2;
    }
  }

  @Override
  public void setDisplacement(long displacement) {
    DISPLACEMENT = displacement;
  }

  @Override
  public Location getLastLocation() {
    if(mLastLocation == null) {
      try {
        mLastLocation = mLocationManager.getLastKnownLocation(getBestProvider());
      } catch(SecurityException securityException) {
        return null;
      }
    }
    LocationUtils.LastKnownLocaiton = mLastLocation;
    return mLastLocation;
  }

  private String getBestProvider() {
    Criteria criteria = new Criteria();
    criteria.setAccuracy(Criteria.ACCURACY_HIGH);
    return mLocationManager.getBestProvider(criteria, false);
  }

  @Override
  public void initLocationClient() {
    mLocationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
  }

  @Override
  public void startLocationUpdates() {
    try {
      // getting GPS status
      isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
      // getting network status
      isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
      if(isNetworkEnabled) {
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, DISPLACEMENT, this);
      }
      if(isGPSEnabled) {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, DISPLACEMENT, this);
      }
    } catch(SecurityException ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    mLastLocation = location;
    if(mLastLocation != null && mLocationListener != null && LocationUtils.isBetterLocation(location)) {
      mLocationListener.onLocationChanged(location);
    }
    if(!mUpdatePeriodically) {
      this.stopListeningForLocationChanges();
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    if(mLocationListener != null) {
      mLocationListener.onLocationProviderStatusChanged(provider, status);
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if(mLocationListener != null) {
      mLocationListener.onLocationProviderEnabled(provider);
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    if(mLocationListener != null) {
      mLocationListener.onLocationProviderDisabled(provider);
    }
  }
}