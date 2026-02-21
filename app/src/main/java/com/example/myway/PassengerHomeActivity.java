package com.example.myway;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Locale;

public class PassengerHomeActivity extends AppCompatActivity {

    private Spinner spinnerFrom, spinnerTo;
    private Button btnSearch, btnMap;
    private ImageButton btnMore;
    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private List<Trip> tripList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences languagePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        languagePrefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        loadLocale();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_home);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupSpinners();
        setupClickListeners();
        setupMoreButton();
        loadAllTrips();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        spinnerFrom = findViewById(R.id.spinnerSearchFrom);
        spinnerTo = findViewById(R.id.spinnerSearchTo);
        btnSearch = findViewById(R.id.btnSearch);
        btnMap = findViewById(R.id.btnOpenMap);
        btnMore = findViewById(R.id.btnMore);
        recyclerView = findViewById(R.id.recyclerViewTrips);
        tripList = new ArrayList<>();
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

    private void setupSpinners() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.cities_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(spinnerAdapter);
        spinnerTo.setAdapter(spinnerAdapter);
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
    }

    private void setupMoreButton() {
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(PassengerHomeActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.common_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.action_account) {
                            startActivity(new Intent(PassengerHomeActivity.this, ProfileActivity.class));
                            return true;
                        } else if (id == R.id.action_language) {
                            showLanguageDialog();
                            return true;
                        } else if (id == R.id.action_logout) {
                            showLogoutConfirmation();
                            return true;
                        }
                        return false;
                    }
                });
                popup.show();
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
                            tripList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip != null && trip.getSeatsAvailable() > 0) {
                                    tripList.add(trip);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void searchTrips() {
        String from = spinnerFrom.getSelectedItem().toString();
        String to = spinnerTo.getSelectedItem().toString();

        db.collection("trips")
                .whereEqualTo("fromLocation", from)
                .whereEqualTo("toLocation", to)
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            tripList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Trip trip = doc.toObject(Trip.class);
                                if (trip != null && trip.getSeatsAvailable() > 0) {
                                    tripList.add(trip);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            if (tripList.isEmpty()) {
                                Toast.makeText(PassengerHomeActivity.this, "No trips found.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void confirmBooking(Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Book a seat to " + trip.getToLocation() + " for " + trip.getPricePerSeat() + " AMD?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeBooking(trip);
                    }
                })
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
                double newSeats = snapshot.getLong("seatsAvailable") - 1;

                if (newSeats < 0) {
                    throw new FirebaseFirestoreException("Trip is full", FirebaseFirestoreException.Code.ABORTED);
                }

                transaction.update(tripRef, "seatsAvailable", newSeats);
                transaction.update(tripRef, "passengerIds", FieldValue.arrayUnion(passengerId));
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(PassengerHomeActivity.this, "Booking Confirmed!", Toast.LENGTH_LONG).show();
                    loadAllTrips();
                } else {
                    Toast.makeText(PassengerHomeActivity.this, "Booking Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "Русский", "Հայերեն"};
        final String[] languageCodes = {"en", "ru", "hy"};

        int currentSelection = getCurrentLanguageIndex(languageCodes);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language");
        builder.setSingleChoiceItems(languages, currentSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLocale(languageCodes[which]);
                dialog.dismiss();
                recreate();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private int getCurrentLanguageIndex(String[] codes) {
        String currentLang = languagePrefs.getString("language", "en");
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) return i;
        }
        return 0;
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        SharedPreferences.Editor editor = languagePrefs.edit();
        editor.putString("language", languageCode);
        editor.apply();
    }

    private void loadLocale() {
        String languageCode = languagePrefs.getString("language", "en");
        setLocale(languageCode);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performLogout();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}