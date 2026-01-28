package com.example.myway;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.myway.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etCarModel, etLicensePlate;
    private RadioGroup rgUserType, rgCarCategory;
    private RadioButton rbDriver, rbEconomy, rbComfort, rbBusiness;
    private LinearLayout driverFieldsLayout;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etCarModel = findViewById(R.id.etCarModel);
        etLicensePlate = findViewById(R.id.etLicensePlate);

        rgUserType = findViewById(R.id.rgUserType);
        rgCarCategory = findViewById(R.id.rgCarCategory);

        rbDriver = findViewById(R.id.rbDriver);
        rbEconomy = findViewById(R.id.rbEconomy);
        rbComfort = findViewById(R.id.rbComfort);
        rbBusiness = findViewById(R.id.rbBusiness);

        driverFieldsLayout = findViewById(R.id.driverFieldsLayout);
        btnRegister = findViewById(R.id.btnRegister);

        rgUserType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbDriver) {
                    driverFieldsLayout.setVisibility(View.VISIBLE);
                } else {
                    driverFieldsLayout.setVisibility(View.GONE);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isDriver = rbDriver.isChecked();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDriver) {
            if (TextUtils.isEmpty(etCarModel.getText()) || TextUtils.isEmpty(etLicensePlate.getText())) {
                Toast.makeText(this, "Drivers must fill car details", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            saveUserToFirestore(firebaseUser);
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String userType = rbDriver.isChecked() ? "Driver" : "Passenger";

        User newUser = new User(uid, name, email, phone, userType);

        if (userType.equals("Driver")) {
            newUser.setCarModel(etCarModel.getText().toString().trim());
            newUser.setLicensePlate(etLicensePlate.getText().toString().trim());

            String category = "Economy";
            if (rbComfort.isChecked()) category = "Comfort";
            if (rbBusiness.isChecked()) category = "Business";

            newUser.setCarCategory(category);
        }

        db.collection("users").document(uid).set(newUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}