package io.kuzzle.demo.demo_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.kuzzle.demo.demo_android.enums.UserType;

public class MainActivity extends AppCompatActivity {

  private String host;
  private SharedPreferences sharedPref;
  private final int REQUEST_CODE = 0x42;
  private final int ERROR_CODE = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    sharedPref = this.getSharedPreferences("cabble", Context.MODE_PRIVATE);
    host = sharedPref.getString("host", "http://192.168.1.34:7512");

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