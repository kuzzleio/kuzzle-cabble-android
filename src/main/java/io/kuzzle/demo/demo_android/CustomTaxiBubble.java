package io.kuzzle.demo.demo_android;

import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by kblondel on 29/10/15.
 */
public class CustomTaxiBubble implements GoogleMap.InfoWindowAdapter {

  public CustomTaxiBubble(/*MapView mapView*/) {
    /*
    super(R.layout.taxi_bubble, mapView);
    mView.findViewById(R.id.bubble_need_ride).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.e("cabble", "Need ride");
      }
    });
    mView.findViewById(R.id.bubble_looking).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.e("cabble", "Looking for customer");
      }
    });
    */
  }

  @Override
  public View getInfoWindow(Marker marker) {
    return null;
  }

  @Override
  public View getInfoContents(Marker marker) {
    return null;
  }
}
