package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.adapters.TripAdapter;
import com.example.myway.models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;

public class PassengerHomeActivity extends MenuActivity {

    private EditText etSearchFrom;
    private EditText etSearchTo;
    private Button btnSearch;
    private Button btnMap;
    private Button btnPostRequest;
    private Button btnMyRequests;
    private ImageButton btnMore;
    private RecyclerView recyclerView;
    private TripAdapter adapter;

    private List<Trip> tripList;
    private List<Trip> allTripList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_home);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        setupMoreButton(btnMore);
        loadAllTrips();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        etSearchFrom = findViewById(R.id.etSearchFrom);
        etSearchTo = findViewById(R.id.etSearchTo);
        btnSearch = findViewById(R.id.btnSearch);
        btnMap = findViewById(R.id.btnOpenMap);
        btnPostRequest = findViewById(R.id.btnPostRequest);
        btnMyRequests = findViewById(R.id.btnMyRequests);
        btnMore = findViewById(R.id.btnMore);
        recyclerView = findViewById(R.id.recyclerViewTrips);
        tripList = new ArrayList<>();
        allTripList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripAdapter(tripList, new TripAdapter.OnTripClickListener() {
            @Override
            public void onBookClick(Trip trip) {
                confirmBooking(trip);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTrips();
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PassengerHomeActivity.this, MapActivity.class));
            }
        });

        btnPostRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PassengerHomeActivity.this,
                        PostPassengerRequestActivity.class));
            }
        });

        btnMyRequests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PassengerHomeActivity.this, MyRequestsActivity.class));
            }
        });
    }

    private void loadAllTrips() {
        db.collection("trips")
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
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
                        }
                    }
                });
    }

    private void searchTrips() {
        String from = etSearchFrom.getText().toString().trim().toLowerCase();
        String to = etSearchTo.getText().toString().trim().toLowerCase();

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
            Toast.makeText(this, "No trips found for this route.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmBooking(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Book a seat to " + trip.getToLocation()
                        + " for " + (int) trip.getPricePerSeat() + " AMD?")
                .setPositiveButton("Yes", (dialog, which) -> executeBooking(trip))
                .setNegativeButton("No", null)
                .show();
    }

    private void executeBooking(Trip trip) {
        final DocumentReference tripRef = db.collection("trips").document(trip.getTripId());
        final String passengerId = mAuth.getCurrentUser().getUid();

        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(tripRef);
                long newSeats = snapshot.getLong("seatsAvailable") - 1;

                if (newSeats < 0) {
                    throw new FirebaseFirestoreException("Trip is full",
                            FirebaseFirestoreException.Code.ABORTED);
                }

                transaction.update(tripRef, "seatsAvailable", newSeats);
                transaction.update(tripRef, "passengerIds", FieldValue.arrayUnion(passengerId));
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(PassengerHomeActivity.this,
                            "Booking Confirmed!", Toast.LENGTH_LONG).show();
                    loadAllTrips();
                } else {
                    Toast.makeText(PassengerHomeActivity.this,
                            "Booking Failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}