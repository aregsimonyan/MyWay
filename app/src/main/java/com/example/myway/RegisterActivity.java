package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etCarModel, etLicensePlate;
    private RadioGroup rgUserType, rgCarCategory;
    private RadioButton rbDriver, rbComfort, rbBusiness;
    private LinearLayout driverFieldsLayout;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bindViews();

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            driverFieldsLayout.setVisibility(
                    checkedId == R.id.rbDriver ? View.VISIBLE : View.GONE
            );
        });

        btnRegister.setOnClickListener(v -> validateAndProceed());
    }

    private void bindViews() {
        etName          = findViewById(R.id.etName);
        etEmail         = findViewById(R.id.etEmail);
        etPhone         = findViewById(R.id.etPhone);
        etPassword      = findViewById(R.id.etPassword);
        etCarModel      = findViewById(R.id.etCarModel);
        etLicensePlate  = findViewById(R.id.etLicensePlate);
        rgUserType      = findViewById(R.id.rgUserType);
        rgCarCategory   = findViewById(R.id.rgCarCategory);
        rbDriver        = findViewById(R.id.rbDriver);
        rbComfort       = findViewById(R.id.rbComfort);
        rbBusiness      = findViewById(R.id.rbBusiness);
        driverFieldsLayout = findViewById(R.id.driverFieldsLayout);
        btnRegister     = findViewById(R.id.btnRegister);
    }

    private void validateAndProceed() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isDriver = rbDriver.isChecked();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (isDriver) {
            if (TextUtils.isEmpty(etCarModel.getText())) {
                etCarModel.setError("Car model is required for drivers");
                return;
            }
            if (TextUtils.isEmpty(etLicensePlate.getText())) {
                etLicensePlate.setError("License plate is required for drivers");
                return;
            }
        }

        String userType = isDriver ? "Driver" : "Passenger";
        String carModel = isDriver ? etCarModel.getText().toString().trim() : "";
        String licensePlate = isDriver ? etLicensePlate.getText().toString().trim() : "";

        String carCategory = "Economy";
        if (rbComfort.isChecked()) carCategory = "Comfort";
        if (rbBusiness.isChecked()) carCategory = "Business";

        // All fields are valid — launch phone verification.
        // The actual Firebase account is created AFTER the phone is verified.
        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra("mode",         "email");
        intent.putExtra("phone",        phone);
        intent.putExtra("name",         name);
        intent.putExtra("email",        email);
        intent.putExtra("password",     password);
        intent.putExtra("userType",     userType);
        intent.putExtra("carModel",     carModel);
        intent.putExtra("licensePlate", licensePlate);
        intent.putExtra("carCategory",  carCategory);
        startActivity(intent);
    }
}