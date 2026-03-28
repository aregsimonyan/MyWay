package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import com.google.android.material.button.MaterialButton;

public class DriverHomeActivity extends MenuActivity {

    private MaterialButton btnPostTrip;
    private MaterialButton btnOpenMap;
    private MaterialButton btnMyTrips;
    private ImageButton btnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        btnPostTrip = findViewById(R.id.btnPostTrip);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnMyTrips = findViewById(R.id.btnMyTrips);
        btnMore = findViewById(R.id.btnMore);

        btnPostTrip.setOnClickListener(v ->
                startActivity(new Intent(DriverHomeActivity.this, AddTripActivity.class)));

        btnOpenMap.setOnClickListener(v ->
                startActivity(new Intent(DriverHomeActivity.this, MapActivity.class)));

        btnMyTrips.setOnClickListener(v ->
                startActivity(new Intent(DriverHomeActivity.this, MyTripsActivity.class)));

        setupMoreButton(btnMore);
    }
}