package com.example.gabriel.testgooglemaps;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

public class NavegationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navegation);

        ImageButton imLogout = (ImageButton) findViewById(R.id.logout);
        imLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavegationActivity.this.setResult(RESULT_OK);
                finish();
            }
        });
    }
}
