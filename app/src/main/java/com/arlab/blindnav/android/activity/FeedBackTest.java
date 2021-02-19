package com.arlab.blindnav.android.activity;

import androidx.appcompat.app.AppCompatActivity;
import com.arlab.blindnav.R;
import com.google.android.material.snackbar.Snackbar;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FeedBackTest extends AppCompatActivity {
    private OkHttpClient client = new OkHttpClient();
    private Button leftBtn, rightBtn, slightRightBtn, frontBtn, slightLeftBtn, allBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_back_test);
        leftBtn = (Button) findViewById(R.id.leftbtn);
        rightBtn = (Button) findViewById(R.id.rightbtn);
        slightLeftBtn = (Button) findViewById(R.id.sleftbtn);
        slightRightBtn = (Button) findViewById(R.id.srightbtn);
        frontBtn = (Button) findViewById(R.id.frontbtn);
        allBtn = (Button) findViewById(R.id.allbtn);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        leftBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generateFeedback(v, "left");
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generateFeedback(v, "right");
            }
        });

        slightLeftBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generateFeedback(v, "sleft");
            }
        });

        slightRightBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generateFeedback(v, "sright");
            }
        });

        frontBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generateFeedback(v, "front");
            }
        });

        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generateFeedback(v, "all");
            }
        });
    }

    public void generateFeedback(View v, String command) {
        String response;
        try {
            response = initiateFeedBack(command);
            Snackbar.make(v, response, Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            Snackbar.make(v, "Connection Lost", Snackbar.LENGTH_LONG).show();
        }
    }

    public String initiateFeedBack(String command) throws IOException {
        String url = "http://192.168.1.52/directions?direction=";
        url += command;
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}