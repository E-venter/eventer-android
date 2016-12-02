package com.example.gabriel.testgooglemaps;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;

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
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.RandomAccess;

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

    private static final String URL_MAIN = "http://172.28.146.124:3000/";
    private static final String URL_TO_GET_EVENTS = URL_MAIN + "sample/events/";
    private static final String URL_TO_CHECK_IN = URL_MAIN + "sample/checkin/";

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
            //requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0x0);
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
                    System.out.println(((EventMarker) marker.getTag()).event.name);
                }

                dispatchTakePictureIntent();

                return false;
            }
        };
        mMap.setOnMarkerClickListener(onMarkerClickListener);
    }

    public class NameValuePair{
        String name;
        String value;
/*
        public NameValuePair(String name, byte[] value){
            this.name = name;
            this.value = value;
        }*/

        public NameValuePair(String name, String value){
            this.name = name;
            this.value = value;
        }
    }

    public String post(URL url, ArrayList<NameValuePair> values) {
        try {

            HttpPost httpPost = new HttpPost();
            httpPost.setTarget(url);

            for (NameValuePair value : values) {
                httpPost.add(value.name, value.value);
            }

            return httpPost.send();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String post(URL url, ArrayList<NameValuePair> values, byte[] image, String imageName) {
        try {

            HttpPost httpPost = new HttpPost();
            httpPost.setTarget(url);

            for (NameValuePair value : values) {
                httpPost.add(value.name, value.value);
            }

            httpPost.image = image;
            httpPost.imageName = imageName;

            return httpPost.send();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        updateEventsWithinBounds(bounds, mLastLocation);
        System.out.println("idle");
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

    private static String getJsonFromLocation(Location location){
        return "{\"latitude\":\""  + location.getLatitude() + "\"," +
                "\"longitude\":\"" + location.getLongitude() + "\"" + "}";
    }

    public EventList getVisibleEvents(URL url, String userId, LatLngBounds latLngBounds, Location currentLocation) {
        LatLng llCurrentLocation;
        if(currentLocation == null){
            llCurrentLocation = new LatLng(0, 0);
        }else {
            llCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        String input = "{\"userId\":\"" + userId + "\"," +
                "\"currentLocation\":"  + getJsonFromLatLng(llCurrentLocation) + "," +
                "\"cornerNE\":"  + getJsonFromLatLng(latLngBounds.northeast) + "," +
                "\"cornerSW\":"  + getJsonFromLatLng(latLngBounds.southwest) +
                "}";

        ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new NameValuePair("info", input));

        String postContent = post(url, nvps);
        System.out.println("START " + postContent + " END");
        if(postContent != null){
            return new Gson().fromJson(postContent, EventList.class);
        }else{
            return null;
        }
    }

    public String getUserID(){
        return "USER_ID";
    }

    public void updateEventsWithinBounds(final LatLngBounds latLngBounds, final Location currentLocation){
        //communicate with database passing latLngBounds.northeast and southwest
        //remove previous events, add new events

        AsyncTask<Integer, Integer, Integer> execute = new AsyncTask<Integer, Integer, Integer>() {
            EventList el;

            @Override
            protected Integer doInBackground(Integer... params) {
                System.out.println("gonna ask");

                try {
                    el = getVisibleEvents(new URL(URL_TO_GET_EVENTS), getUserID(), latLngBounds, currentLocation);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                System.out.println("asked");

                return null;
            }

            protected void onPostExecute(Integer result) {
                mMap.clear();
                if (el != null) {
                    for (Event e : el.list) {
                        System.out.println(e.toString());
                        new EventMarker(e, mMap);
                    }
                }
            }
        };

        execute.execute();
    }

    public void getCheckedInAtEvent(String eventId, String url) {

        AsyncTask<String, Integer, Integer> execute = new AsyncTask<String, Integer, Integer>() {
            CheckInError cie = null;

            @Override
            protected Integer doInBackground(String... params) {
                System.out.println("gonna send image");

                String eventId = params[0];
                String url = params[1];

                if(imageToSendToCheckIn == null){
                    cie = new CheckInError(CheckInError.ERROR_NO_PHOTO, "No photo selected for Check In");
                    return 0;
                }

                ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new NameValuePair("eventId", eventId));

                String postContent;
                try {
                    postContent = post(new URL(url), nvps, imageToSendToCheckIn, imageToSendToCheckInName);

                    System.out.println("START " + postContent + " END");
                    if(postContent != null){
                        cie =  new CheckInError(CheckInError.ERROR_SUCCESS, postContent);
                    }else{
                        cie =  new CheckInError(CheckInError.ERROR_CONNECTION_FAILED, "Connection Timed Out");
                    }

                    System.out.println("sent image");
                } catch (MalformedURLException e) {
                    System.out.println("NOT sent image");
                    e.printStackTrace();
                }
                return 0;
            }

            protected void onPostExecute(Integer result) {
                System.out.println(cie.msgError);
            }
        };

        execute.execute(eventId, url);

    }

    private static class CheckInError{
        static final int ERROR_SUCCESS = 0;
        static final int ERROR_CONNECTION_FAILED = 1;
        static final int ERROR_NO_PHOTO = 2;
        static final int ERROR_ERROR3 = 3;

        int codError;
        String msgError;

        CheckInError(){}

        CheckInError(int codError, String msgError){
            this.codError = codError;
            this.msgError = msgError;
        }
    }

    //To be used when we need to take the picture
    static final int REQUEST_TAKE_PHOTO  = 1;
    private final String TEMP_IMAGE_FILE_NAME = "temp";

    byte[] imageToSendToCheckIn = null;
    String imageToSendToCheckInName = null;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        try {
            createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = getImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                File imageFile = getImageFile();
                Bitmap photo = (Bitmap) data.getExtras().get("data");

                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(photo , 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                imageToSendToCheckIn = stream.toByteArray();
                imageToSendToCheckInName = TEMP_IMAGE_FILE_NAME + ".jpg";


                System.out.println("RESULT DA IMAGE");
                getCheckedInAtEvent("1001001", URL_TO_CHECK_IN);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private File createImageFile() throws IOException {
        File newFile = getImageFile();
        newFile.createNewFile();

        return newFile;
    }

    private File getImageFile() throws IOException {
        File imagePath = new File(getFilesDir(), "temp_images");
        imagePath.mkdirs();
        File newFile = new File(imagePath, "default_image.jpg");

        return newFile;
    }
}
