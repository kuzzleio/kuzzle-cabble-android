package io.kuzzle.demo.demo_android;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import io.kuzzle.demo.demo_android.enums.UserType;

public class MainActivity extends AppCompatActivity {

  private String host;
  private SharedPreferences sharedPref;
  private final int REQUEST_CODE = 0x42;
  private final int ERROR_CODE = -1;
  private final int PERMISSION_REQUEST = 0x43;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    sharedPref = this.getSharedPreferences("cabble", Context.MODE_PRIVATE);
    host = sharedPref.getString("host", "http://cabble.kuzzle.io:7512");
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) || !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
        showMessageOKCancel("You need to allow access to Location",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                requestPermissionsNeeded();
              }
            });
        return;
      }
      requestPermissionsNeeded();
    }
    // Listeners
    findViewById(R.id.cab_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        launchMapActivity(UserType.CAB);
      }
    });
    findViewById(R.id.traveler_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        launchMapActivity(UserType.CUSTOMER);
      }
    });
    ((TextView)findViewById(R.id.desc)).setMovementMethod(LinkMovementMethod.getInstance());
  }

  private void requestPermissionsNeeded() {
    ActivityCompat.requestPermissions(MainActivity.this,
        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
        PERMISSION_REQUEST);
  }

  private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
    new AlertDialog.Builder(MainActivity.this)
        .setMessage(message)
        .setPositiveButton("OK", okListener)
        .setNegativeButton("Cancel", null)
        .create()
        .show();
  }

  private void  launchMapActivity(UserType type) {
    Intent intent = new Intent(MainActivity.this, MapActivity.class);
    intent.putExtra("type", type);
    intent.putExtra("kuzzle_host", host);
    startActivityForResult(intent, 0x42);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE) {
      if (resultCode == ERROR_CODE) {
        Toast.makeText(MainActivity.this, "Error during connection to kuzzle", Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
    switch(requestCode) {
      case PERMISSION_REQUEST:
        Map<String, Integer> perms = new HashMap<String, Integer>();
        for (int i = 0; i < permissions.length; i++) {
          perms.put(permissions[i], grantResult[i]);
        }
        if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
          Toast.makeText(MainActivity.this, "Please allow location permission to be able to use cabble.", Toast.LENGTH_LONG).show();
          requestPermissionsNeeded();
        }
        break;
      default:
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean  onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.action_settings:

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(R.layout.settings);
        AlertDialog dialog = builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            host = ((TextView)((AlertDialog) dialog).findViewById(R.id.host)).getText().toString();
            sharedPref.edit().putString("host", host).commit();
          }
        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        })
          .create();
        dialog.show();
        ((TextView)dialog.findViewById(R.id.host)).setText(host);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}