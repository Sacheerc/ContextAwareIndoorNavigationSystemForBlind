package com.arlab.blindnav.android.util;

import android.app.Activity;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import com.arlab.blindnav.R;
import com.arlab.blindnav.android.activity.FeedBackTest;
import com.arlab.blindnav.android.activity.FingerprintCollectorActivity;
import com.arlab.blindnav.android.activity.LocationDebuggerActivity;
import com.arlab.blindnav.android.activity.OfferDetailActivity;
import com.arlab.blindnav.android.activity.WayFindingActivity;
import com.arlab.blindnav.android.extension.ArchitectViewExtension;
import com.arlab.blindnav.data.DataProvider;
import com.arlab.blindnav.data.model.Offer;
import com.wikitude.architect.ArchitectJavaScriptInterfaceListener;
import com.wikitude.architect.ArchitectView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Locale;

public class ArchitectJavaScriptListener extends ArchitectViewExtension implements ArchitectJavaScriptInterfaceListener {
  TextToSpeech tts;
  public ArchitectJavaScriptListener(Activity activity, ArchitectView architectView) {
    super(activity, architectView);
  }

  @Override
  public void onCreate() {
    tts=new TextToSpeech(activity.getApplicationContext(), new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
          tts.setLanguage(Locale.UK);
        }
      }
    });
    architectView.addArchitectJavaScriptInterfaceListener(this);
  }

  @Override
  public void onDestroy() {
    architectView.removeArchitectJavaScriptInterfaceListener(this);
  }

  @Override
  public void onJSONObjectReceived(JSONObject jsonObject) {
    final Intent poiDetailIntent = new Intent(activity, OfferDetailActivity.class);
    try {
      switch(jsonObject.getString("action")) {
        case "present_poi_details": {
          poiDetailIntent.putExtra(OfferDetailActivity.EXTRAS_KEY_POI_ID, jsonObject.getString("id"));
          activity.startActivity(poiDetailIntent);
          break;
        }
        case "open_location_debugger": {
          Intent locationDebugger = new Intent(activity, LocationDebuggerActivity.class);
          activity.startActivity(locationDebugger);
          break;
        }
        case "open_fingerprint_collector": {
          Intent fingerprintCollector = new Intent(activity, FingerprintCollectorActivity.class);
          activity.startActivity(fingerprintCollector);
          break;
        }
        case "open_way_finder": {
          Intent wayFindingActivity = new Intent(activity, WayFindingActivity.class);
          activity.startActivity(wayFindingActivity);
          break;
        }
        case "open_feedback_test": {
          Intent feedbackTest = new Intent(activity, FeedBackTest.class);
          activity.startActivity(feedbackTest);
          break;
        }
        case "places_labels_get": {
          JSONObject obj = new JSONObject();
          JSONArray array = new JSONArray();
          for(Offer offer : new DataProvider(activity).getOffers()) {
            JSONObject item = new JSONObject();
            item.put("offerId", offer.getId());
            item.put("address", new DataProvider(activity).getOffersAddress(offer).getAddress());
            array.put(item);
          }
          obj.put("items", array);
          architectView.callJavascript("World.onPlacesAddressesReceived('" + obj + "')");
          break;
        }
        case "user_address_get": {
          JSONObject userItem = new JSONObject();
          userItem.put("address", new DataProvider(activity).getUserAddressLine());
          architectView.callJavascript("panelSetUserAddress('" + userItem + "')");
          break;
        }
        case "getContext": {
          JSONArray array = jsonObject.getJSONArray("context");
          tts.speak("These are the nearby landmarks. Please select one as the destination.", TextToSpeech.QUEUE_ADD, null);
          for (int i =0; i<array.length(); i++) {
            JSONObject object = array.getJSONObject(i).getJSONObject("poiData");
            String title =object.getString("title");
            tts.speak(title, TextToSpeech.QUEUE_ADD, null);
          }
//          Toast.makeText(activity, String.valueOf(len), Toast.LENGTH_LONG).show();
          break;
        }
        default: break;
      }
    } catch(JSONException e) {
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(activity, R.string.error_parsing_json, Toast.LENGTH_LONG).show();
        }
      });
      e.printStackTrace();
    }
  }
}
