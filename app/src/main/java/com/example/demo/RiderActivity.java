/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demo;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * This shows how to listen to some {@link GoogleMap} events.
 */
public class RiderActivity extends AppCompatActivity
        implements OnMapClickListener,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    String userID;
    private GoogleMap mMap;
    private Marker mPos;
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;



    private Location mLastLocation;
    private boolean driverFound = false;
    private boolean isLoggingOut = false;

    private String driverID;
    private Button points;

    private Button mLogout;
    private Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_demo);


        button = (Button) findViewById(R.id.button);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        userID =  FirebaseAuth.getInstance().getCurrentUser().getUid();

        points = findViewById(R.id.button3);

        searchPoint();
        mLogout = (Button)findViewById(R.id.button4);


        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;

                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(RiderActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }

        });
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {

                                mLastLocation = location;
                            }
                        }
                    });
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setOnMapClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();

        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onMapClick(final LatLng point) {


        mMap.clear();

        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title("Destination")

                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_destination))
                .infoWindowAnchor(0.5f, 0.5f));
        button.setText("Make a Request");

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                button.setText("Looking for Driver");
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("ridingRequest");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {

                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    } });




                Map hash = new HashMap();
                hash.put("Destination", Arrays.asList(point.latitude, point.longitude));
                //pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                //pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                ref.child(userID).updateChildren(hash);

                ref.child(userID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child("DriverID").exists()) {
                            if (driverFound) {
                                return;
                            }


                            driverFound = true;
                            driverID = (String) dataSnapshot.child("DriverID").getValue();
                            Toast.makeText(getApplicationContext(), driverID, Toast.LENGTH_LONG).show();


                            getDriverLocation();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });


    }
    Marker mDriverMarker;

    private void searchPoint() {



        DatabaseReference pointRef = FirebaseDatabase.getInstance().getReference().child("Users").child(userID).child("Points");
        pointRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {


                    Long value = (Long) dataSnapshot.getValue();

                    points.setText("p: " + String.valueOf(value));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getDriverLocation(){
        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance().getReference().child("workers").child(driverID).child("l");
        driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLng);
                    if(mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(mLastLocation.getLatitude());
                    loc1.setLongitude(mLastLocation.getLongitude());

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<1000){
                        button.setText("Driver's Here");
                        Toast.makeText(getApplicationContext(),"Driver's Here", Toast.LENGTH_LONG).show();
                    }else{
                        button.setText("Driver Found: " + String.valueOf(distance));
                    }



                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car1)));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }




    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
