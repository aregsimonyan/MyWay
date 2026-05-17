package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.adapters.PassengerAdapter;
import com.example.myway.models.BookedPassenger;
import com.example.myway.models.Trip;
import com.example.myway.utils.RatingUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class TripPassengersActivity extends MenuActivity {

    public static final String EXTRA_TRIP_ID = "tripId";

    private TextView     tvTripRoute;
    private TextView     tvTripDateTime;
    private TextView     tvBookedCount;
    private TextView     tvAvailableCount;
    private TextView     tvTotalCount;
    private TextView     tvPassengerCount;
    private TextView     tvNoPassengers;
    private ProgressBar  progressPassengers;
    private RecyclerView recyclerView;
    private ImageButton  btnMore;

    private final List<BookedPassenger> passengerList = new ArrayList<>();
    private PassengerAdapter   adapter;
    private ListenerRegistration tripListener;
    private String tripId;
    private boolean tripEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_passengers);

        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null || tripId.isEmpty()) {
            Toast.makeText(this, "Trip not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTripRoute        = findViewById(R.id.tvTripRoute);
        tvTripDateTime     = findViewById(R.id.tvTripDateTime);
        tvBookedCount      = findViewById(R.id.tvBookedCount);
        tvAvailableCount   = findViewById(R.id.tvAvailableCount);
        tvTotalCount       = findViewById(R.id.tvTotalCount);
        tvPassengerCount   = findViewById(R.id.tvPassengerCount);
        tvNoPassengers     = findViewById(R.id.tvNoPassengers);
        progressPassengers = findViewById(R.id.progressPassengers);
        recyclerView       = findViewById(R.id.recyclerViewPassengers);
        btnMore            = findViewById(R.id.btnMore);

        setupMoreButton(btnMore);

        adapter = new PassengerAdapter(passengerList, this::openRatePassenger, () -> tripEnded);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        attachTripListener();
    }

    private void attachTripListener() {
        progressPassengers.setVisibility(View.VISIBLE);

        tripListener = db.collection("trips")
                .document(tripId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        progressPassengers.setVisibility(View.GONE);
                        Toast.makeText(this, "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        progressPassengers.setVisibility(View.GONE);
                        return;
                    }
                    Trip trip = snapshot.toObject(Trip.class);
                    if (trip == null) {
                        progressPassengers.setVisibility(View.GONE);
                        return;
                    }
                    tripEnded = trip.getDateTime() < System.currentTimeMillis();
                    updateTripSummary(trip);
                    loadPassengerProfiles(trip);
                });
    }

    private void updateTripSummary(@NonNull Trip trip) {
        tvTripRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);
        tvTripDateTime.setText(sdf.format(trip.getDateTime()));

        int total  = trip.getTotalSeats();
        int avail  = trip.getSeatsAvailable();
        int booked = total - avail;

        tvTotalCount.setText(String.valueOf(total));
        tvAvailableCount.setText(String.valueOf(avail));
        tvBookedCount.setText(String.valueOf(booked));
        tvPassengerCount.setText(booked + " passenger" + (booked == 1 ? "" : "s") + " booked");
    }

    private void loadPassengerProfiles(Trip trip) {
        List<String> passengerIds = trip.getPassengerIds();
        passengerList.clear();

        if (passengerIds == null || passengerIds.isEmpty()) {
            progressPassengers.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            tvNoPassengers.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
            return;
        }

        progressPassengers.setVisibility(View.VISIBLE);
        tvNoPassengers.setVisibility(View.GONE);

        int total = passengerIds.size();
        AtomicInteger remaining = new AtomicInteger(total);
        List<BookedPassenger> ordered = new ArrayList<>();
        for (int i = 0; i < total; i++) ordered.add(null);

        for (int i = 0; i < total; i++) {
            final int index  = i;
            final String uid = passengerIds.get(index);

            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot doc = task.getResult();
                            String name  = doc.getString("name");
                            String email = doc.getString("email");
                            String phone = doc.getString("phone");
                            Double avg   = doc.getDouble("averageRating");
                            Long   count = doc.getLong("ratingCount");

                            BookedPassenger bp = new BookedPassenger(
                                    uid,
                                    name  != null ? name  : "Unknown",
                                    email != null ? email : "",
                                    phone != null ? phone : "");

                            if (avg != null && count != null && count > 0) {
                                bp.setAverageRating(avg);
                                bp.setRatingCount(count.intValue());
                            }
                            ordered.set(index, bp);
                        }

                        if (remaining.decrementAndGet() == 0) {
                            passengerList.clear();
                            for (BookedPassenger bp : ordered) {
                                if (bp != null) passengerList.add(bp);
                            }
                            progressPassengers.setVisibility(View.GONE);
                            if (passengerList.isEmpty()) {
                                recyclerView.setVisibility(View.GONE);
                                tvNoPassengers.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                tvNoPassengers.setVisibility(View.GONE);
                            }
                            adapter.notifyDataSetChanged();

                            if (!tripEnded) {
                                scheduleAlarmsForAllPassengers(trip, passengerList);
                            }
                        }
                    });
        }
    }

    private void scheduleAlarmsForAllPassengers(Trip trip, List<BookedPassenger> passengers) {
        String driverId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (driverId == null) return;

        for (BookedPassenger p : passengers) {
            RatingUtils.scheduleRatingNotification(
                    this,
                    trip.getTripId() + "_" + p.getUid(),
                    p.getUid(),
                    p.getName(),
                    "driver",
                    trip.getDateTime());
        }
    }

    private void openRatePassenger(BookedPassenger passenger) {
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra(RatingActivity.EXTRA_TRIP_ID,         tripId);
        intent.putExtra(RatingActivity.EXTRA_RATED_USER_ID,   passenger.getUid());
        intent.putExtra(RatingActivity.EXTRA_RATED_USER_NAME, passenger.getName());
        intent.putExtra(RatingActivity.EXTRA_RATER_TYPE,      "driver");
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tripListener != null) tripListener.remove();
    }
}