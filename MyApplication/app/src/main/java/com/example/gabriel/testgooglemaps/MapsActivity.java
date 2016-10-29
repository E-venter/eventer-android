package com.example.gabriel.testgooglemaps;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.util.Locale;

public class MapsActivity extends FragmentActivity
    implements OnMapReadyCallback,
                GoogleApiClient.ConnectionCallbacks,
                GoogleApiClient.OnConnectionFailedListener,
                GoogleMap.OnCameraIdleListener,
                GoogleMap.OnCameraMoveStartedListener,
                GoogleMap.OnCameraMoveListener,
                GoogleMap.OnCameraMoveCanceledListener{

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;
    private boolean firstTimeGettingLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0x0);
            mMap.setMyLocationEnabled(true);
        }
        mMap.setMyLocationEnabled(true);

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraMoveCanceledListener(this);

        GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                String floatFormat = "%1$.3f";
                String s = "Marker in " + String.format(Locale.US, floatFormat, latLng.latitude) + ", " + String.format(Locale.US, floatFormat, latLng.longitude);
                new EventMarker(s, latLng, mMap);
            }
        };
        mMap.setOnMapClickListener(onMapClickListener);

        GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.getTag() != null && marker.getTag() instanceof EventMarker) {
                    System.out.println(((EventMarker) marker.getTag()).s);
                }

                return false;
            }
        };
        mMap.setOnMarkerClickListener(onMarkerClickListener);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null && firstTimeGettingLocation) {
            firstTimeGettingLocation = false;

            LatLng ll = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 17f));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onCameraIdle() {
        //fazer on Idle ou no moved? verificação de velocidade de internet? pegar apenas eventos da nova area?
        //no idea.
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        updateEventsWithinBounds(bounds, mLastLocation);
        System.out.println("idle");
    }

    public void updateEventsWithinBounds(LatLngBounds latLngBounds, Location currentLocation){
        //communicate with database passing latLngBounds.northeast and southwest
        //remove previous events, add new events
    }

    @Override
    public void onCameraMoveCanceled() {

    }

    @Override
    public void onCameraMove() {
        System.out.println("moved");
    }

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            System.out.println("The user gestured on the map.");
        } else if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION) {
            System.out.println("The user tapped something on the map.");
        } else if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
            System.out.println("The app moved the camera.");
        }
    }

    private static String getJsonFromLatLng(LatLng latLng){
        return "{\"latitude\":\""  + latLng.latitude + "\"," +
                "\"longitude\":\"" + latLng.longitude + "\"" + "}";
    }

    //Test method, not yet integrated
    public String doPost(String databaseAddress, LatLngBounds latLngBounds, Location currentLocation) {
        try {
            Client client = Client.create();

            WebResource webResource = client.resource(databaseAddress);

            int someVariable = 0;

            LatLng llCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            String input = "{\"currentLocation\":\""  + getJsonFromLatLng(llCurrentLocation) + "\"," +
                            "\"cornerNE\":\""  + getJsonFromLatLng(latLngBounds.northeast) + "\"," +
                            "\"cornerSW\":\""  + getJsonFromLatLng(latLngBounds.southwest) + "\"" +
                            "}";

            ClientResponse response = webResource.type("application/json")
                    .post(ClientResponse.class, input);

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            String output = response.getEntity(String.class);
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
