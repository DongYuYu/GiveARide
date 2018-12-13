package com.example.demo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/*
Use the package of jd-alexender to draw route


 */
public class DriverActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, GoogleMap.OnMarkerClickListener, RoutingListener {


    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private boolean isLoggingOut = false;
    private boolean working = false;
    private boolean driverFound = false;
    private int radius = 10000;
    private String userID;
    private Button points;
    private Button mLogout;

    int status = 0;
    private Button mRideStatus;
    private List<Marker> markers = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        userID =  FirebaseAuth.getInstance().getCurrentUser().getUid();
        points = findViewById(R.id.button3);
        searchPoint();
        mLogout = (Button)findViewById(R.id.button4);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }

        });
        mRideStatus = (Button)findViewById(R.id.button2);
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(status){
                    case 1:
                        status=2;
                        erasePolylines();
                        if(previous != null){

                            getRouteToMarker(previous.getPosition());

                        }
                        mRideStatus.setText("drive completed");

                        break;
                    case 2:
                        erasePolylines();
                        endRide();
                        earn();
                        break;
                }

            }
        });
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {

                                mLastLocation = location;
                                getClosestRequest();
                            }
                        }
                    });
        }
    }

    GeoQuery geoQuery;
    private boolean first = true;
    private void getClosestRequest() {


        DatabaseReference ridingRequest = FirebaseDatabase.getInstance().getReference().child("ridingRequest");
        GeoFire geoFire = new GeoFire(ridingRequest);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {

                if(driverFound) {
                    return;
                }
                driverFound = true;


                Marker temp = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.latitude, location.longitude))
                        .title(key)
                        .snippet("")
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup))
                        .infoWindowAnchor(0.5f, 0.5f));
                markers.add(temp);

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {



                    radius++;
                    getClosestRequest();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void earn() {


        final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {


                    Long driverValue = (Long) dataSnapshot.child(userID).child("Points").getValue();
                    usersRef.child(userID).child("Points").setValue(driverValue + 10);
                    points.setText("p: " + String.valueOf(driverValue + 10));

                    Long riderValue = (Long) dataSnapshot.child(customerID).child("Points").getValue();
                    usersRef.child(customerID).child("Points").setValue(riderValue - 10);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

//data



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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();

        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {


        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (first == true) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

            first = false;
        }

        if (working == true && status == 1 && click != null) {
            getRouteToMarker(click.getPosition());

            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference().child("workers");

            GeoFire driver = new GeoFire(refWorking);
            driver.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {

                @Override
                public void onComplete(String key, DatabaseError error) {

                }
            });

        }
        if (working == true && status == 2 && previous != null) {
            getRouteToMarker(previous.getPosition());

            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference().child("workers");

            int i =23;
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    Marker click;
    Marker previous;

    String customerID = "";
    @Override
    public boolean onMarkerClick(final Marker marker) {

        if (previous != null && previous.getTitle().equals("Destination")) {
            previous.remove();
        }

        click = marker;

        DatabaseReference request = FirebaseDatabase.getInstance().getReference().child("ridingRequest").child(marker.getTitle()).child("Destination");

        request.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                List<Double> loc = (List<Double>) dataSnapshot.getValue();
                if (loc != null) {
                    previous = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(loc.get(0), loc.get(1)))
                            .title("Destination")
                            .snippet(" ")
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_destination))
                            .infoWindowAnchor(0.5f, 0.5f));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Code
            }
        });

        Button button = findViewById(R.id.button);

        button.setText("give a ride");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DatabaseReference ridingRequest = FirebaseDatabase.getInstance().getReference().child("ridingRequest").child(marker.getTitle());   //

                customerID = marker.getTitle();
                Map hash = new HashMap();
                hash.put("DriverID", userID);

                ridingRequest.updateChildren(hash);
                getRouteToMarker(click.getPosition());


                hide();
                click.setVisible(true);

                status = 1;
                working = true;
            }
        });

        return false;
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .key(getString(R.string.google_key))
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }

    private List<Polyline> polylines = new ArrayList();
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);


        }

    }

    private void hide() {
        for (Marker r: markers) r.setVisible(false);
    }
    @Override
    public void onRoutingCancelled() {
    }

    private void erasePolylines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }

    private void disconnectDriver() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        //String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");

        //GeoFire geoFire = new GeoFire(ref);
        //geoFire.removeLocation(userId);
    }

    private void endRide() {
        mRideStatus.setText("picked customer");
        erasePolylines();
        working = false;
        //String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ridingRequest").child(customerID);
        ref.removeValue();
        //GeoFire geoFire = new GeoFire(ref);
        //geoFire.removeLocation(customerID);
        erasePolylines();
        click.remove();
        previous.remove();

    }
}
