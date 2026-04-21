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

import com.example.myway.adapters.TripAdapter;
import com.example.myway.models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyBookingsActivity extends MenuActivity {

    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private List<Trip> bookedTrips;
    private ProgressBar progressBookings;
    private TextView tvEmpty;
    private ImageButton btnMore;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView     = findViewById(R.id.recyclerViewBookings);
        progressBookings = findViewById(R.id.progressBookings);
        tvEmpty          = findViewById(R.id.tvEmptyBookings);
        btnMore          = findViewById(R.id.btnMore);

        bookedTrips = new ArrayList<>();

        String currentUserId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        adapter = new TripAdapter(bookedTrips, currentUserId,
                new TripAdapter.OnTripActionListener() {
                    @Override
                    public void onCardClick(Trip trip) {
                        Intent intent = new Intent(MyBookingsActivity.this,
                                BookedTripActivity.class);
                        intent.putExtra("tripId", trip.getTripId());
                        startActivity(intent);
                    }

                    @Override
                    public void onBookClick(Trip trip) {
                        // Already booked — no action needed
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupMoreButton(btnMore);
        loadMyBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyBookings();
    }

    private void loadMyBookings() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        progressBookings.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        // Using only whereArrayContains avoids the need for a composite Firestore index.
        // We filter out past trips ourselves in Java below.
        db.collection("trips")
                .whereArrayContains("passengerIds", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressBookings.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            bookedTrips.clear();
                            long now = System.currentTimeMillis();

                            for (DocumentSnapshot doc : task.getResult()) {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip != null && trip.getDateTime() > now) {
                                    bookedTrips.add(trip);
                                }
                            }

                            bookedTrips.sort((a, b) ->
                                    Long.compare(a.getDateTime(), b.getDateTime()));

                            adapter.notifyDataSetChanged();

                            if (bookedTrips.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                            }

                        } else {
                            Toast.makeText(MyBookingsActivity.this,
                                    "Failed to load bookings: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}