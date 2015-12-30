package io.kuzzle.demo.demo_android;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BlinkingMarker extends android.support.v4.app.FragmentActivity {
  private final static String TAG = BlinkingMarker.class.getSimpleName();

  private static int DEFAULT_FPS = 10;
  private static int DEFAULT_FREQUENCY_MILLIS = 2000;

  private float currentAlpha = 1;
  private boolean alphaAsc = true;

  // Dependencies
  private GoogleMap mMap;

  // State
  private int mFps;

  private Bitmap mOriginalBitmap;
  private Marker mMarker;
  private Handler mUiHandler;

  private LatLng mNewPosition;

  private String  snippet;

  /**
   * <p>Constructor for a blinking marker, with default frequency and fps.</p>
   *
   * @param bitmap - the bitmap for the marker
   * @param map    - the GoogleMap instance to which the marker is attached
   */
  public BlinkingMarker(Bitmap bitmap, GoogleMap map) {
    this(bitmap, map, DEFAULT_FPS, DEFAULT_FREQUENCY_MILLIS);
  }

  /**
   * <p>Constructor for a blinking marker, with a custom frequency and fps.</p>
   *
   * @param bitmap            - the bitmap for the marker
   * @param map               - the GoogleMap instance to which the marker is attached
   * @param fps               - the fps of the blinking
   * @param frequencyInMillis - the frequency of the blinking in milliseconds
   */
  public BlinkingMarker(Bitmap bitmap, GoogleMap map, int fps, int frequencyInMillis) {
    mMap = map;
    mOriginalBitmap = bitmap;
    mFps = fps;
  }

  /**
   * <p>Add the marker to the Map. Adding a blinking marker means adding
   * several markers with different opacity to the map. At every time only
   * one marker is visible to the user.</p>
   * <p>Note! Have to be called from the UI thread.</p>
   *
   * @param position - the position of the marker
   * @throws IllegalStateException - if it isn't called form the UI thread
   */
  public void addToMap(LatLng position) throws IllegalStateException {
    checkIfUiThread();
    if (mMarker != null) {
      Log.w(TAG, "Marker was already added.");
      return;
    }

    mMarker = addMarker(adjustOpacity(mOriginalBitmap, 255), position);
  }

  /**
   * <p>Removes the marker from the map. It could free up a lot of
   * memory, so use this when you don't need the marker anymore.</p>
   * <p>Note! Have to be called from the UI thread.</p>
   *
   * @throws IllegalStateException - if it isn't called form the UI thread
   */
  public BlinkingMarker removeMarker() throws IllegalStateException {
    checkIfUiThread();
    if (mUiHandler != null) {
      stopBlinking();
    }
    mMarker.remove();
    return this;
  }

  /**
   * <p>Starts the blinking of the marker. Don't forget to stop it
   * if your activity goes to the background.</p>
   *
   * @throws IllegalStateException - if it isn't called form the UI thread
   */
  public BlinkingMarker startBlinking() throws IllegalStateException {
    checkIfUiThread();
    if (mUiHandler != null) {
      Log.w(TAG, "Marker was already added.");
      return this;
    }
    mUiHandler = new Handler();
    mUiHandler.post(mBlinkerRunnable);
    return this;
  }

  /**
   * <p>Stops the blinking of the marker. You sould call this method
   * at least on the onPause() of the Activity.</p>
   *
   * @throws IllegalStateException - if it isn't called form the UI thread
   */
  public BlinkingMarker stopBlinking() throws IllegalStateException {
    checkIfUiThread();
    if (mUiHandler == null) {
      return this;
    }
    currentAlpha = 1;
    mMarker.setAlpha(currentAlpha);
    mUiHandler.removeCallbacks(mBlinkerRunnable);
    mUiHandler = null;
    return this;
  }

  private Runnable mBlinkerRunnable = new Runnable() {
    @Override
    public void run() {
      if (currentAlpha <= 0 || currentAlpha >= 1)
        alphaAsc = !alphaAsc;
      if (alphaAsc)
        currentAlpha += .1;
      else
        currentAlpha -= .1;
      mMarker.setAlpha(currentAlpha);

      final LatLng newPosition = mNewPosition;
      if (newPosition != null) {
        moveMarker(newPosition);
        mNewPosition = null;
      }
      mUiHandler.postDelayed(mBlinkerRunnable, 1000 / mFps);
    }
  };

  public void moveMarker(final LatLng newPosition) {
    mNewPosition = newPosition;
    mMarker.setPosition(newPosition);
  }

  private Marker addMarker(Bitmap bitmap, LatLng position) {
    MarkerOptions markerOptions = new MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromBitmap(bitmap));
    Marker marker = mMap.addMarker(markerOptions);
    marker.setVisible(true);
    marker.setSnippet(snippet);
    return marker;
  }

  private Bitmap adjustOpacity(Bitmap bitmap, int opacity) {
    Bitmap mutableBitmap = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
    Canvas canvas = new Canvas(mutableBitmap);
    int colour = (opacity & 0xFF) << 24;
    canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
    return mutableBitmap;
  }

  private void checkIfUiThread() throws IllegalStateException {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      throw new IllegalStateException("This call has to be made from the UI thread.");
    }
  }

  public BlinkingMarker hideInfoWindow() {
    this.mMarker.hideInfoWindow();
    return this;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }
}