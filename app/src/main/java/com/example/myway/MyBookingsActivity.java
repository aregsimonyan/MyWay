package com.example.myway;

import android.content.Intent;
import android.graphics.Typeface;
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

    private List<Trip> displayList;
    private List<Trip> allUpcoming;
    private List<Trip> allPast;

    private ProgressBar progressBookings;
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
        setContentView(R.layout.activity_my_bookings);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView     = findViewById(R.id.recyclerViewBookings);
        progressBookings = findViewById(R.id.progressBookings);
        tvEmpty          = findViewById(R.id.tvEmptyBookings);
        tabUpcoming      = findViewById(R.id.tabUpcoming);
        tabPast          = findViewById(R.id.tabPast);
        tabIndicator     = findViewById(R.id.tabIndicator);
        btnMore          = findViewById(R.id.btnMore);

        displayList  = new ArrayList<>();
        allUpcoming  = new ArrayList<>();
        allPast      = new ArrayList<>();

        String currentUserId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        adapter = new TripAdapter(displayList, currentUserId,
                new TripAdapter.OnTripActionListener() {
                    @Override
                    public void onCardClick(Trip trip) {
                        // For upcoming trips open the full booking detail screen;
                        // for past trips the same screen shows the expired countdown
                        Intent intent = new Intent(MyBookingsActivity.this,
                                BookedTripActivity.class);
                        intent.putExtra("tripId", trip.getTripId());
                        startActivity(intent);
                    }

                    @Override
                    public void onBookClick(Trip trip) {
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupMoreButton(btnMore);

        tabUpcoming.setOnClickListener(v -> switchTab(true));
        tabPast.setOnClickListener(v -> switchTab(false));

        loadAllMyBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllMyBookings();
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
                    ? "You have no upcoming bookings."
                    : "You have no past bookings.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void loadAllMyBookings() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        long now = System.currentTimeMillis();

        progressBookings.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("trips")
                .whereArrayContains("passengerIds", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressBookings.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            allUpcoming.clear();
                            allPast.clear();

                            for (DocumentSnapshot doc : task.getResult()) {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip == null) continue;

                                if (trip.getDateTime() > now) {
                                    allUpcoming.add(trip);
                                } else {
                                    allPast.add(trip);
                                }
                            }

                            allUpcoming.sort((a, b) ->
                                    Long.compare(a.getDateTime(), b.getDateTime()));
                            allPast.sort((a, b) ->
                                    Long.compare(b.getDateTime(), a.getDateTime()));

                            switchTab(showingUpcoming);

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