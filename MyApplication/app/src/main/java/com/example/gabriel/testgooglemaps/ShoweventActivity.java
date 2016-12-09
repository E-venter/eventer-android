package com.example.gabriel.testgooglemaps;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class ShoweventActivity extends AppCompatActivity {

    String selectedEventId;
    byte[] imageToSendToCheckIn = null;
    String imageToSendToCheckInName = null;

    String token;
    String client;
    String uid;

    LatLng mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showevent);

        selectedEventId = getIntent().getStringExtra("event_id");
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);

        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        Long start_time = getIntent().getLongExtra("start_time", 0l);
        Long end_time = getIntent().getLongExtra("end_time", 0l);
        String description = getIntent().getStringExtra("description");

        System.out.println( start_time +" ; "+ getDate(start_time, "dd/MM/yyyy HH:mm") +" ; "+end_time+" ; "+ getDate(end_time, "dd/MM/yyyy HH:mm"));

        ((TextView) findViewById(R.id.nomeEvento)).setText(name);
        ((TextView) findViewById(R.id.enderecoValor)).setText(address);
        ((TextView) findViewById(R.id.dataIniValor)).setText(getDate(start_time, "dd/MM/yyyy HH:mm"));
        ((TextView) findViewById(R.id.dataFimValor)).setText(getDate(end_time, "dd/MM/yyyy HH:mm"));
        ((TextView) findViewById(R.id.descricaoValor)).setText(description);

        mLastLocation = new LatLng(latitude, longitude);

        token = getIntent().getStringExtra("token");
        client = getIntent().getStringExtra("client");
        uid = getIntent().getStringExtra("uid");

        ImageButton imCheckIn = (ImageButton) findViewById(R.id.fazerCheckIn);
        imCheckIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickFazerCheckIn();
            }
        });

        ImageButton imClose = (ImageButton) findViewById(R.id.closeButton);
        imClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShoweventActivity.this.setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds/1000);
        return formatter.format(calendar.getTime());
    }

    public void onClickFazerCheckIn(){
        dispatchTakePictureIntent();
    }

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
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, MapsActivity.REQUEST_TAKE_PHOTO);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MapsActivity.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                File imageFile = getImageFile();

                Bitmap photo = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                System.out.println(imageFile.getAbsolutePath());
                System.out.println(imageFile.exists());

                if(photo != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    photo.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                    imageToSendToCheckIn = stream.toByteArray();
                    imageToSendToCheckInName = MapsActivity.TEMP_IMAGE_FILE_NAME + ".jpg";
                    imageToSendToCheckInName = MapsActivity.TEMP_IMAGE_FILE_NAME + ".jpg";


                    LatLng llCurrentLocation;
                    if (mLastLocation == null) {
                        llCurrentLocation = new LatLng(0, 0);
                    } else {
                        llCurrentLocation = new LatLng(mLastLocation.latitude, mLastLocation.longitude);
                    }

                    getCheckedInAtEvent(selectedEventId, MapsActivity.URL_TO_CHECK_IN, llCurrentLocation, this);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }else if(requestCode == MapsActivity.REQUEST_LOGIN){
            if(resultCode == RESULT_OK){
                client = data.getStringExtra("client");
                uid = data.getStringExtra("uid");
                token = data.getStringExtra("auth");

                storeLoginCredentials();
            }else{
                startActivityForResult(
                        new Intent(this, LoginActivity.class),
                        MapsActivity.REQUEST_LOGIN);
            }
        }
    }

    private static String getJsonFromLatLng(LatLng latLng) {
        return "{\"latitude\":\"" + latLng.latitude + "\"," +
                "\"longitude\":\"" + latLng.longitude + "\"" + "}";
    }

    public HttpPost post(URL url, ArrayList<NameValuePair> values, byte[] image, String imageName) {
        try {

            HttpPost httpPost = new HttpPost(url, token, client, uid);

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

    public void requestLogin() {
        startActivityForResult(
                new Intent(this, LoginActivity.class),
                MapsActivity.REQUEST_LOGIN);
    }

    public void getCheckedInAtEvent(String eventId, String url, final LatLng currentLocation, final Activity activity) {

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
                    if (postContent != null) {
                        activity.setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Error on Server Connection!", Toast.LENGTH_LONG).show();
                    }
                } else if (post.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    requestLogin();
                } else if (post.responseCode == 416) {
                    Toast.makeText(getApplicationContext(), "Você não se encontra no evento!", Toast.LENGTH_LONG).show();
                } else if (post.responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    Toast.makeText(getApplicationContext(), "Imagem não Encontrada! Tire outra foto!", Toast.LENGTH_LONG).show();
                } else if (post.responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE) {
                    Toast.makeText(getApplicationContext(), "Muitas ou Nenhuma face encontrada! Tire outra foto!", Toast.LENGTH_LONG).show();
                } else if (post.responseCode == 488) {
                    Toast.makeText(getApplicationContext(), "O evento não começou ainda!", Toast.LENGTH_LONG).show();
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
