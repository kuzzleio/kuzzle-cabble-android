package io.kuzzle.demo.demo_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import io.kuzzle.demo.demo_android.enums.UserType;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.cab_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        intent.putExtra("type", UserType.CAB);
        intent.putExtra("kuzzle_host", ((TextView) findViewById(R.id.kuzzle_host)).getText());
        startActivity(intent);
      }
    });
    findViewById(R.id.traveler_btn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        intent.putExtra("type", UserType.CUSTOMER);
        intent.putExtra("kuzzle_host", ((TextView) findViewById(R.id.kuzzle_host)).getText());
        startActivity(intent);
      }
    });
  }
}