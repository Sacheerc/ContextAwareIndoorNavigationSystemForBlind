package com.arlab.blindnav.android.util.IndoorLocation;

import java.util.ArrayList;

public class LocationUtils {

    private static ArrayList<Double> getIndoorLocationCoordinates() {
        ArrayList<Double> locationCoordinates = new ArrayList<>();

        return locationCoordinates;
    }

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
