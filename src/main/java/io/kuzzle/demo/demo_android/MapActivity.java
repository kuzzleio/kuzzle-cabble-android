package io.kuzzle.demo.demo_android;

import android.app.NotificationManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.kuzzle.demo.demo_android.enums.RideAction;
import io.kuzzle.demo.demo_android.enums.Status;
import io.kuzzle.demo.demo_android.enums.UserType;
import io.kuzzle.sdk.core.Kuzzle;
import io.kuzzle.sdk.core.KuzzleDataCollection;
import io.kuzzle.sdk.core.KuzzleDataMapping;
import io.kuzzle.sdk.core.KuzzleDocument;
import io.kuzzle.sdk.core.KuzzleOptions;
import io.kuzzle.sdk.core.KuzzleRoom;
import io.kuzzle.sdk.core.KuzzleRoomOptions;
import io.kuzzle.sdk.enums.Mode;
import io.kuzzle.sdk.enums.Users;
import io.kuzzle.sdk.listeners.ResponseListener;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

  private Kuzzle kuzzle;
  private Handler handler = new Handler();
  private SupportMapFragment mapFragment;

  private String  kuzzle_host;
  private UserType userType;

  // Documents
  private KuzzleDocument self;
  private KuzzleDocument currentRide;

  // Collections
  private KuzzleDataCollection userCollection;
  private KuzzleDataCollection rideCollection;

  // Rooms
  private KuzzleRoom userRoom;
  private KuzzleRoom rideRoom;
  private KuzzleRoom usersScope;

  private JSONObject userSubscribeFilter;

  private Map<String, KuzzleDocument> userList = new HashMap<String, KuzzleDocument>();
  private Circle vicinity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.map_view);
    userType = (UserType) getIntent().getExtras().get("type");
    kuzzle_host = getIntent().getExtras().get("kuzzle_host").toString();
    findViewById(R.id.declineride).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        findViewById(R.id.notification).setVisibility(View.GONE);
        MapController.getSingleton(MapActivity.this).makeMarkerStopBlinking();
        try {
          manageRideProposal(RideAction.DECLINED);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    findViewById(R.id.acceptride).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          self.setContent("status", Status.RIDING);
          manageRideProposal(RideAction.ACCEPTED);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    findViewById(R.id.finishride).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          manageRideProposal(RideAction.FINISHED);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });

    if (userType == UserType.CAB) {
      ((Button) findViewById(R.id.availability)).setText(getResources().getString(R.string.look_for_customers));
    } else {
      ((Button) findViewById(R.id.availability)).setText(getResources().getString(R.string.look_for_cab));
    }
    findViewById(R.id.availability).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        changeUserStatus();
      }
    });
    MapController mapControllerInstance = MapController.getSingleton(this);
    mapControllerInstance.setFinishRideHeader(findViewById(R.id.inridepanel));
    mapControllerInstance.setNotificationHeader(findViewById(R.id.notification));
    mapControllerInstance.setNotificationHeaderText((TextView) findViewById(R.id.notification_text));

    this.mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }

  private void changeUserStatus() {
    Status userStatus = Status.valueOf(self.getContent("status").toString().toUpperCase());
    if (userType == UserType.CAB) {
      if (userStatus == Status.IDLE) {
        self.setContent("status", Status.TOHIRE.toString());
        ((Button) findViewById(R.id.availability)).setText(R.string.stop_looking_for_customers);
      } else if (userStatus == Status.TOHIRE) {
        self.setContent("status", Status.IDLE.toString());
        ((Button) findViewById(R.id.availability)).setText(R.string.look_for_customers);
      }
    } else if (userType == UserType.CUSTOMER) {
      if (userStatus == Status.IDLE) {
        self.setContent("status", Status.WANTTOHIRE.toString());
        ((Button) findViewById(R.id.availability)).setText(R.string.stop_looking_for_cab);
      } else if (userStatus == Status.WANTTOHIRE) {
        self.setContent("status", Status.IDLE.toString());
        ((Button) findViewById(R.id.availability)).setText(R.string.look_for_cab);
      }
    }
    self.save();
  }

  public void manageRideProposal(final RideAction action) throws JSONException {
    if (currentRide == null)
      return;
    if (!currentRide.getJSONObject("body").keys().hasNext() && !currentRide.isNull("_source")) {
      currentRide.put("body", currentRide.getJSONObject("_source"));
      currentRide.remove("_source");
    }
    currentRide.setContent("status", action.toString().toLowerCase());
    currentRide.save(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        switch (action) {
          case DECLINED:
            MapController.getSingleton(MapActivity.this.getBaseContext()).onRideRefused();
            currentRide.delete();
            break;
          case ACCEPTED:
            MapController.getSingleton(MapActivity.this.getBaseContext()).onRideAccepted(currentRide.getContent("from").toString());
            handler.post(new Runnable() {
              @Override
              public void run() {
                findViewById(R.id.gif).setVisibility(View.VISIBLE);
                findViewById(R.id.availability).setVisibility(View.INVISIBLE);
                ((WebView) findViewById(R.id.gif)).loadUrl("file:///android_asset/gif.html");
              }
            });
            break;
          case FINISHED:
            MapController.getSingleton(MapActivity.this.getBaseContext()).onRideFinished(currentRide.getContent("from").toString());
            handler.post(new Runnable() {
              @Override
              public void run() {
                findViewById(R.id.gif).setVisibility(View.GONE);
                findViewById(R.id.availability).setVisibility(View.VISIBLE);
              }
            });
            currentRide.delete();
            self.setContent("status", Status.IDLE);
            handler.post(new Runnable() {
              @Override
              public void run() {
                changeUserStatus();
              }
            });
            break;
          case CANCELLED:
            MapController.getSingleton(MapActivity.this.getBaseContext()).onRideCancelled(currentRide.getContent("from").toString());
            handler.post(new Runnable() {
              @Override
              public void run() {
                MapActivity.this.findViewById(R.id.notification).setVisibility(View.GONE);
              }
            });
            break;
        }
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble", error.toString());
      }
    });
  }

  private void manageResponseProposal(RideAction action, JSONObject proposal) {
    try {
      switch (action) {
        case DECLINED:
          MapController.getSingleton(MapActivity.this.getBaseContext()).onRideRefused();
          currentRide.delete();
          break;
        case ACCEPTED:
          MapController.getSingleton(MapActivity.this.getBaseContext()).onRideAccepted(proposal.getString("from"));
          handler.post(new Runnable() {
            @Override
            public void run() {
              findViewById(R.id.gif).setVisibility(View.VISIBLE);
              findViewById(R.id.availability).setVisibility(View.INVISIBLE);
              ((WebView) findViewById(R.id.gif)).loadUrl("file:///android_asset/gif.html");
            }
          });
          break;
        case FINISHED:
          MapController.getSingleton(MapActivity.this.getBaseContext()).onRideFinished(proposal.getString("from").toString());
          handler.post(new Runnable() {
            @Override
            public void run() {
              findViewById(R.id.gif).setVisibility(View.GONE);
              findViewById(R.id.availability).setVisibility(View.VISIBLE);
            }
          });
          currentRide.delete();
          self.setContent("status", Status.IDLE);
          break;
        case CANCELLED:
          MapController.getSingleton(MapActivity.this.getBaseContext()).onRideCancelled(proposal.getString("from").toString());
          handler.post(new Runnable() {
            @Override
            public void run() {
              MapActivity.this.findViewById(R.id.notification).setVisibility(View.GONE);
            }
          });
          break;
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }


  private void  publishRideProposal(RideAction action, String candidateId) throws JSONException {
    switch (action) {
      case ASK:
      case PROPOSE:
        currentRide.setContent("from", self.getId());
        currentRide.setContent("to", candidateId);
        currentRide.setContent("status", "awaiting");
        currentRide.save();
        break;
    }
  }

  private void connectToKuzzle() throws URISyntaxException {
    KuzzleOptions options = new KuzzleOptions();
    options.setAutoReconnect(true);
    options.setAutoReplay(true);
    options.setQueuable(true);
    options.setOfflineMode(Mode.AUTO);
    kuzzle = new Kuzzle(kuzzle_host, "cabble", options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        Log.e("cabble", "Connected to kuzzle");
        prepareMapping(new ResponseListener() {
          @Override
          public void onSuccess(JSONObject object) {
            createMySelf();
          }

          @Override
          public void onError(JSONObject error) {

          }
        });
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble", "Error during connection: " + error.toString());
      }
    });
  }

  private void mapGeoPoint() {
    KuzzleDataMapping mapping = new KuzzleDataMapping(userCollection);
    try {
      mapping.set("pos", new JSONObject().put("type", "geo_point"));
      mapping.apply();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void  prepareMapping(final ResponseListener listener) {
    kuzzle.listCollections(new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        userCollection = kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_users));
        rideCollection = kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_rides));
        listener.onSuccess(null);
      }

      @Override
      public void onError(JSONObject error) {
        JSONObject query = new JSONObject();
        try {
          query.put("controller", "admin").put("action", "createIndex").put("index", "cabble");
          kuzzle.query("", "admin", "createIndex", query, new ResponseListener() {
            @Override
            public void onSuccess(JSONObject object) {
              userCollection = kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_users));
              rideCollection = kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_rides));
              userCollection.create(new ResponseListener() {
                @Override
                public void onSuccess(JSONObject object) {
                  mapGeoPoint();
                  rideCollection.create(new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject object) {
                      listener.onSuccess(null);
                    }

                    @Override
                    public void onError(JSONObject error) {

                    }
                  });
                }

                @Override
                public void onError(JSONObject error) {

                }
              });
            }

            @Override
            public void onError(JSONObject error) {

            }
          });
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void createMySelf() {
    JSONObject pos = null;
    try {
      pos = new JSONObject()
          .put("lat", getResources().getString(R.string.default_lat))
          .put("lon", getResources().getString(R.string.default_lon));
    } catch (JSONException e) {
      e.printStackTrace();
    }
    self = new KuzzleDocument(userCollection);
    self.setContent("type", userType.toString().toLowerCase())
        .setContent("pos", pos)
        .setContent("status", Status.IDLE.toString())
        .setContent("sibling", "none");
    userCollection.createDocument(self, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        Log.e("cabble", "Created myself: " + self.toString());
        try {
          self.put("_id", object.getString("_id"));
          initUserSubscribeFilter();
          subscribeToUsersScope();
          subscribeToCollections();
          getConnectedUsers();
        } catch (JSONException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
      }
    });
  }

  private JSONObject  getGeoDistanceFilter() throws JSONException {
    JSONObject geo_distance = new JSONObject();
    JSONObject filter = new JSONObject();
    geo_distance.put("pos", self.getContent("pos"));
    geo_distance.put("distance", getResources().getString(R.string.distance_filter) + "m");
    filter.put("geo_distance", geo_distance);
    return filter;
  }

  private void initUserSubscribeFilter() throws JSONException {
    if (userSubscribeFilter == null) {
      userSubscribeFilter = new JSONObject();
      JSONArray and = new JSONArray();
      JSONArray types = new JSONArray();
      JSONArray status = new JSONArray();
      JSONObject statu = new JSONObject();
      JSONObject terms = new JSONObject();
      JSONObject terms2 = new JSONObject();
      JSONObject type = new JSONObject();
      types.put("cab");
      status.put("idle").put("toHire").put("riding");
      if (userType == UserType.CAB) {
        types.put("customer");
        status.put("wantToHire");
      } else {
        status.put("hired");
      }
      type.put("type", types);
      statu.put("status", status);
      terms.put("terms", statu);
      terms2.put("terms", type);
      and.put(terms).put(terms2);
      and.put(getGeoDistanceFilter());

      userSubscribeFilter.put("and", and);
    }
  }

  private JSONObject initRideSubscribeFilter() throws JSONException {
    JSONObject rideFilter = new JSONObject();
    JSONArray or = new JSONArray();
    JSONObject term = new JSONObject();
    JSONObject term2 = new JSONObject();
    JSONObject to = new JSONObject();
    JSONObject from = new JSONObject();
    to.put("to", self.getId());
    from.put("from", self.getId());
    term.put("term", to);
    term2.put("term", from);
    or.put(term).put(term2);
    rideFilter.put("or", or);
    return rideFilter;
  }

  private void subscribeToCollections() throws JSONException, IOException {
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setSubscribeToSelf(false);
    userRoom = userCollection.subscribe(userSubscribeFilter, options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        Log.e("cabble", "# user: " + object.toString());
        try {
          if (!object.getString("scope").equals("out")) {
            if (!object.getString("_id").equals(self.getId())) {
              JSONObject source = object.getJSONObject("_source");
              KuzzleDocument user = new KuzzleDocument(userCollection, object);
              user.put("body", object.getJSONObject("_source"));
              object.remove("_source");
              userList.put(object.getString("_id"), user);
              MapController.getSingleton(MapActivity.this).moveMarker(object.getString("_id"), UserType.valueOf(source.getString("type").toUpperCase()), source.getJSONObject("pos").getDouble("lat"), source.getJSONObject("pos").getDouble("lon"));
              if (Status.valueOf(user.getContent("status").toString().toUpperCase()) == Status.TOHIRE ||
                  Status.valueOf(user.getContent("status").toString().toUpperCase()) == Status.WANTTOHIRE) {
                MapController.getSingleton(MapActivity.this).makeMarkerBlink(object.getString("_id"));
              } else {
                MapController.getSingleton(MapActivity.this).makeMarkerStopBlinking(object.getString("_id"));
              }
            }
          } else {
            MapController.getSingleton(MapActivity.this.getBaseContext()).removeCandidate(object.getString("_id"));
            userList.remove(object.getString("_id"));
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble", "Error while subscribing to users " + error.toString());
      }
    });
    rideRoom = rideCollection.subscribe(initRideSubscribeFilter(), options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          currentRide = new KuzzleDocument(rideCollection, object);
          manageRideProposal();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble", "Error while subscribing to rides " + error.toString());
      }
    });
  }

  private void  pushNotification() {
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
              .setSmallIcon(R.drawable.favicon)
              .setContentText("Go to cabble to see.");
    if (userType == UserType.CUSTOMER)
      mBuilder.setContentTitle("A cab is ready to take you !");
    else
      mBuilder.setContentTitle("A customer wants a ride !");
    if (mBuilder != null) {
      mBuilder.setVibrate(new long[] {1000, 1000});
      Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      mBuilder.setSound(alarmSound);
      NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      mNotifyMgr.notify(42, mBuilder.build());
      mBuilder.build();
    }
  }

  private void manageRideProposal() throws IOException, JSONException {
    if (!currentRide.getString("action").equals("delete")) {
      final JSONObject source = currentRide.getJSONObject("_source");
      if (source.getString("from").equals(self.getId()) || !source.getString("status").equals("awaiting")) {
        handler.post(new Runnable() {
          @Override
          public void run() {
            try {
              manageResponseProposal(RideAction.valueOf(source.getString("status").toUpperCase()), source);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        });
      } else {
        MapController.getSingleton(this).onRideProposal(currentRide, userType);
        pushNotification();
      }
    }
  }

  private void subscribeToUsersScope() throws JSONException, IOException {
    KuzzleRoomOptions options = new KuzzleRoomOptions();
    options.setUsers(Users.OUT);
    JSONObject meta = new JSONObject();
    meta.put("_id", self.getString("_id"));
    options.setMetadata(meta);
    options.setSubscribeToSelf(false);
    usersScope = userCollection.subscribe(null, options, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        try {
          if (object.getString("action").equals("off")) {
            MapController.getSingleton(MapActivity.this.getBaseContext()).removeCandidate(object.getJSONObject("metadata").getString("_id"));
            userList.remove(object.getJSONObject("metadata").getString("_id"));
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble error", error.toString());
      }
    });
  }

  private void getConnectedUsers() throws JSONException, IOException {
    JSONObject filter = new JSONObject();
    JSONArray types = new JSONArray();
    types.put("cab");
    if (userType == UserType.CAB) {
      types.put("customer");
    }
    initUserSubscribeFilter();
    filter.put("filter", userSubscribeFilter);
    filter.put("query", new JSONObject().put("terms", new JSONObject().put("type", types)));
    kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_users)).advancedSearch(filter, new ResponseListener() {
      @Override
      public void onSuccess(JSONObject object) {
        Log.e("cabble## ", object.toString());
        try {
          for (int i = 0; i < object.getJSONArray("documents").length(); i++) {
            KuzzleDocument doc = new KuzzleDocument(userCollection, object.getJSONArray("documents").getJSONObject(i));
            userList.put(doc.getString("_id"), doc);
            updateUserPosition(object.getJSONArray("documents").getJSONObject(i));
          }
        } catch (JSONException e) {
          e.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onError(JSONObject error) {
        Log.e("cabble", error.toString());
      }
    });
  }

  private void updateUserPosition(final JSONObject object) throws Exception {
    JSONObject source = object.getJSONObject("body");
    JSONObject pos = source.getJSONObject("pos");
    MapController.getSingleton(this).moveMarker(object.getString("_id"), UserType.valueOf(source.getString("type").toUpperCase()), pos.getDouble("lat"), pos.getDouble("lon"));
  }

  private void updateMyPosition(LatLng location) throws JSONException, IOException {
    JSONObject pos = new JSONObject()
        .put("lat", location.latitude)
        .put("lon", location.longitude);
    if (self != null) {
      self.setContent("pos", pos);
      self.save();
      vicinity.setCenter(new LatLng(location.latitude, location.longitude));
    }
  }

  private void invalidate() throws JSONException, IOException {
    kuzzle.dataCollectionFactory(getResources().getString(R.string.cabble_collection_users)).deleteDocument(self.getString("_id"));
    MapController.getSingleton(this).clearMap();
    if (userRoom != null)
      userRoom.unsubscribe();
    if (rideRoom != null)
      rideRoom.unsubscribe();
    if (usersScope != null)
      usersScope.unsubscribe();
    kuzzle.logout();
  }

  @Override
  protected void onDestroy() {
    try {
      this.invalidate();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    super.onDestroy();
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    try {
      connectToKuzzle();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    googleMap.setMyLocationEnabled(true);
    googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
      @Override
      public void onMyLocationChange(Location location) {
        try {
          LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
          updateMyPosition(latLng);
        } catch (IOException e) {
          e.printStackTrace();
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    double lat = Double.parseDouble(MapActivity.this.getResources().getString(R.string.default_lat));
    double lon = Double.parseDouble(MapActivity.this.getResources().getString(R.string.default_lon));
    vicinity = googleMap.addCircle(new CircleOptions()
        .center(new LatLng(lat, lon))
        .radius(Double.parseDouble(MapActivity.this.getResources().getString(R.string.distance_filter))));

    MapController.getSingleton(this).setMapView(googleMap);
    MapController.getSingleton(this).setHandler(handler);
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(MapActivity.this.getResources().getString(R.string.default_lat)), Double.parseDouble(MapActivity.this.getResources().getString(R.string.default_lon))), 14));
    googleMap.getUiSettings().setZoomControlsEnabled(true);
    googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
      @Override
      public View getInfoWindow(Marker marker) {
        return null;
      }

      @Override
      public View getInfoContents(final Marker marker) {
        View v = null;
        String id = marker.getSnippet();
        try {
          if (Status.valueOf(userList.get(id).getContent("status").toString().toUpperCase()) == Status.IDLE ||
              UserType.valueOf(userList.get(id).getContent("type").toString().toUpperCase()) == UserType.valueOf(self.getContent("type").toString().toUpperCase())) {
            v = getLayoutInflater().inflate(R.layout.idle_bubble, null);
            ((TextView) v.findViewById(R.id.userid)).setText(MapActivity.this.userList.get(id).getString("_id"));
            ((TextView) v.findViewById(R.id.status)).setText(MapActivity.this.userList.get(id).getContent("status").toString());
          } else {
            if (userType != UserType.CAB) {
              v = getLayoutInflater().inflate(R.layout.taxi_bubble, null);
            } else {
              v = getLayoutInflater().inflate(R.layout.customer_bubble, null);
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
        return v;
      }
    });
    googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
      @Override
      public void onInfoWindowClick(Marker marker) {
        try {
          currentRide = new KuzzleDocument(rideCollection);
          publishRideProposal(RideAction.ASK, marker.getSnippet());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
