package com.arlab.blindnav.android.util.IndoorLocation;

public class LocationUtils {

    //  get distance between the device and BLE beacon
    private static double getDistance(double txPower, double rssi) {
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
}
