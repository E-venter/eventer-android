package com.example.gabriel.testgooglemaps;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
                    System.out.println(((EventMarker) marker.getTag()).event.name);
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
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        updateEventsWithinBounds(bounds, mLastLocation);
        //System.out.println("idle");
    }

    @Override
    public void onCameraMoveCanceled() {

    }

    @Override
    public void onCameraMove() {
        //System.out.println("moved");
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

    public EventList getVisibleEvents(String databaseAddress, String userId, LatLngBounds latLngBounds, Location currentLocation) {
        try {
            Client client = Client.create();

            WebResource webResource = client.resource(databaseAddress);

            LatLng llCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            String input = "{\"userId\":\"" + userId + "\"," +
                            "\"currentLocation\":"  + getJsonFromLatLng(llCurrentLocation) + "," +
                            "\"cornerNE\":"  + getJsonFromLatLng(latLngBounds.northeast) + "," +
                            "\"cornerSW\":"  + getJsonFromLatLng(latLngBounds.southwest) +
                            "}";

            ClientResponse response = webResource.type("application/json")
                    .post(ClientResponse.class, input);

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }

            String output = response.getEntity(String.class);

            return new Gson().fromJson(output, EventList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUserID(){
        return "USER_ID";
    }

    public void updateEventsWithinBounds(LatLngBounds latLngBounds, Location currentLocation){
        //communicate with database passing latLngBounds.northeast and southwest
        //remove previous events, add new events
        mMap.clear();

        EventList el = getVisibleEvents("", getUserID(), latLngBounds, currentLocation);
        if(el != null){
            for(Event e : el.list){
                new EventMarker(e, mMap);
            }
        }
    }

    public CheckInError getCheckedInAtEvent(String eventId, String filePath, String urlString) {
        if(imageToSendToCheckIn == null){
            return new CheckInError(CheckInError.ERROR_NO_PHOTO, "No photo selected for Check In");
        }

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(urlString);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addTextBody("userId", getUserID());
        builder.addTextBody("eventId", eventId);
        builder.addTextBody("currentLocation", getJsonFromLocation(mLastLocation));
        builder.addPart("image", new FileBody(imageToSendToCheckIn));

        post.setEntity(builder.build());
        try {
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            CheckInError checkInResult = new Gson().fromJson(getHttpResponseContent(response), CheckInError.class);

            EntityUtils.consume(entity);
            client.close();

            return checkInResult;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CheckInError(CheckInError.ERROR_CONNECTION_FAILED, "Connection Timed Out");
    }

    private static class CheckInError{
        public static final int ERROR_SUCCESS = 0;
        public static final int ERROR_CONNECTION_FAILED = 1;
        public static final int ERROR_NO_PHOTO = 2;
        public static final int ERROR_ERROR3 = 3;

        public int codError;
        public String msgError;

        public CheckInError(){}

        public CheckInError(int codError, String msgError){
            this.codError = codError;
            this.msgError = msgError;
        }
    }

    private String getHttpResponseContent(HttpResponse response) {
        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String body;
            String content = "";

            while ((body = rd.readLine()) != null)
            {
                content += body;
            }
            return content.trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    //To be used when we need to take the picture
    static final int REQUEST_TAKE_PHOTO  = 1;
    static File imageToSendToCheckIn = null;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            String imagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            imagePath = imagePath.endsWith("/") ? imagePath : imagePath + "/";

            imageToSendToCheckIn = new File(imagePath + TEMP_IMAGE_FILE_NAME);
        }
    }

    private final String TEMP_IMAGE_FILE_NAME = "temp";
    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = TEMP_IMAGE_FILE_NAME;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }
}
