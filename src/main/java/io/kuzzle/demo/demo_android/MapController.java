package io.kuzzle.demo.demo_android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.kuzzle.demo.demo_android.enums.UserType;

public class MapController {

  private GoogleMap mapView;
  private final Context ctx;
  private static MapController singleton = null;
  private Handler handler;
  private View  notificationHeader;
  private View  finishRideHeader;
  private TextView  notificationHeaderText;

  private BlinkingMarker  currentRideProposal = null;

  private Map<String, BlinkingMarker> markerList = new HashMap<>();

  public static MapController getSingleton(final Context ctx) {
    if (singleton == null) {
      singleton = new MapController(ctx);
    }
    return singleton;
  }

  private MapController(final Context ctx) {
    this.ctx = ctx;
  }

  public MapController addNewMarker(final LatLng location, final String userId, UserType type) {
    if (this.mapView == null)
      throw new RuntimeException("The MapView cannot be null");
    // Create new marker on the map

    final Bitmap bitmap;
    if (type == UserType.CAB) {
      bitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.taxi);
    } else {
      bitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.customer);
    }

    handler.post(new Runnable() {
      @Override
      public void run() {
        BlinkingMarker marker = new BlinkingMarker(bitmap, mapView);
        marker.setSnippet(userId);
        marker.addToMap(location);
        markerList.put(userId, marker);
      }
    });
    return this;
  }

  public MapController moveMarker(String userId, UserType type, double lat, double lon) {
    final BlinkingMarker marker = markerList.get(userId);
    final LatLng pos = new LatLng(lat, lon);
    if (marker == null) {
      // New user
      addNewMarker(pos, userId, type);
    } else {
      // Move user if location changed
      handler.post(new Runnable() {
        @Override
        public void run() {
          marker.moveMarker(pos);
          marker.show();
          if (marker.hasInfoWindow()) {
            marker.showInfoWindow();
          }
        }
      });
    }
    return this;
  }

  public MapController onRideProposal(final JSONObject rideProposal, final UserType userType) throws JSONException {
    JSONObject source = rideProposal.getJSONObject("_source");
    final BlinkingMarker marker = markerList.get(source.getString("from"));
    if (marker != null) {
      currentRideProposal = marker;
      makeMarkerBlink(marker);
      if (notificationHeader != null) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            if (userType == UserType.CAB)
              notificationHeaderText.setText(ctx.getResources().getString(R.string.taxi_to_customer_text));
            else
              notificationHeaderText.setText(ctx.getResources().getString(R.string.customer_to_taxi_text));
            notificationHeader.setVisibility(View.VISIBLE);
          }
        });
      }
    }
    return this;
  }

  private void  makeMarkerBlink(final BlinkingMarker marker) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        marker.startBlinking();
      }
    });
  }

  public void  makeMarkerBlink(final String id) {
    final BlinkingMarker marker = markerList.get(id);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (marker != null)
          marker.startBlinking();
      }
    });
  }

  public void  makeMarkerStopBlinking() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (currentRideProposal != null)
          currentRideProposal.stopBlinking();
      }
    });
  }

  public void  makeMarkerStopBlinking(final String id) {
    final BlinkingMarker marker = markerList.get(id);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (marker != null)
          marker.stopBlinking();
      }
    });
  }

  public MapController removeCandidate(final String userId) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        BlinkingMarker marker = markerList.get(userId);
        if (marker != null) {
          marker.stopBlinking().removeMarker();
          markerList.remove(userId);
        }
      }
    });
    return this;
  }

  public MapController  hideCandidate(final String userId) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        BlinkingMarker marker = markerList.get(userId);
        if (marker != null) {
          if (!marker.isInfoWindowShown()) {
            marker.hasInfoWindow(false);
          }
          marker.hide();
        }
      }
    });
    return this;
  }

  public void hasInfoWindow(final String userId, boolean has) {
    BlinkingMarker marker = markerList.get(userId);
    if (marker != null) {
      marker.hasInfoWindow(has);
    }
  }

  public void setMapView(GoogleMap mv) {
    this.mapView = mv;
  }

  public void setHandler(Handler handler) {
    this.handler = handler;
  }

  public void setNotificationHeader(View notificationHeader) {
    this.notificationHeader = notificationHeader;
  }

  public void setNotificationHeaderText(TextView notificationHeaderText) {
    this.notificationHeaderText = notificationHeaderText;
  }

  public void onRideRefused(final String candidateId) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        notificationHeader.setVisibility(View.GONE);
        if (markerList.get(candidateId) != null) {
          markerList.get(candidateId).hideInfoWindow();
        }
      }
    });
  }

  public void onRideAccepted() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        notificationHeader.setVisibility(View.GONE);
        finishRideHeader.setVisibility(View.VISIBLE);
      }
    });
  }

  public MapController  onRideFinished(final String candidateId) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (markerList.get(candidateId) != null)
          markerList.get(candidateId).stopBlinking();
        finishRideHeader.setVisibility(View.GONE);
      }
    });
    return this;
  }

  public void onMapClick() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        for (BlinkingMarker m : markerList.values()) {
          m.hasInfoWindow(false);
        }
      }
    });
  }

  public MapController  onRideCancelled(final String candidateId) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (markerList.get(candidateId) != null)
          markerList.get(candidateId).stopBlinking();
      }
    });
    return this;
  }

  public MapController  clearMap() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        markerList.clear();
      }
    });
    return this;
  }

  public void setFinishRideHeader(View finishRideHeader) {
    this.finishRideHeader = finishRideHeader;
  }

}
