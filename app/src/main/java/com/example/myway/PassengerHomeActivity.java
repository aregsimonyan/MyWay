package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.adapters.TripAdapter;
import com.example.myway.models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PassengerHomeActivity extends MenuActivity {

    private AutoCompleteTextView etSearchFrom;
    private AutoCompleteTextView etSearchTo;
    private MaterialButton btnSearch;
    private MaterialButton btnOpenMap;
    private MaterialButton btnMyRequests;
    private MaterialButton btnMyBookings;
    private MaterialButton btnPostRequest;
    private ImageButton btnMore;
    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private TextView tvTripCount;
    private TextView tvEmptyTrips;
    private ProgressBar progressTrips;

    private List<Trip> tripList;
    private List<Trip> allTripList;

    private ArrayAdapter<String> fromSuggestionsAdapter;
    private ArrayAdapter<String> toSuggestionsAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_home);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etSearchFrom   = findViewById(R.id.etSearchFrom);
        etSearchTo     = findViewById(R.id.etSearchTo);
        btnSearch      = findViewById(R.id.btnSearch);
        btnOpenMap     = findViewById(R.id.btnOpenMap);
        btnMyRequests  = findViewById(R.id.btnMyRequests);
        btnMyBookings  = findViewById(R.id.btnMyBookings);
        btnPostRequest = findViewById(R.id.btnPostRequest);
        btnMore        = findViewById(R.id.btnMore);
        recyclerView   = findViewById(R.id.recyclerViewTrips);
        tvTripCount    = findViewById(R.id.tvTripCount);
        tvEmptyTrips   = findViewById(R.id.tvEmptyTrips);
        progressTrips  = findViewById(R.id.progressTrips);

        tripList    = new ArrayList<>();
        allTripList = new ArrayList<>();

        fromSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        toSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());

        etSearchFrom.setAdapter(fromSuggestionsAdapter);
        etSearchTo.setAdapter(toSuggestionsAdapter);

        etSearchFrom.setOnItemClickListener((parent, view, position, id) -> searchTrips());
        etSearchTo.setOnItemClickListener((parent, view, position, id) -> searchTrips());

        String currentUserId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        adapter = new TripAdapter(tripList, currentUserId, new TripAdapter.OnTripActionListener() {
            @Override
            public void onCardClick(Trip trip) {
                openTripOnMap(trip);
            }

            @Override
            public void onBookClick(Trip trip) {
                confirmBooking(trip);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        btnSearch.setOnClickListener(v -> searchTrips());

        btnOpenMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));

        btnMyRequests.setOnClickListener(v ->
                startActivity(new Intent(this, MyRequestsActivity.class)));

        btnMyBookings.setOnClickListener(v ->
                startActivity(new Intent(this, MyBookingsActivity.class)));

        btnPostRequest.setOnClickListener(v ->
                startActivity(new Intent(this, PostPassengerRequestActivity.class)));

        setupMoreButton(btnMore);
        loadAllTrips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllTrips();
    }

    private void openTripOnMap(Trip trip) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("tripId", trip.getTripId());
        startActivity(intent);
    }

    private void loadAllTrips() {
        progressTrips.setVisibility(View.VISIBLE);
        tvEmptyTrips.setVisibility(View.GONE);
        tvTripCount.setText("Loading trips...");

        db.collection("trips")
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressTrips.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            allTripList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip != null && trip.getSeatsAvailable() > 0) {
                                    allTripList.add(trip);
                                }
                            }

                            tripList.clear();
                            tripList.addAll(allTripList);
                            adapter.notifyDataSetChanged();

                            int count = allTripList.size();
                            tvTripCount.setText(count + " ride"
                                    + (count == 1 ? "" : "s") + " available");

                            if (tripList.isEmpty()) {
                                tvEmptyTrips.setText("No trips available right now.");
                                tvEmptyTrips.setVisibility(View.VISIBLE);
                            }

                            refreshSuggestions();

                        } else {
                            tvTripCount.setText("Could not load trips");
                        }
                    }
                });
    }

    private void refreshSuggestions() {
        Set<String> fromSet = new LinkedHashSet<>();
        Set<String> toSet   = new LinkedHashSet<>();

        for (Trip trip : allTripList) {
            if (trip.getFromLocation() != null && !trip.getFromLocation().isEmpty()) {
                fromSet.add(trip.getFromLocation());
            }
            if (trip.getToLocation() != null && !trip.getToLocation().isEmpty()) {
                toSet.add(trip.getToLocation());
            }
        }

        fromSuggestionsAdapter.clear();
        fromSuggestionsAdapter.addAll(new ArrayList<>(fromSet));
        fromSuggestionsAdapter.notifyDataSetChanged();

        toSuggestionsAdapter.clear();
        toSuggestionsAdapter.addAll(new ArrayList<>(toSet));
        toSuggestionsAdapter.notifyDataSetChanged();
    }

    private void searchTrips() {
        String from = etSearchFrom.getText().toString().trim().toLowerCase();
        String to   = etSearchTo.getText().toString().trim().toLowerCase();

        tripList.clear();
        for (Trip trip : allTripList) {
            boolean fromMatch = from.isEmpty()
                    || trip.getFromLocation().toLowerCase().contains(from);
            boolean toMatch = to.isEmpty()
                    || trip.getToLocation().toLowerCase().contains(to);
            if (fromMatch && toMatch) {
                tripList.add(trip);
            }
        }
        adapter.notifyDataSetChanged();

        if (tripList.isEmpty()) {
            tvEmptyTrips.setText("No trips found for this route.\nTry different keywords.");
            tvEmptyTrips.setVisibility(View.VISIBLE);
        } else {
            tvEmptyTrips.setVisibility(View.GONE);
        }

        tvTripCount.setText(tripList.size() + " result"
                + (tripList.size() == 1 ? "" : "s") + " found");
    }

    private void confirmBooking(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Book a seat from " + trip.getFromLocation()
                        + " to " + trip.getToLocation()
                        + " for " + (int) trip.getPricePerSeat() + " AMD?")
                .setPositiveButton("Book Now", (dialog, which) -> executeBooking(trip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeBooking(Trip trip) {
        final DocumentReference tripRef =
                db.collection("trips").document(trip.getTripId());
        final String passengerId = mAuth.getCurrentUser().getUid();

        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction)
                    throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(tripRef);
                long newSeats = snapshot.getLong("seatsAvailable") - 1;
                if (newSeats < 0) {
                    throw new FirebaseFirestoreException("Trip is full",
                            FirebaseFirestoreException.Code.ABORTED);
                }
                transaction.update(tripRef, "seatsAvailable", newSeats);
                transaction.update(tripRef, "passengerIds",
                        FieldValue.arrayUnion(passengerId));
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(PassengerHomeActivity.this,
                            BookedTripActivity.class);
                    intent.putExtra("tripId", trip.getTripId());
                    startActivity(intent);
                    loadAllTrips();
                } else {
                    Toast.makeText(PassengerHomeActivity.this,
                            "Booking failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}