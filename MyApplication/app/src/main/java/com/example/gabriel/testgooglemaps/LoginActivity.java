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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final AppCompatActivity aux = this;

        ImageButton fab = (ImageButton) findViewById(R.id.LoginGo);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClick_login();

                /*
                //faz as verificacoes de login aqui
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);*/
            }
        });
/*
        ImageButton esqueci = (ImageButton) findViewById(R.id.esqueciSenha);
        esqueci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent recuperarSenha = new Intent(LoginActivity.this, LoginPasswordForgottenActivity.class);
                recuperarSenha.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(recuperarSenha);
            }
        });*/

        ImageButton cadastrar = (ImageButton) findViewById(R.id.criarConta);
        cadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //faz as verificacoes de login aqui
                //fzer a cadastrarAc

                Intent cadastrar = new Intent(LoginActivity.this, CadastrarActivity.class);
                //cadastrar.putExtra("activity", aux);
                startActivity(cadastrar);
            }
        });
    }

    public void onClick_login(){

        EditText email = (EditText)findViewById(R.id.emailLogin);
        EditText password = (EditText)findViewById(R.id.senhaLogin);

        try {
            sendLoginMessage(new URL(MapsActivity.URL_TO_LOGIN), email.getText().toString(), password.getText().toString(), this, this.getIntent());
        } catch (MalformedURLException e) {
        }
    }

    public void sendLoginMessage(final URL url, final String email, final String password, final AppCompatActivity act, final Intent my_intent){
        AsyncTask<Integer, Integer, HttpPost> execute = new AsyncTask<Integer, Integer, HttpPost>() {

            @Override
            protected HttpPost doInBackground(Integer... params) {
                return post(url, email, password);
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
                    Toast.makeText(getApplicationContext(), "Erro no Email/Password!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        execute.execute();
    }

    public HttpPost post(URL url, String email, final String password) {
        try {

            HttpPost httpPost = new HttpPost(url, "", "", "");

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
