package com.myway.myway;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.myway.myway.utils.LicensePlateUtils;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etCarModel, etLicensePlate;
    private RadioGroup rgUserType, rgCarCategory;
    private RadioButton rbDriver, rbComfort, rbBusiness;
    private CheckBox cbAlsoBoth;
    private LinearLayout driverFieldsLayout;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        bindViews();

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isDriverSelected = (checkedId == R.id.rbDriver);
            cbAlsoBoth.setText(isDriverSelected
                    ? "Also register as a Passenger"
                    : "Also register as a Driver");
            updateDriverFieldsVisibility();
        });

        cbAlsoBoth.setOnCheckedChangeListener((btn, isChecked) -> updateDriverFieldsVisibility());

        btnRegister.setOnClickListener(v -> validateAndProceed());
    }

    private void bindViews() {
        etName             = findViewById(R.id.etName);
        etEmail            = findViewById(R.id.etEmail);
        etPhone            = findViewById(R.id.etPhone);
        etPassword         = findViewById(R.id.etPassword);
        etCarModel         = findViewById(R.id.etCarModel);
        etLicensePlate     = findViewById(R.id.etLicensePlate);
        rgUserType         = findViewById(R.id.rgUserType);
        rgCarCategory      = findViewById(R.id.rgCarCategory);
        rbDriver           = findViewById(R.id.rbDriver);
        rbComfort          = findViewById(R.id.rbComfort);
        rbBusiness         = findViewById(R.id.rbBusiness);
        cbAlsoBoth         = findViewById(R.id.cbAlsoBoth);
        driverFieldsLayout = findViewById(R.id.driverFieldsLayout);
        btnRegister        = findViewById(R.id.btnRegister);

        etLicensePlate.setFilters(new InputFilter[]{
                LicensePlateUtils.buildFilter(),
                LicensePlateUtils.buildLengthFilter()
        });
    }

    private void updateDriverFieldsVisibility() {
        boolean isDriver   = rbDriver.isChecked();
        boolean alsoDriver = !isDriver && cbAlsoBoth.isChecked();
        driverFieldsLayout.setVisibility((isDriver || alsoDriver) ? View.VISIBLE : View.GONE);
    }

    private void validateAndProceed() {
        String name      = etName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();
        boolean isDriver = rbDriver.isChecked();
        boolean hasBothRoles     = cbAlsoBoth.isChecked();
        boolean needsDriverDetails = isDriver || (hasBothRoles && !isDriver);

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
        if (needsDriverDetails) {
            if (TextUtils.isEmpty(etCarModel.getText())) {
                etCarModel.setError("Car model is required for drivers");
                return;
            }
            if (!LicensePlateUtils.isValid(etLicensePlate.getText().toString())) {
                etLicensePlate.setError(LicensePlateUtils.formatError());
                return;
            }
        }

        String primaryRole  = isDriver ? "Driver" : "Passenger";
        String carModel     = needsDriverDetails ? etCarModel.getText().toString().trim() : "";
        String licensePlate = needsDriverDetails ? etLicensePlate.getText().toString().trim() : "";

        String carCategory = "Economy";
        if (rbComfort.isChecked())  carCategory = "Comfort";
        if (rbBusiness.isChecked()) carCategory = "Business";

        ArrayList<String> roles = new ArrayList<>();
        roles.add(primaryRole);
        if (hasBothRoles) {
            roles.add(isDriver ? "Passenger" : "Driver");
        }

        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra("mode",         "email");
        intent.putExtra("phone",        phone);
        intent.putExtra("name",         name);
        intent.putExtra("email",        email);
        intent.putExtra("password",     password);
        intent.putExtra("userType",     primaryRole);
        intent.putExtra("activeRole",   primaryRole);
        intent.putExtra("carModel",     carModel);
        intent.putExtra("licensePlate", licensePlate);
        intent.putExtra("carCategory",  carCategory);
        intent.putStringArrayListExtra("roles", roles);
        startActivity(intent);
    }
}