package com.example.myway;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.adapters.DriverTripAdapter;
import com.example.myway.models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyTripsActivity extends MenuActivity {

    private RecyclerView recyclerView;
    private DriverTripAdapter adapter;
    private List<Trip> displayList;
    private List<Trip> allUpcoming;
    private List<Trip> allPast;

    private ProgressBar progressTrips;
    private TextView tvEmpty;
    private TextView tabUpcoming;
    private TextView tabPast;
    private View tabIndicator;
    private ImageButton btnMore;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean showingUpcoming = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerViewDriverTrips);
        progressTrips = findViewById(R.id.progressTrips);
        tvEmpty = findViewById(R.id.tvEmptyTrips);
        tabUpcoming = findViewById(R.id.tabUpcoming);
        tabPast = findViewById(R.id.tabPast);
        tabIndicator = findViewById(R.id.tabIndicator);
        btnMore = findViewById(R.id.btnMore);

        displayList = new ArrayList<>();
        allUpcoming = new ArrayList<>();
        allPast = new ArrayList<>();

        adapter = new DriverTripAdapter(displayList, this::confirmDelete);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupMoreButton(btnMore);

        tabUpcoming.setOnClickListener(v -> switchTab(true));
        tabPast.setOnClickListener(v -> switchTab(false));

        loadAllMyTrips();
    }

    private void switchTab(boolean upcoming) {
        showingUpcoming = upcoming;

        if (upcoming) {
            tabUpcoming.setTextColor(getResources().getColor(R.color.colorPrimaryText));
            tabUpcoming.setTypeface(null, Typeface.BOLD);
            tabPast.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            tabPast.setTypeface(null, Typeface.NORMAL);
            tabIndicator.setTranslationX(0);
            displayList.clear();
            displayList.addAll(allUpcoming);
        } else {
            tabPast.setTextColor(getResources().getColor(R.color.colorPrimaryText));
            tabPast.setTypeface(null, Typeface.BOLD);
            tabUpcoming.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            tabUpcoming.setTypeface(null, Typeface.NORMAL);
            float halfWidth = tabIndicator.getWidth();
            tabIndicator.setTranslationX(tabUpcoming.getWidth());
            displayList.clear();
            displayList.addAll(allPast);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (displayList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(showingUpcoming
                    ? "You have no upcoming trips."
                    : "You have no past trips.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void loadAllMyTrips() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        long now = System.currentTimeMillis();

        progressTrips.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("trips")
                .whereEqualTo("driverId", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressTrips.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            allUpcoming.clear();
                            allPast.clear();

                            for (DocumentSnapshot doc : task.getResult()) {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip == null) continue;
                                if (trip.getDateTime() >= now) {
                                    allUpcoming.add(trip);
                                } else {
                                    allPast.add(trip);
                                }
                            }

                            allUpcoming.sort((a, b) -> Long.compare(a.getDateTime(), b.getDateTime()));
                            allPast.sort((a, b) -> Long.compare(b.getDateTime(), a.getDateTime()));

                            switchTab(true);
                        } else {
                            Toast.makeText(MyTripsActivity.this,
                                    "Failed to load trips", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void confirmDelete(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Delete your trip from "
                        + trip.getFromLocation() + " to " + trip.getToLocation()
                        + "? This will also remove it from the live map.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTrip(trip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTrip(Trip trip) {
        db.collection("trips").document(trip.getTripId()).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MyTripsActivity.this,
                                    "Trip deleted", Toast.LENGTH_SHORT).show();
                            loadAllMyTrips();
                        } else {
                            Toast.makeText(MyTripsActivity.this,
                                    "Failed to delete: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}