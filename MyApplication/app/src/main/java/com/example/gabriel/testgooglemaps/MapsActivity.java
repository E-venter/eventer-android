package com.example.gabriel.testgooglemaps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;
    private boolean firstTimeGettingLocation = true;

    public static final String URL_MAIN = "http://logica.dyndns.info:25566/";
    public static final String URL_TO_GET_EVENTS = URL_MAIN + "events/around/";
    public static final String URL_TO_CHECK_IN = URL_MAIN + "checkin/";
    public static final String URL_TO_NEW_EVENT = URL_MAIN + "events/";
    public static final String URL_TO_LOGIN = URL_MAIN + "api/auth/sign_in/";
    public static final String URL_TO_SIGN_UP = URL_MAIN + "api/auth/";

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_LOGIN = 2;
    static final int REQUEST_PERMISSIONS = 3;
    static final int REQUEST_CHECK_IN = 4;
    static final int REQUEST_MENU = 5;

    public String client = null, uid = null, token = null;

    public String selectedEventId = "";

    public Map<Integer, EventMarker> eventList = new HashMap<>();

    SupportMapFragment mapFragment;

    Marker newEventMarker = null;
    Circle newEventCircle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (!getCredentialsFromFile()) {
            requestLogin();
        }

        ImageButton imMenu = (ImageButton) findViewById(R.id.menu);
        imMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MapsActivity.this, NavegationActivity.class), REQUEST_MENU);
            }
        });

    }

    public void requestLogin() {
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


    private void displayPopupWindow(View anchorView, LatLng latLng) {
        final PopupWindow popup = new PopupWindow(MapsActivity.this);
        final View layout = getLayoutInflater().inflate(R.layout.popup_content, null);
        popup.setContentView(layout);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);

        // Set content width and height
        popup.setHeight(size.y);
        popup.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        // Closes the popup window when touch outside of it - when looses focus
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);

        // Show anchored to button
        popup.setBackgroundDrawable(new BitmapDrawable());
        //popup.showAsDropDown(anchorView);

        popup.showAtLocation(layout, Gravity.BOTTOM, 0, 0);

        newEventMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        newEventCircle = mMap.addCircle(
                new CircleOptions().center(latLng)
                        .radius(5)
                        .strokeWidth(5f)
                        .fillColor(0x503FB6BC)
        );

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latLng.latitude - 0.001, latLng.longitude), 17f));

        PopupWindow.OnDismissListener onDismissListener = new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                newEventMarker.remove();
                newEventMarker = null;

                newEventCircle.remove();
                newEventCircle = null;
            }
        };

        popup.setOnDismissListener(onDismissListener);

        ImageButton cancelar = (ImageButton) layout.findViewById(R.id.cancelarB);
        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.dismiss();
            }
        });

        ImageButton salvar = (ImageButton) layout.findViewById(R.id.salvarB);
        salvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String name = ((EditText) layout.findViewById(R.id.Nome)).getText().toString();
                    String address = ((EditText) layout.findViewById(R.id.endereco)).getText().toString();
                    String latitude = newEventMarker.getPosition().latitude + "";
                    String longitude = newEventMarker.getPosition().longitude + "";
                    String description = ((EditText) layout.findViewById(R.id.descricaoEvento)).getText().toString();

                    DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);

                    String start_time = format.parse(((EditText) layout.findViewById(R.id.horaIni)).getText().toString()).getTime() + "";//((EditText)layout.findViewById(R.id.datainicio)).getText().toString();
                    String end_time = format.parse(((EditText) layout.findViewById(R.id.editText)).getText().toString()).getTime() + "";//((EditText)layout.findViewById(R.id.dataTermino)).getText().toString();
                    String radius = newEventCircle.getRadius() + "";
                    String event_type = ((CheckBox) layout.findViewById(R.id.checkBox2)).isChecked() ? "private" : "public";

                    ArrayList<NameValuePair> nvps = new ArrayList<>();
                    nvps.add(new NameValuePair("name", name));
                    nvps.add(new NameValuePair("address", address));
                    nvps.add(new NameValuePair("latitude", latitude));
                    nvps.add(new NameValuePair("longitude", longitude));
                    nvps.add(new NameValuePair("description", description));
                    nvps.add(new NameValuePair("start_time", start_time));
                    nvps.add(new NameValuePair("end_time", end_time));
                    nvps.add(new NameValuePair("radius", radius));
                    nvps.add(new NameValuePair("event_type", event_type));

                    try {
                        simpleParalelPost(new URL(URL_TO_NEW_EVENT), nvps);
                        popup.dismiss();

                        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                        updateEventsWithinBounds(bounds, mLastLocation);
                    } catch (Exception e) {
                    }
                } catch (ParseException e) {
                    Toast.makeText(getBaseContext(), "Data Invalida!", Toast.LENGTH_LONG);
                }
            }
        });

        SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seekBar2);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newEventCircle.setRadius(5 + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                } else {

                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
            //mMap.setMyLocationEnabled(true);
        } else {
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraMoveCanceledListener(this);

        GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

            }
        };
        mMap.setOnMapClickListener(onMapClickListener);

        GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.getTag() != null && marker.getTag() instanceof EventMarker) {
                    System.out.println(((EventMarker) marker.getTag()).event.name);
                    selectedEventId = ((EventMarker) marker.getTag()).event.id + "";
                    Event eve = ((EventMarker) marker.getTag()).event;
                    //dispatchTakePictureIntent();

                    Intent intent = new Intent(MapsActivity.this, ShoweventActivity.class);
                    intent.putExtra("event_id",selectedEventId);
                    intent.putExtra("latitude",mLastLocation.getLatitude());
                    intent.putExtra("longitude",mLastLocation.getLongitude());

                    intent.putExtra("client", getClient());
                    intent.putExtra("uid", getUid());
                    intent.putExtra("token", getToken());

                    intent.putExtra("name", eve.name);
                    intent.putExtra("address", eve.address);
                    intent.putExtra("start_time", eve.start_time);
                    intent.putExtra("end_time", eve.end_time);
                    intent.putExtra("description", eve.description);

                    startActivityForResult(intent, REQUEST_CHECK_IN);
                }

                return false;
            }
        };
        mMap.setOnMarkerClickListener(onMarkerClickListener);

        GoogleMap.OnMapLongClickListener onMapLongClickListener = new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng latLng) {
                System.out.println("long click");
                displayPopupWindow(mapFragment.getView(), latLng);
            }
        };

        mMap.setOnMapLongClickListener(onMapLongClickListener);

    }

    public void simpleParalelPost(final URL url, final ArrayList<NameValuePair> values) {
        AsyncTask<Integer, Integer, HttpPost> execute = new AsyncTask<Integer, Integer, HttpPost>() {
            @Override
            protected HttpPost doInBackground(Integer... params) {
                return post(url, values);
            }

            protected void onPostExecute(HttpPost result) {

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

    private static String getJsonFromLatLng(LatLng latLng) {
        return "{\"latitude\":\"" + latLng.latitude + "\"," +
                "\"longitude\":\"" + latLng.longitude + "\"" + "}";
    }

    private static String getJsonFromLocation(Location location) {
        return "{\"latitude\":\"" + location.getLatitude() + "\"," +
                "\"longitude\":\"" + location.getLongitude() + "\"" + "}";
    }

    public HttpPost getVisibleEvents(URL url, LatLngBounds latLngBounds, Location currentLocation) {
        LatLng llCurrentLocation;
        if (currentLocation == null) {
            llCurrentLocation = new LatLng(0, 0);
        } else {
            llCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }

        ArrayList<NameValuePair> nvps = new ArrayList<>();

        nvps.add(new NameValuePair("current_location", getJsonFromLatLng(llCurrentLocation)));
        nvps.add(new NameValuePair("corner_ne", getJsonFromLatLng(latLngBounds.northeast)));
        nvps.add(new NameValuePair("corner_sw", getJsonFromLatLng(latLngBounds.southwest)));

        HttpPost post = post(url, nvps);

        return post;
    }

    public String getClient() {
        return client;//"USER_ID";
    }

    public String getToken() {
        return token;//"Auth";
    }

    public String getUid() {
        return uid;//"email";
    }

    public void updateEventsWithinBounds(final LatLngBounds latLngBounds, final Location currentLocation) {
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
                if (result == null) return;

                String postContent = result.response;

                if (result.responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("START " + postContent + " END");
                    if (postContent != null) {
                        el = new Gson().fromJson(postContent, EventList.class);

                        //mMap.clear();

                        for (Integer integer : eventList.keySet()) {
                            eventList.get(integer).getMarker().remove();
                            eventList.get(integer).getCircle().remove();
                        }

                        if (el != null && el.list != null) {
                            for (Event e : el.list) {
                                System.out.println(e.toString());
                                EventMarker em = new EventMarker(e, mMap, getUid());
                                eventList.put(e.id, em);

                                /*if (!eventList.containsKey(e.id)) {
                                    EventMarker em = new EventMarker(e, mMap, getUid());
                                    eventList.put(e.id, em);
                                    System.out.println(em.event.id);
                                }*/
                            }
                            System.out.println(eventList.size());
                        }
                    } else {
                        return;
                    }
                } else if (result.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    requestLogin();
                    return;
                } else {
                    System.out.println("ERROR: " + result.responseCode);
                    Toast.makeText(getApplicationContext(), "Error on Server Connection!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        execute.execute();
    }

    public void getCheckedInAtEvent(String eventId, String url, final LatLng currentLocation) {

        AsyncTask<String, Integer, HttpPost> execute = new AsyncTask<String, Integer, HttpPost>() {
            @Override
            protected HttpPost doInBackground(String... params) {
                System.out.println("gonna send image");

                String eventId = params[0];
                String url = params[1];

                if (imageToSendToCheckIn == null) {
                    return null;
                }

                ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new NameValuePair("event_id", eventId));
                nvps.add(new NameValuePair("location", getJsonFromLatLng(currentLocation)));
                try {
                    return post(new URL(url), nvps, imageToSendToCheckIn, imageToSendToCheckInName);
                } catch (Exception e) {
                }

                return null;
            }

            class ResultCheckIn {
                public String success;
                public int event_id;
            }

            protected void onPostExecute(HttpPost result) {
                String postContent;

                HttpPost post = result;
                postContent = post.response;

                if (post.responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("START " + postContent + " END");
                    if (postContent != null) {
                        ResultCheckIn rci = new Gson().fromJson(postContent, ResultCheckIn.class);
                        eventList.get(rci.event_id).getMarker().setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else {
                        Toast.makeText(getApplicationContext(), "Error on Server Connection!", Toast.LENGTH_LONG).show();
                    }
                } else if (post.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    requestLogin();
                } else if (post.responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    Toast.makeText(getApplicationContext(), "Imagem não Encontrada! Tire outra foto!", Toast.LENGTH_LONG).show();
                } else if (post.responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE) {
                    Toast.makeText(getApplicationContext(), "Muitas ou Nenhuma face encontrada! Tire outra foto!", Toast.LENGTH_LONG).show();
                } else if (post.responseCode == HttpURLConnection.HTTP_PRECON_FAILED) {
                    Toast.makeText(getApplicationContext(), "Face incompatível! Tire outra foto!", Toast.LENGTH_LONG).show();
                } else if (post.responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    Toast.makeText(getApplicationContext(), "Você já está confirmado no evento!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Erro na Conexão com o Servidor!", Toast.LENGTH_LONG).show();
                }
            }
        };

        execute.execute(eventId, url);
    }

    public static class CheckInError {
        static final int ERROR_SUCCESS = 0;
        static final int ERROR_CONNECTION_FAILED = 1;
        static final int ERROR_NO_PHOTO = 2;
        static final int ERROR_ERROR3 = 3;

        int codError;
        String msgError;

        CheckInError() {
        }

        CheckInError(int codError, String msgError) {
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
        if (requestCode == REQUEST_PERMISSIONS && requestCode == RESULT_OK) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        }else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
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
        }else if(requestCode == REQUEST_CHECK_IN){
            if(resultCode == RESULT_OK){
                eventList.get(Integer.parseInt(selectedEventId)).getMarker().setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }else{

            }
        }else if(requestCode == REQUEST_MENU){
            if(resultCode == RESULT_OK){
                token = null;
                client = null;
                uid = null;

                getLoginCredentialsFile().delete();
                requestLogin();
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
