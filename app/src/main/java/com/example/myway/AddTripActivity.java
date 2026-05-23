package com.example.myway;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myway.models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTripActivity extends MenuActivity {

    private TextView tvFromLocation;
    private TextView tvToLocation;
    private EditText etDate;
    private EditText etTime;
    private EditText etPrice;
    private EditText etSeats;
    private Button btnPublish;
    private ImageButton btnMore;
    private TextView tvRouteStatus;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Calendar calendar;

    private String driverName;
    private String licensePlate;
    private String carModel;
    private String carCategory;

    private String fromLocationName = "";
    private String toLocationName = "";
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private String encodedPolyline = "";
    private boolean routeSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        mAuth    = FirebaseAuth.getInstance();
        db       = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        tvFromLocation = findViewById(R.id.tvFromLocation);
        tvToLocation   = findViewById(R.id.tvToLocation);
        etDate         = findViewById(R.id.etDate);
        etTime         = findViewById(R.id.etTime);
        etPrice        = findViewById(R.id.etPrice);
        etSeats        = findViewById(R.id.etSeats);
        btnPublish     = findViewById(R.id.btnPublish);
        btnMore        = findViewById(R.id.btnMore);
        tvRouteStatus  = findViewById(R.id.tvRouteStatus);

        fetchDriverDetails();
        setupMoreButton(btnMore);

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());

        findViewById(R.id.btnSelectRouteOnMap).setOnClickListener(v -> {
            Intent intent = new Intent(AddTripActivity.this, RouteSelectionActivity.class);
            startActivityForResult(intent, RouteSelectionActivity.REQUEST_CODE);
        });

        btnPublish.setOnClickListener(v -> publishTrip());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RouteSelectionActivity.REQUEST_CODE
                && resultCode == RESULT_OK && data != null) {
            startLat         = data.getDoubleExtra("startLat", 0);
            startLng         = data.getDoubleExtra("startLng", 0);
            endLat           = data.getDoubleExtra("endLat", 0);
            endLng           = data.getDoubleExtra("endLng", 0);
            encodedPolyline  = data.getStringExtra("encodedPolyline");
            fromLocationName = data.getStringExtra("startName");
            toLocationName   = data.getStringExtra("endName");

            if (encodedPolyline  == null) encodedPolyline  = "";
            if (fromLocationName == null) fromLocationName = "";
            if (toLocationName   == null) toLocationName   = "";

            routeSelected = true;

            tvFromLocation.setText(fromLocationName);
            tvFromLocation.setTextColor(getResources().getColor(R.color.colorPrimaryText));
            tvToLocation.setText(toLocationName);
            tvToLocation.setTextColor(getResources().getColor(R.color.colorPrimaryText));

            tvRouteStatus.setText("Route drawn on map");
            tvRouteStatus.setVisibility(View.VISIBLE);
        }
    }

    private void fetchDriverDetails() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot doc = task.getResult();
                            driverName   = doc.getString("name");
                            licensePlate = doc.getString("licensePlate");
                            carModel     = doc.getString("carModel");
                            carCategory  = doc.getString("carCategory");
                        }
                    }
                });
    }

    private void showDatePicker() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                etDate.setText(sdf.format(calendar.getTime()));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
                etTime.setText(sdf.format(calendar.getTime()));
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void publishTrip() {
        String priceStr = etPrice.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();

        if (TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(seatsStr)
                || TextUtils.isEmpty(etDate.getText())) {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!routeSelected) {
            Toast.makeText(this,
                    "Please draw your route on the map so passengers can find you",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (fromLocationName.equals(toLocationName)) {
            Toast.makeText(this,
                    "Start and destination cannot be the same", Toast.LENGTH_SHORT).show();
            return;
        }

        double price  = Double.parseDouble(priceStr);
        int seats     = Integer.parseInt(seatsStr);
        String tripId = db.collection("trips").document().getId();
        String driverId = mAuth.getCurrentUser().getUid();

        Trip newTrip = new Trip(tripId, driverId, driverName, licensePlate,
                fromLocationName, toLocationName, calendar.getTimeInMillis(),
                price, seats, carCategory);

        newTrip.setCarModel(carModel);
        newTrip.setStartLat(startLat);
        newTrip.setStartLng(startLng);
        newTrip.setEndLat(endLat);
        newTrip.setEndLng(endLng);
        newTrip.setEncodedPolyline(encodedPolyline);

        db.collection("trips").document(tripId).set(newTrip)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AddTripActivity.this,
                                    "Ride published successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(AddTripActivity.this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}