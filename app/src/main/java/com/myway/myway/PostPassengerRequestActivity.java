package com.myway.myway;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.myway.myway.models.PassengerRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PostPassengerRequestActivity extends MenuActivity {

    private TextView tvReqFromLocation;
    private TextView tvReqToLocation;
    private EditText etReqDate;
    private EditText etReqTime;
    private EditText etReqMaxPrice;
    private TextView tvReqRouteStatus;
    private ProgressBar progressPublish;
    private ImageButton btnMore;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Calendar calendar;

    private String passengerName;
    private String passengerPhone;
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
        setContentView(R.layout.activity_post_passenger_request);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        tvReqFromLocation = findViewById(R.id.tvReqFromLocation);
        tvReqToLocation = findViewById(R.id.tvReqToLocation);
        etReqDate = findViewById(R.id.etReqDate);
        etReqTime = findViewById(R.id.etReqTime);
        etReqMaxPrice = findViewById(R.id.etReqMaxPrice);
        tvReqRouteStatus = findViewById(R.id.tvReqRouteStatus);
        progressPublish = findViewById(R.id.progressPublish);
        btnMore = findViewById(R.id.btnMore);

        fetchPassengerProfile();
        setupMoreButton(btnMore);

        etReqDate.setOnClickListener(v -> showDatePicker());
        etReqTime.setOnClickListener(v -> showTimePicker());

        findViewById(R.id.btnReqSelectRoute).setOnClickListener(v -> {
            Intent intent = new Intent(PostPassengerRequestActivity.this,
                    RouteSelectionActivity.class);
            startActivityForResult(intent, RouteSelectionActivity.REQUEST_CODE);
        });

        findViewById(R.id.btnPublishRequest).setOnClickListener(v -> checkDuplicateThenPublish());
    }

    private void fetchPassengerProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            passengerName = task.getResult().getString("name");
                            passengerPhone = task.getResult().getString("phone");
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RouteSelectionActivity.REQUEST_CODE
                && resultCode == RESULT_OK && data != null) {
            startLat = data.getDoubleExtra("startLat", 0);
            startLng = data.getDoubleExtra("startLng", 0);
            endLat = data.getDoubleExtra("endLat", 0);
            endLng = data.getDoubleExtra("endLng", 0);
            encodedPolyline = data.getStringExtra("encodedPolyline");
            fromLocationName = data.getStringExtra("startName");
            toLocationName = data.getStringExtra("endName");

            if (encodedPolyline == null) encodedPolyline = "";
            if (fromLocationName == null) fromLocationName = "";
            if (toLocationName == null) toLocationName = "";

            routeSelected = true;

            tvReqFromLocation.setText(fromLocationName);
            tvReqFromLocation.setTextColor(getResources().getColor(R.color.colorPrimaryText));
            tvReqToLocation.setText(toLocationName);
            tvReqToLocation.setTextColor(getResources().getColor(R.color.colorPrimaryText));

            tvReqRouteStatus.setText("Route drawn on map");
            tvReqRouteStatus.setVisibility(View.VISIBLE);
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                etReqDate.setText(sdf.format(calendar.getTime()));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hour, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
                etReqTime.setText(sdf.format(calendar.getTime()));
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void checkDuplicateThenPublish() {
        String priceStr = etReqMaxPrice.getText().toString().trim();

        if (TextUtils.isEmpty(etReqDate.getText())) {
            Toast.makeText(this, "Please pick a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Please enter your maximum price", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!routeSelected) {
            Toast.makeText(this, "Please draw your route on the map", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fromLocationName.equals(toLocationName)) {
            Toast.makeText(this, "Start and destination cannot be the same", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        progressPublish.setVisibility(View.VISIBLE);
        findViewById(R.id.btnPublishRequest).setEnabled(false);

        db.collection("requests")
                .whereEqualTo("passengerId", uid)
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressPublish.setVisibility(View.GONE);
                        findViewById(R.id.btnPublishRequest).setEnabled(true);

                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            new AlertDialog.Builder(PostPassengerRequestActivity.this)
                                    .setTitle("Active Request Exists")
                                    .setMessage("You already have an active ride request. "
                                            + "Please delete it from My Requests before posting a new one.")
                                    .setPositiveButton("My Requests", (dialog, which) -> {
                                        startActivity(new Intent(PostPassengerRequestActivity.this,
                                                MyRequestsActivity.class));
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            publishRequest(fromLocationName, toLocationName,
                                    Double.parseDouble(priceStr), uid);
                        }
                    }
                });
    }

    private void publishRequest(String from, String to, double maxPrice, String uid) {
        progressPublish.setVisibility(View.VISIBLE);
        String requestId = db.collection("requests").document().getId();

        PassengerRequest request = new PassengerRequest(
                requestId, uid,
                passengerName != null ? passengerName : "Passenger",
                passengerPhone != null ? passengerPhone : "",
                from, to,
                startLat, startLng, endLat, endLng,
                maxPrice, calendar.getTimeInMillis(), encodedPolyline
        );

        db.collection("requests").document(requestId).set(request)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressPublish.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(PostPassengerRequestActivity.this,
                                    "Request posted!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(PostPassengerRequestActivity.this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}