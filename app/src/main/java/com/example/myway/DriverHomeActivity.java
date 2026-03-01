package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class DriverHomeActivity extends MenuActivity {

    private Button btnPostTrip;
    private ImageButton btnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        btnPostTrip = findViewById(R.id.btnPostTrip);
        btnMore = findViewById(R.id.btnMore);

        btnPostTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverHomeActivity.this, AddTripActivity.class));
            }
        });

        setupMoreButton(btnMore);
    }
}