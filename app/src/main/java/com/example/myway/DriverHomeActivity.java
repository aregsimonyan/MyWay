package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class DriverHomeActivity extends AppCompatActivity {

    private Button btnPostTrip, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        btnPostTrip = findViewById(R.id.btnPostTrip);
        btnLogout = findViewById(R.id.btnLogoutDriver);

        btnPostTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverHomeActivity.this, AddTripActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DriverHomeActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}