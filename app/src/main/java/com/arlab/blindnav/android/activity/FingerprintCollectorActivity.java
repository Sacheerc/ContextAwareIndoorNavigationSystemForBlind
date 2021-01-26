package com.arlab.blindnav.android.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.arlab.blindnav.R;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class FingerprintCollectorActivity extends AppCompatActivity implements SensorEventListener {
    private Button button, collectBtn, saveBtn;
    public static DecimalFormat DECIMAL_FORMATTER;
    private SensorManager sensorManager;
    private Context mContext;
    private TextView value, stepCountTextView, x_value, y_value, z_value, fingerprintCountView;
    int stepDetector, fingerprintCount;
    double magnitudeGlobe;
    ArrayList<String> fingerprintArray = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint_collector);
        final TextView textView = findViewById(R.id.text_fingerprint);
        textView.setText("Fingerprint Collector");
        button = (Button) findViewById(R.id.button);
        collectBtn = (Button) findViewById(R.id.collect_btn);
        saveBtn = (Button) findViewById(R.id.saveBtn);

        value = (TextView) findViewById(R.id.value);
        x_value = (TextView) findViewById(R.id.x_value);
        y_value = (TextView) findViewById(R.id.y_value);
        z_value = (TextView) findViewById(R.id.z_value);
        fingerprintCountView = (TextView) findViewById(R.id.fingerprintCount);
//        stepCountTextView = (TextView) root.findViewById(R.id.stepCountVal);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                x_value.setText(DECIMAL_FORMATTER.format(0));
                y_value.setText(DECIMAL_FORMATTER.format(0));
                z_value.setText(DECIMAL_FORMATTER.format(0));
                value.setText(DECIMAL_FORMATTER.format(0) + " \u00B5Tesla");
                fingerprintCount = 0;
                magnitudeGlobe = 0;
                fingerprintArray.clear();
                fingerprintCountView.setText(String.valueOf(fingerprintCount));
                Snackbar.make(v, "All the values are initialized to 0", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        collectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fingerprintCount += 1;
                fingerprintArray.add(String.valueOf(magnitudeGlobe));
                fingerprintCountView.setText(String.valueOf(fingerprintCount));
                Snackbar.make(v, "collected", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    File path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    File myFile = new File(path, "fingerprints.txt");
                    FileOutputStream fOut = new FileOutputStream(myFile,true);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    for (String s : fingerprintArray) {
                        myOutWriter.append(s+ "\n");
                    }
                    myOutWriter.close();
                    fOut.close();
                    fingerprintArray.clear();

                    Snackbar.make(v, "Fingerprints are saved successfully", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                catch (java.io.IOException e) {

                    //do something if an IOException occurs.
                    Snackbar.make(v, "Fingerprints failed", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
        });

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER = new DecimalFormat("#.000", symbols);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // get values for each axes X,Y,Z
            float magX = event.values[0];
            float magY = event.values[1];
            float magZ = event.values[2];
            double magnitude = Math.sqrt((magX * magX) + (magY * magY) + (magZ * magZ));
            magnitudeGlobe = magnitude;
            // set value on the screen
            x_value.setText(DECIMAL_FORMATTER.format(magX));
            y_value.setText(DECIMAL_FORMATTER.format(magY));
            z_value.setText(DECIMAL_FORMATTER.format(magZ));
            value.setText(DECIMAL_FORMATTER.format(magnitude) + " \u00B5Tesla");
//            fingerprintCount += 1;
//            fingerprintArray.add(String.valueOf(magnitudeGlobe));
//            fingerprintCountView.setText(String.valueOf(fingerprintCount));
        }else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
//            stepDetector = (int) (stepDetector+event.values[0]);
//            stepCountTextView.setText(String.valueOf(stepDetector));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}