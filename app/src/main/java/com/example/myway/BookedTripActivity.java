package com.example.myway;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myway.models.Trip;
import com.example.myway.utils.RatingUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BookedTripActivity extends AppCompatActivity {

    private ProgressBar    progressBooked;
    private LinearLayout   layoutContent;
    private TextView       tvBookedRoute;
    private TextView       tvBookedDateTime;
    private TextView       tvBookedPrice;
    private TextView       tvBookedDriver;
    private TextView       tvBookedCarModel;
    private TextView       tvBookedCarCategory;
    private TextView       tvBookedLicensePlate;
    private TextView       tvDriverRating;
    private MaterialButton btnCallDriver;
    private TextView       tvCountdown;
    private TextView       tvCountdownLabel;
    private MaterialButton btnViewOnMap;
    private MaterialButton btnCancelBooking;
    private MaterialButton btnRateDriver;

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;
    private CountDownTimer    countDownTimer;
    private Trip              currentTrip;
    private String            tripId;
    private String            driverPhone = "";
    private String            driverName  = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booked_trip);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tripId = getIntent().getStringExtra("tripId");

        progressBooked       = findViewById(R.id.progressBooked);
        layoutContent        = findViewById(R.id.layoutContent);
        tvBookedRoute        = findViewById(R.id.tvBookedRoute);
        tvBookedDateTime     = findViewById(R.id.tvBookedDateTime);
        tvBookedPrice        = findViewById(R.id.tvBookedPrice);
        tvBookedDriver       = findViewById(R.id.tvBookedDriver);
        tvBookedCarModel     = findViewById(R.id.tvBookedCarModel);
        tvBookedCarCategory  = findViewById(R.id.tvBookedCarCategory);
        tvBookedLicensePlate = findViewById(R.id.tvBookedLicensePlate);
        tvDriverRating       = findViewById(R.id.tvDriverRating);
        btnCallDriver        = findViewById(R.id.btnCallDriver);
        tvCountdown          = findViewById(R.id.tvCountdown);
        tvCountdownLabel     = findViewById(R.id.tvCountdownLabel);
        btnViewOnMap         = findViewById(R.id.btnViewOnMap);
        btnCancelBooking     = findViewById(R.id.btnCancelBooking);
        btnRateDriver        = findViewById(R.id.btnRateDriver);

        if (tripId == null) {
            Toast.makeText(this, "Trip not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTripDetails();
    }

    private void loadTripDetails() {
        progressBooked.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);

        db.collection("trips").document(tripId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().exists()) {
                        currentTrip = task.getResult().toObject(Trip.class);
                        if (currentTrip != null) {
                            loadDriverDetails(currentTrip);
                        } else {
                            progressBooked.setVisibility(View.GONE);
                        }
                    } else {
                        progressBooked.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not load trip details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadDriverDetails(Trip trip) {
        db.collection("users").document(trip.getDriverId()).get()
                .addOnCompleteListener(task -> {
                    progressBooked.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        String phone = doc.getString("phone");
                        if (phone != null && !phone.isEmpty()) driverPhone = phone;

                        String fetchedName = doc.getString("name");
                        if (fetchedName != null && !fetchedName.isEmpty()) driverName = fetchedName;

                        Double avg   = doc.getDouble("averageRating");
                        Long   count = doc.getLong("ratingCount");
                        if (avg != null && count != null && count > 0) {
                            tvDriverRating.setText(RatingUtils.buildStarsText(avg, count.intValue()));
                            tvDriverRating.setVisibility(View.VISIBLE);
                        } else {
                            tvDriverRating.setVisibility(View.GONE);
                        }
                    }
                    displayTripInfo(trip);
                    layoutContent.setVisibility(View.VISIBLE);
                });
    }

    private void displayTripInfo(Trip trip) {
        tvBookedRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM  HH:mm", Locale.US);
        tvBookedDateTime.setText(sdf.format(trip.getDateTime()));
        tvBookedPrice.setText((int) trip.getPricePerSeat() + " AMD");
        tvBookedDriver.setText(trip.getDriverName());

        tvBookedCarModel.setText(
                trip.getCarModel() != null && !trip.getCarModel().isEmpty()
                        ? trip.getCarModel() : "—");
        tvBookedCarCategory.setText(trip.getCarCategory() != null ? trip.getCarCategory() : "—");
        tvBookedLicensePlate.setText(trip.getLicensePlate() != null ? trip.getLicensePlate() : "—");

        if (!driverPhone.isEmpty()) {
            btnCallDriver.setVisibility(View.VISIBLE);
            btnCallDriver.setText("Call Driver  " + driverPhone);
            btnCallDriver.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + driverPhone));
                startActivity(intent);
            });
        }

        long now      = System.currentTimeMillis();
        boolean ended = trip.getDateTime() < now;

        startCountdown(trip.getDateTime());

        btnViewOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("tripId", trip.getTripId());
            startActivity(intent);
        });

        if (ended) {
            btnCancelBooking.setVisibility(View.GONE);
            btnRateDriver.setVisibility(View.VISIBLE);
            btnRateDriver.setOnClickListener(v -> openRatingScreen(trip));
        } else {
            btnCancelBooking.setVisibility(View.VISIBLE);
            btnRateDriver.setVisibility(View.GONE);
            btnCancelBooking.setOnClickListener(v -> confirmCancel(trip));

            String nameToUse = !driverName.isEmpty() ? driverName : trip.getDriverName();
            if (nameToUse == null) nameToUse = "Driver";

            RatingUtils.scheduleRatingNotification(
                    this,
                    trip.getTripId(),
                    trip.getDriverId(),
                    nameToUse,
                    "passenger",
                    trip.getDateTime());
        }
    }

    private void openRatingScreen(Trip trip) {
        String nameToUse = !driverName.isEmpty() ? driverName : trip.getDriverName();
        if (nameToUse == null) nameToUse = "Driver";

        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra(RatingActivity.EXTRA_TRIP_ID,         trip.getTripId());
        intent.putExtra(RatingActivity.EXTRA_RATED_USER_ID,   trip.getDriverId());
        intent.putExtra(RatingActivity.EXTRA_RATED_USER_NAME, nameToUse);
        intent.putExtra(RatingActivity.EXTRA_RATER_TYPE,      "passenger");
        startActivity(intent);
    }

    private void startCountdown(long tripTimeMillis) {
        long remaining = tripTimeMillis - System.currentTimeMillis();

        if (remaining <= 0) {
            tvCountdown.setText("Trip has departed");
            tvCountdownLabel.setVisibility(View.GONE);
            return;
        }

        updateCountdownText(remaining);

        countDownTimer = new CountDownTimer(remaining, 60_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdownText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("Trip has departed");
                tvCountdownLabel.setVisibility(View.GONE);
                btnCancelBooking.setVisibility(View.GONE);
                btnRateDriver.setVisibility(View.VISIBLE);
                if (currentTrip != null) {
                    btnRateDriver.setOnClickListener(v -> openRatingScreen(currentTrip));
                }
            }
        }.start();
    }

    private void updateCountdownText(long millis) {
        long days    = millis / (1000 * 60 * 60 * 24);
        long hours   = (millis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        tvCountdown.setText(String.format(Locale.US,
                "%02d d  %02d h  %02d m", days, hours, minutes));
    }

    private void confirmCancel(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage("Are you sure you want to cancel your booking for the trip from "
                        + trip.getFromLocation() + " to " + trip.getToLocation() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelBooking(trip))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelBooking(Trip trip) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("trips").document(trip.getTripId())
                .update(
                        "passengerIds",    FieldValue.arrayRemove(uid),
                        "seatsAvailable",  trip.getSeatsAvailable() + 1
                )
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Booking cancelled.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Could not cancel: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}