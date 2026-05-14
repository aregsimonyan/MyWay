package com.example.myway;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.core.app.ActivityCompat;

import com.example.myway.utils.NotificationHelperUtils;
import com.google.android.material.button.MaterialButton;

public class DriverHomeActivity extends MenuActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private MaterialButton btnPostTrip;
    private MaterialButton btnOpenMap;
    private MaterialButton btnMyTrips;
    private ImageButton    btnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        NotificationHelperUtils.init(this);
        requestNotificationPermissionIfNeeded();

        btnPostTrip = findViewById(R.id.btnPostTrip);
        btnOpenMap  = findViewById(R.id.btnOpenMap);
        btnMyTrips  = findViewById(R.id.btnMyTrips);
        btnMore     = findViewById(R.id.btnMore);

        btnPostTrip.setOnClickListener(v ->
                startActivity(new Intent(this, AddTripActivity.class)));

        btnOpenMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));

        btnMyTrips.setOnClickListener(v ->
                startActivity(new Intent(this, MyTripsActivity.class)));

        setupMoreButton(btnMore);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, BookingListenerService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService(new Intent(this, BookingListenerService.class));
    }
}