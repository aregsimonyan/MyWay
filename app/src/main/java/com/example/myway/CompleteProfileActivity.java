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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CompleteProfileActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private EditText etPhone, etCarModel, etLicensePlate;
    private RadioGroup rgUserType, rgCarCategory;
    private RadioButton rbDriver, rbComfort, rbBusiness;
    private LinearLayout driverFieldsLayout;
    private Button btnSave;

    private String googleName, googleEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completeprofile);

        googleName  = getIntent().getStringExtra("name");
        googleEmail = getIntent().getStringExtra("email");

        bindViews();

        tvWelcomeName.setText("Hi, " + googleName + "!\nJust a few more details.");

        rgUserType.setOnCheckedChangeListener((group, checkedId) ->
                driverFieldsLayout.setVisibility(
                        checkedId == R.id.rbDriver ? View.VISIBLE : View.GONE
                )
        );

        btnSave.setOnClickListener(v -> validateAndProceed());
    }

    private void bindViews() {
        tvWelcomeName      = findViewById(R.id.tvWelcomeName);
        etPhone            = findViewById(R.id.etPhone);
        etCarModel         = findViewById(R.id.etCarModel);
        etLicensePlate     = findViewById(R.id.etLicensePlate);
        rgUserType         = findViewById(R.id.rgUserType);
        rgCarCategory      = findViewById(R.id.rgCarCategory);
        rbDriver           = findViewById(R.id.rbDriver);
        rbComfort          = findViewById(R.id.rbComfort);
        rbBusiness         = findViewById(R.id.rbBusiness);
        driverFieldsLayout = findViewById(R.id.driverFieldsLayout);
        btnSave            = findViewById(R.id.btnSave);
    }

    private void validateAndProceed() {
        String phone     = etPhone.getText().toString().trim();
        boolean isDriver = rbDriver.isChecked();

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
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

        String userType     = isDriver ? "Driver" : "Passenger";
        String carModel     = isDriver ? etCarModel.getText().toString().trim() : "";
        String licensePlate = isDriver ? etLicensePlate.getText().toString().trim() : "";

        String carCategory = "Economy";
        if (rbComfort.isChecked())  carCategory = "Comfort";
        if (rbBusiness.isChecked()) carCategory = "Business";


        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra("mode",         "google");
        intent.putExtra("phone",        phone);
        intent.putExtra("name",         googleName);
        intent.putExtra("email",        googleEmail);
        intent.putExtra("userType",     userType);
        intent.putExtra("carModel",     carModel);
        intent.putExtra("licensePlate", licensePlate);
        intent.putExtra("carCategory",  carCategory);
        startActivity(intent);
    }
}