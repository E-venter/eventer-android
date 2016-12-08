package com.example.gabriel.testgooglemaps;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

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

    public static final String URL_MAIN = "http://172.26.184.65:3000/";
    public static final String URL_TO_GET_EVENTS = URL_MAIN + "events/around/";
    public static final String URL_TO_CHECK_IN = URL_MAIN + "checkin/";
    public static final String URL_TO_NEW_EVENT = URL_MAIN + "sample/events/";
    public static final String URL_TO_LOGIN = URL_MAIN + "api/auth/sign_in/";
    public static final String URL_TO_SIGN_UP = URL_MAIN + "api/auth/";



    static final int REQUEST_TAKE_PHOTO  = 1;
    static final int REQUEST_LOGIN  = 2;

    public String client = null, uid = null, token = null;

    public String selectedEventId = "";

    public ArrayList<Event> eventList = new ArrayList<>();

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

        if(!getCredentialsFromFile()) {
            requestLogin();
        }
    }

    public void requestLogin(){
        startActivityForResult(
            new Intent(this, LoginActivity.class),
            REQUEST_LOGIN);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }



    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case 0:
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
                else {

                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0x0);
            //mMap.setMyLocationEnabled(true);
        }else {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraMoveCanceledListener(this);

        GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                String floatFormat = "%1$.3f";
                String s = "Marker in " + String.format(Locale.US, floatFormat, latLng.latitude) + ", " + String.format(Locale.US, floatFormat, latLng.longitude);
                EventMarker em = new EventMarker(s, latLng, new Date(), new Date(), mMap);

                String eventString = new Gson().toJson(em.event);
                ArrayList<NameValuePair> nvps = new ArrayList<>();
                nvps.add(new NameValuePair("info", eventString));

                System.out.println(eventString);

                try {
                    simpleParalelPost(new URL(URL_TO_NEW_EVENT), nvps);
                } catch (MalformedURLException e) {}
            }
        };
        mMap.setOnMapClickListener(onMapClickListener);

        GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.getTag() != null && marker.getTag() instanceof EventMarker) {
                    System.out.println(((EventMarker) marker.getTag()).event.name);

                    selectedEventId = ((EventMarker) marker.getTag()).event.id + "";

                    dispatchTakePictureIntent();
                }

                return false;
            }
        };
        mMap.setOnMarkerClickListener(onMarkerClickListener);
    }

    public void simpleParalelPost(final URL url, final ArrayList<NameValuePair> values){
        AsyncTask<Integer, Integer, Integer> execute = new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Integer... params) {
                post(url, values);

                return null;
            }

            protected void onPostExecute(Integer result) {

            }
        };

        execute.execute();
    }

    public HttpPost post(URL url, ArrayList<NameValuePair> values) {
        try {

            HttpPost httpPost = new HttpPost(url, getToken(), getClient(), getUid());

            for (NameValuePair value : values) {
                httpPost.add(value);
            }

            httpPost.send();

            return httpPost;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public HttpPost post(URL url, ArrayList<NameValuePair> values, byte[] image, String imageName) {
        try {

            HttpPost httpPost = new HttpPost(url, getToken(), getClient(), getUid());

            for (NameValuePair value : values) {
                httpPost.add(value);
            }

            httpPost.image = image;
            httpPost.imageName = imageName;

            httpPost.send();

            return httpPost;
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

    public HttpPost getVisibleEvents(URL url, LatLngBounds latLngBounds, Location currentLocation) {
        LatLng llCurrentLocation;
        if(currentLocation == null){
            llCurrentLocation = new LatLng(0, 0);
        }else {
            llCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        String input = "{" +
                "\"current_location\":"  + getJsonFromLatLng(llCurrentLocation) + "," +
                "\"corner_ne\":"  + getJsonFromLatLng(latLngBounds.northeast) + "," +
                "\"corner_sw\":"  + getJsonFromLatLng(latLngBounds.southwest) +
                "}";

        ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new NameValuePair("info", input));

        HttpPost post = post(url, nvps);

        return post;
    }

    public String getClient(){
        return client;//"USER_ID";
    }

    public String getToken(){
        return token;//"Auth";
    }

    public String getUid(){
        return uid;//"email";
    }

    public void updateEventsWithinBounds(final LatLngBounds latLngBounds, final Location currentLocation){
        //communicate with database passing latLngBounds.northeast and southwest
        //remove previous events, add new events

        AsyncTask<Integer, Integer, HttpPost> execute = new AsyncTask<Integer, Integer, HttpPost>() {
            EventList el;

            @Override
            protected HttpPost doInBackground(Integer... params) {
                System.out.println("gonna ask");

                try {
                    return getVisibleEvents(new URL(URL_TO_GET_EVENTS), latLngBounds, currentLocation);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                System.out.println("asked");

                return null;
            }

            protected void onPostExecute(HttpPost result) {
                if(result == null) return;

                String postContent = result.response;

                if(result.responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("START " + postContent + " END");
                    if (postContent != null) {
                        el = new Gson().fromJson(postContent, EventList.class);

                        //mMap.clear();
                        if (el != null && el.list != null) {
                            for (Event e : el.list) {
                                System.out.println(e.toString());

                                if(!eventList.contains(e)){
                                    new EventMarker(e, mMap);
                                    eventList.add(e);
                                }
                            }
                            System.out.println(eventList.size());
                        }
                    } else {
                        return;
                    }
                }else if(result.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                    requestLogin();
                    return;
                }else{
                    System.out.println("ERROR: " + result.responseCode);
                    Toast.makeText(getApplicationContext(), "Error on Server Connection!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        execute.execute();
    }

    public void getCheckedInAtEvent(String eventId, String url, final LatLng currentLocation) {

        AsyncTask<String, Integer, HttpPost> execute = new AsyncTask<String, Integer, HttpPost>() {
            CheckInError cie = null;

            @Override
            protected HttpPost doInBackground(String... params) {
                System.out.println("gonna send image");

                String eventId = params[0];
                String url = params[1];

                if(imageToSendToCheckIn == null){
                    cie = new CheckInError(CheckInError.ERROR_NO_PHOTO, "No photo selected for Check In");
                    return null;
                }

                ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new NameValuePair("event_id", eventId));
                nvps.add(new NameValuePair("location", getJsonFromLatLng(currentLocation)));
                try {
                    return post(new URL(url), nvps, imageToSendToCheckIn, imageToSendToCheckInName);
                }catch(Exception e){

                }

                return null;
            }

            protected void onPostExecute(HttpPost result) {
                String postContent;

                HttpPost post = result;
                postContent = post.response;

                if(post.responseCode == HttpURLConnection.HTTP_OK){
                    System.out.println("START " + postContent + " END");
                    if(postContent != null){
                        cie =  new CheckInError(CheckInError.ERROR_SUCCESS, postContent);
                    }else{
                        cie =  new CheckInError(CheckInError.ERROR_CONNECTION_FAILED, "Connection Timed Out");
                    }
                    System.out.println("sent image");
                }else if(post.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                    requestLogin();
                }else{
                    Toast.makeText(getApplicationContext(), "Error on Server Connection!", Toast.LENGTH_LONG).show();
                }
            }
        };

        execute.execute(eventId, url);

    }

    public static class CheckInError{
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
    public static final String TEMP_IMAGE_FILE_NAME = "temp";

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

                Bitmap photo = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                System.out.println(imageFile.getAbsolutePath());
                System.out.println(imageFile.exists());

                if(photo != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    imageToSendToCheckIn = stream.toByteArray();
                    imageToSendToCheckInName = MapsActivity.TEMP_IMAGE_FILE_NAME + ".jpg";
                    imageToSendToCheckInName = MapsActivity.TEMP_IMAGE_FILE_NAME + ".jpg";


                    LatLng llCurrentLocation;
                    if (mLastLocation == null) {
                        llCurrentLocation = new LatLng(0, 0);
                    } else {
                        llCurrentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    }

                    getCheckedInAtEvent(selectedEventId, URL_TO_CHECK_IN, llCurrentLocation);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(requestCode == REQUEST_LOGIN){
            if(resultCode == RESULT_OK){
                client = data.getStringExtra("client");
                uid = data.getStringExtra("uid");
                token = data.getStringExtra("auth");

                storeLoginCredentials();
            }else{
                startActivityForResult(
                    new Intent(this, LoginActivity.class),
                    REQUEST_LOGIN);
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

    private File getLoginCredentialsFile(){
        File path = new File(getFilesDir(), "login_credentials");
        path.mkdirs();
        File newFile = new File(path, "credentials.dat");

        return newFile;
    }

    private void storeLoginCredentials(){
        File newFile = getLoginCredentialsFile();

        try(PrintWriter writer = new PrintWriter(newFile, "UTF-8")) {
            writer.println(uid);
            writer.println(token);
            writer.println(client);
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean getCredentialsFromFile(){
        File newFile = getLoginCredentialsFile();

        try {
            Scanner scanner = new Scanner(newFile);
            uid = scanner.nextLine();
            token = scanner.nextLine();
            client = scanner.nextLine();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
