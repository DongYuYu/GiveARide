package com.example.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DriverRiderActivity extends AppCompatActivity {

    private Button mDriverButton, mRiderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_rider_login);

        mDriverButton = (Button)findViewById(R.id.driverButton);
        mRiderButton = (Button)findViewById(R.id.riderButton);


        mDriverButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(DriverRiderActivity.this, DriverActivity.class);
                startActivity(intent);
            }
        });

        mRiderButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(DriverRiderActivity.this, RiderActivity.class);
                startActivity(intent);
            }
        });
    }
}
