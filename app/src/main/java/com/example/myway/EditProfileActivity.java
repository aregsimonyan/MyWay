package com.example.myway;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends MenuActivity {

    private TextInputEditText etEditName, etEditPhone, etEditCarModel, etEditLicensePlate;
    private RadioGroup rgEditCarCategory;
    private RadioButton rbEditEconomy, rbEditComfort, rbEditBusiness;
    private LinearLayout layoutCarFields;
    private MaterialButton btnSaveProfile;
    private ProgressBar progressSave;
    private ImageButton btnMore;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth     = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etEditName         = findViewById(R.id.etEditName);
        etEditPhone        = findViewById(R.id.etEditPhone);
        etEditCarModel     = findViewById(R.id.etEditCarModel);
        etEditLicensePlate = findViewById(R.id.etEditLicensePlate);
        rgEditCarCategory  = findViewById(R.id.rgEditCarCategory);
        rbEditEconomy      = findViewById(R.id.rbEditEconomy);
        rbEditComfort      = findViewById(R.id.rbEditComfort);
        rbEditBusiness     = findViewById(R.id.rbEditBusiness);
        layoutCarFields    = findViewById(R.id.layoutCarFields);
        btnSaveProfile     = findViewById(R.id.btnSaveProfile);
        progressSave       = findViewById(R.id.progressSave);
        btnMore            = findViewById(R.id.btnMore);

        etEditLicensePlate.setFilters(new InputFilter[]{
                LicensePlateUtils.buildFilter(),
                LicensePlateUtils.buildLengthFilter()
        });

        setupMoreButton(btnMore);
        loadCurrentData();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadCurrentData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        progressSave.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        firestore.collection("users").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        progressSave.setVisibility(View.GONE);
                        btnSaveProfile.setEnabled(true);

                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot doc = task.getResult();

                            etEditName.setText(doc.getString("name"));
                            etEditPhone.setText(doc.getString("phone"));

                            List<String> roles = (List<String>) doc.get("roles");
                            boolean isDriver = roles != null && roles.contains("Driver");

                            if (isDriver) {
                                layoutCarFields.setVisibility(View.VISIBLE);
                                etEditCarModel.setText(doc.getString("carModel"));
                                etEditLicensePlate.setText(doc.getString("licensePlate"));

                                String category = doc.getString("carCategory");
                                if ("Comfort".equals(category))       rbEditComfort.setChecked(true);
                                else if ("Business".equals(category)) rbEditBusiness.setChecked(true);
                                else                                   rbEditEconomy.setChecked(true);
                            } else {
                                layoutCarFields.setVisibility(View.GONE);
                            }
                        } else {
                            Toast.makeText(EditProfileActivity.this,
                                    "Could not load profile data.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveProfile() {
        String name  = etEditName.getText()  != null ? etEditName.getText().toString().trim()  : "";
        String phone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etEditName.setError("Name cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etEditPhone.setError("Phone cannot be empty");
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",  name);
        updates.put("phone", phone);

        if (layoutCarFields.getVisibility() == View.VISIBLE) {
            String carModel     = etEditCarModel.getText()     != null ? etEditCarModel.getText().toString().trim()     : "";
            String licensePlate = etEditLicensePlate.getText() != null ? etEditLicensePlate.getText().toString().trim() : "";

            if (TextUtils.isEmpty(carModel)) {
                etEditCarModel.setError("Car model cannot be empty");
                return;
            }
            if (!LicensePlateUtils.isValid(licensePlate)) {
                etEditLicensePlate.setError(LicensePlateUtils.formatError());
                return;
            }

            String carCategory = "Economy";
            if (rbEditComfort.isChecked())  carCategory = "Comfort";
            if (rbEditBusiness.isChecked()) carCategory = "Business";

            updates.put("carModel",     carModel);
            updates.put("licensePlate", licensePlate);
            updates.put("carCategory",  carCategory);
        }

        progressSave.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        firestore.collection("users").document(uid).update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressSave.setVisibility(View.GONE);
                        btnSaveProfile.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(EditProfileActivity.this,
                                    "Profile updated.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this,
                                    "Could not save: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}