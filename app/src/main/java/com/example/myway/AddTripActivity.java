package com.example.myway;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myway.models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTripActivity extends AppCompatActivity {

    private Spinner spinnerFrom, spinnerTo;
    private EditText etDate, etTime, etPrice, etSeats;
    private Button btnPublish;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Calendar calendar;

    private String driverName, licensePlate, carCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPrice = findViewById(R.id.etPrice);
        etSeats = findViewById(R.id.etSeats);
        btnPublish = findViewById(R.id.btnPublish);

        setupSpinners();
        fetchDriverDetails();

        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        etTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishTrip();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
    }

    private void fetchDriverDetails() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            driverName = document.getString("name");
                            licensePlate = document.getString("licensePlate");
                            carCategory = document.getString("carCategory");
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
                updateDateLabel();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                updateTimeLabel();
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        etDate.setText(sdf.format(calendar.getTime()));
    }

    private void updateTimeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
        etTime.setText(sdf.format(calendar.getTime()));
    }

    private void publishTrip() {
        String from = spinnerFrom.getSelectedItem().toString();
        String to = spinnerTo.getSelectedItem().toString();
        String priceStr = etPrice.getText().toString();
        String seatsStr = etSeats.getText().toString();

        if (TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(seatsStr) || TextUtils.isEmpty(etDate.getText())) {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        if (from.equals(to)) {
            Toast.makeText(this, "Destination cannot be the same as Start", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int seats = Integer.parseInt(seatsStr);
        long tripTime = calendar.getTimeInMillis();

        String tripId = db.collection("trips").document().getId();
        String driverId = mAuth.getCurrentUser().getUid();

        Trip newTrip = new Trip(tripId, driverId, driverName, licensePlate, from, to, tripTime, price, seats, carCategory);

        db.collection("trips").document(tripId).set(newTrip)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AddTripActivity.this, "Trip Published Successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(AddTripActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}