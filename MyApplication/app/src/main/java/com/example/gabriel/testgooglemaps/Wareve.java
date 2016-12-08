package com.example.gabriel.testgooglemaps;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Gabriel on 08/12/2016.
 */

public class Wareve extends AppCompatActivity {

    public void onClick_signin(){

        EditText email = (EditText)findViewById(R.id.emailLogin);
        EditText password = (EditText)findViewById(R.id.senhaLogin);

        ArrayList<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new NameValuePair("email", email.getText().toString()));
        nvps.add(new NameValuePair("password", password.getText().toString()));

        try {
            sendSigninMessage(new URL(MapsActivity.URL_TO_LOGIN), nvps, this, this.getIntent());
        } catch (MalformedURLException e) {
        }
    }

    public void sendSigninMessage(final URL url, final ArrayList<NameValuePair> nvps, final AppCompatActivity act, final Intent my_intent){

        AsyncTask<Integer, Integer, HttpPost> execute = new AsyncTask<Integer, Integer, HttpPost>() {
            @Override
            protected HttpPost doInBackground(Integer... params) {
                return post(url, nvps);
            }

            protected void onPostExecute(HttpPost result) {

                
                if(result.responseHeader.containsKey("Access-Token") &&
                        result.responseHeader.containsKey("Client") &&
                        result.responseHeader.containsKey("Uid")){
                    String token = result.responseHeader.get("Access-Token").get(0);
                    String client = result.responseHeader.get("Client").get(0);
                    String uid = result.responseHeader.get("Uid").get(0);

                    Intent intent = my_intent;
                    intent.putExtra("client", client);
                    intent.putExtra("uid", uid);
                    intent.putExtra("auth", token);
                    act.setResult(RESULT_OK, intent);
                    finish();
                }else{
                    Toast.makeText(getApplicationContext(), "Wrong Email/Password!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        execute.execute();
    }

    public HttpPost post(URL url,  ArrayList<NameValuePair> nvps) {
        try {

            HttpPost httpPost = new HttpPost(url, "", "", "");

            for (NameValuePair nvp : nvps) {
                httpPost.add(nvp);
            }

            httpPost.send();

            return httpPost;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
