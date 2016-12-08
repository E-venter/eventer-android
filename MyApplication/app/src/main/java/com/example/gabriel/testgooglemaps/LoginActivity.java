package com.example.gabriel.testgooglemaps;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public void onClick_login(View v){
        /*
        Intent intent = this.getIntent();
        intent.putExtra("client", "PDDMZWZ8asi3KXz3hoJCig");
        intent.putExtra("uid", "joseph@mailinator.com");
        intent.putExtra("auth", "JJrhZMlFNSu1mcgvpEKPSg");
        this.setResult(RESULT_OK, intent);
        finish();
        */

        EditText mEdit   = (EditText)findViewById(R.id.editText);
        EditText mPassword   = (EditText)findViewById(R.id.editText2);

        try {
            sendLoginMessage(new URL(MapsActivity.URL_TO_LOGIN), mEdit.getText().toString(), mPassword.getText().toString(), this, this.getIntent());
        } catch (MalformedURLException e) {
        }
    }

    public void sendLoginMessage(final URL url, final String email, final String password, final AppCompatActivity act, final Intent my_intent){
        AsyncTask<Integer, Integer, HttpPost> execute = new AsyncTask<Integer, Integer, HttpPost>() {
            EventList el;

            @Override
            protected HttpPost doInBackground(Integer... params) {
                return post(url, email, password);
            }

            protected void onPostExecute(HttpPost result) {
                /*
                System.out.println("HEADER");
                for(String key : result.responseHeader.keySet()){
                    for(String value : result.responseHeader.get(key)){
                        System.out.println(key + " : " + value);
                    }
                }
                */



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
                    //act.setResult(RESULT_CANCELED);
                    //finish();
                    Toast.makeText(getApplicationContext(), "Wrong Email/Password!", Toast.LENGTH_SHORT).show();
                }

            }
        };

        execute.execute();
    }

    public HttpPost post(URL url, String email, final String password) {
        try {

            HttpPost httpPost = new HttpPost(url, "", "", "");

            //String info = "{"+"\"email\" : \"" + email + "\" \"password\" : \""+password+"\""+"}";
            httpPost.add("email", email);
            httpPost.add("password", password);
            httpPost.send();

            return httpPost;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
