package com.example.gabriel.testgooglemaps;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class CadastrarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar);

        ImageButton cadastrar = (ImageButton) findViewById(R.id.LoginGo);
        cadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClick_signin();
            }
        });

        ImageButton camBtn = (ImageButton) findViewById(R.id.uploadFoto);
        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Gonna take a picture");
                dispatchTakePictureIntent();
            }
        });
    }

    public void onClick_signin(){
        if(imageToSendToCheckIn != null) {
            EditText nome = (EditText) findViewById(R.id.emailC);
            EditText email = (EditText) findViewById(R.id.passwordC);
            EditText password = (EditText) findViewById(R.id.senhaC);

            ArrayList<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new NameValuePair("email", email.getText().toString()));
            nvps.add(new NameValuePair("password", password.getText().toString()));
            nvps.add(new NameValuePair("password_confirmation", password.getText().toString()));
            nvps.add(new NameValuePair("name", password.getText().toString()));

            try {
                sendSigninMessage(new URL(MapsActivity.URL_TO_SIGN_UP), nvps, this, this.getIntent());
            } catch (MalformedURLException e) {
            }
        }else{
            Toast.makeText(getApplicationContext(), "Tire uma foto antes!", Toast.LENGTH_LONG).show();
        }
    }

    public void sendSigninMessage(final URL url, final ArrayList<NameValuePair> nvps, final AppCompatActivity act, final Intent my_intent){

        AsyncTask<Integer, Integer, HttpPost> execute = new AsyncTask<Integer, Integer, HttpPost>() {
            @Override
            protected HttpPost doInBackground(Integer... params) {
                return post(url, nvps, imageToSendToCheckIn, imageToSendToCheckInName);
            }

            protected void onPostExecute(HttpPost result) {

                if(result.responseCode == HttpURLConnection.HTTP_OK) {
                    ResponseWareve rw = new Gson().fromJson(result.response, ResponseWareve.class);
                    Toast.makeText(getApplicationContext(), "Cadastro efetuado com sucesso!", Toast.LENGTH_LONG).show();
                    act.setResult(RESULT_OK);
                    finish();
                }else if(result.responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE){
                    Toast.makeText(getApplicationContext(), "Muitos ou Nenhum rosto na foto!", Toast.LENGTH_LONG).show();
                }else if(result.responseCode == 422){
                    Toast.makeText(getApplicationContext(), "Email ja existente!", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Erro "+result.responseCode+" na conexao!", Toast.LENGTH_LONG).show();
                }
            }
        };

        execute.execute();
    }

    public class ResponseWareve{
        String success = "";
        String[] errors = {};
    }

    public HttpPost post(URL url, ArrayList<NameValuePair> values, byte[] image, String imageName) {
        try {
            HttpPost httpPost = new HttpPost(url, "","", "");

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
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, MapsActivity.REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MapsActivity.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            try {
                File imageFile = getImageFile();
                //Bitmap photo = (Bitmap) data.getExtras().get("data");

                Bitmap photo = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                //Matrix matrix = new Matrix();
                //matrix.postRotate(90);
                //Bitmap rotatedBitmap = Bitmap.createBitmap(photo , 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                //stream.write();

                photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                imageToSendToCheckIn = stream.toByteArray();
                imageToSendToCheckInName = MapsActivity.TEMP_IMAGE_FILE_NAME + ".jpg";
                imageToSendToCheckInName = MapsActivity.TEMP_IMAGE_FILE_NAME + ".jpg";

                ((CheckBox)findViewById(R.id.checkBoxFoto)).setChecked(true);
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
