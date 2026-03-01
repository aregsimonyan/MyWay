package com.example.myway;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends MenuActivity {

    private TextView tvName, tvEmail, tvPhone, tvType, tvCar;
    private ImageButton btnMore;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvType = findViewById(R.id.tvProfileType);
        tvCar = findViewById(R.id.tvProfileCar);
        btnMore = findViewById(R.id.btnMore);

        loadProfileData();
        setupMoreButton(btnMore);
    }

    private void loadProfileData() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot doc = task.getResult();
                            tvName.setText("Name: " + doc.getString("name"));
                            tvEmail.setText("Email: " + doc.getString("email"));
                            tvPhone.setText("Phone: " + doc.getString("phone"));

                            String type = doc.getString("userType");
                            tvType.setText("Type: " + type);

                            if ("Driver".equals(type)) {
                                tvCar.setVisibility(View.VISIBLE);
                                tvCar.setText("Car: " + doc.getString("carModel") + " (" + doc.getString("licensePlate") + ")");
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}