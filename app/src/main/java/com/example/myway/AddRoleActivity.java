package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myway.utils.LicensePlateUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddRoleActivity extends MenuActivity {

    private TextInputEditText etAddCarModel, etAddLicensePlate;
    private RadioGroup rgAddCarCategory;
    private RadioButton rbAddComfort, rbAddBusiness;
    private LinearLayout layoutAddDriverFields;
    private MaterialButton btnConfirmAddRole;
    private ProgressBar progressAddRole;
    private ImageButton btnMore;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String roleToAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_role);

        mAuth     = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        roleToAdd = getIntent().getStringExtra("roleToAdd");
        if (roleToAdd == null) roleToAdd = "Driver";

        etAddCarModel         = findViewById(R.id.etAddCarModel);
        etAddLicensePlate     = findViewById(R.id.etAddLicensePlate);
        rgAddCarCategory      = findViewById(R.id.rgAddCarCategory);
        rbAddComfort          = findViewById(R.id.rbAddComfort);
        rbAddBusiness         = findViewById(R.id.rbAddBusiness);
        layoutAddDriverFields = findViewById(R.id.layoutAddDriverFields);
        btnConfirmAddRole     = findViewById(R.id.btnConfirmAddRole);
        progressAddRole       = findViewById(R.id.progressAddRole);
        btnMore               = findViewById(R.id.btnMore);

        etAddLicensePlate.setFilters(new InputFilter[]{
                LicensePlateUtils.buildFilter(),
                LicensePlateUtils.buildLengthFilter()
        });

        setupMoreButton(btnMore);

        android.widget.TextView tvAddRoleTitle    = findViewById(R.id.tvAddRoleTitle);
        android.widget.TextView tvAddRoleSubtitle = findViewById(R.id.tvAddRoleSubtitle);

        if ("Driver".equals(roleToAdd)) {
            tvAddRoleTitle.setText("Become a Driver");
            tvAddRoleSubtitle.setText("Add your vehicle details to start offering rides.");
            layoutAddDriverFields.setVisibility(View.VISIBLE);
        } else {
            tvAddRoleTitle.setText("Add Passenger Account");
            tvAddRoleSubtitle.setText("You can now also book rides as a passenger.");
            layoutAddDriverFields.setVisibility(View.GONE);
        }

        btnConfirmAddRole.setOnClickListener(v -> validateAndAddRole());
    }

    private void validateAndAddRole() {
        if ("Driver".equals(roleToAdd)) {
            String carModel     = etAddCarModel.getText()     != null ? etAddCarModel.getText().toString().trim()     : "";
            String licensePlate = etAddLicensePlate.getText() != null ? etAddLicensePlate.getText().toString().trim() : "";

            if (TextUtils.isEmpty(carModel)) {
                etAddCarModel.setError("Car model is required");
                return;
            }
            if (!LicensePlateUtils.isValid(licensePlate)) {
                etAddLicensePlate.setError(LicensePlateUtils.formatError());
                return;
            }
        }

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        progressAddRole.setVisibility(View.VISIBLE);
        btnConfirmAddRole.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("roles",      FieldValue.arrayUnion(roleToAdd));
        updates.put("activeRole", roleToAdd);

        if ("Driver".equals(roleToAdd)) {
            String carModel     = etAddCarModel.getText().toString().trim();
            String licensePlate = etAddLicensePlate.getText().toString().trim();
            String carCategory  = "Economy";
            if (rbAddComfort.isChecked())  carCategory = "Comfort";
            if (rbAddBusiness.isChecked()) carCategory = "Business";

            updates.put("carModel",     carModel);
            updates.put("licensePlate", licensePlate);
            updates.put("carCategory",  carCategory);
        }

        firestore.collection("users").document(uid).update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressAddRole.setVisibility(View.GONE);
                        btnConfirmAddRole.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(AddRoleActivity.this,
                                    roleToAdd + " account added.", Toast.LENGTH_SHORT).show();

                            Intent intent = "Driver".equals(roleToAdd)
                                    ? new Intent(AddRoleActivity.this, DriverHomeActivity.class)
                                    : new Intent(AddRoleActivity.this, PassengerHomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(AddRoleActivity.this,
                                    "Could not add role: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}