package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myway.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    private TextView tvPhoneDisplay, tvCountdown, tvResend;
    private EditText etOtpCode;
    private Button btnVerify;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;

    private String mode, phone, name, email, password;
    private String userType, carModel, licensePlate, carCategory, activeRole;
    private ArrayList<String> roles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        bindViews();
        readExtras();

        tvPhoneDisplay.setText("Code sent to " + phone);

        sendOtp(null);

        btnVerify.setOnClickListener(v -> verifyEnteredCode());

        tvResend.setOnClickListener(v -> {
            if (canResend) {
                sendOtp(resendToken);
            }
        });
    }

    private void bindViews() {
        tvPhoneDisplay = findViewById(R.id.tvPhoneDisplay);
        tvCountdown    = findViewById(R.id.tvCountdown);
        tvResend       = findViewById(R.id.tvResend);
        etOtpCode      = findViewById(R.id.etOtpCode);
        btnVerify      = findViewById(R.id.btnVerify);
    }

    private void readExtras() {
        Bundle b     = getIntent().getExtras();
        mode         = b.getString("mode");
        phone        = b.getString("phone");
        name         = b.getString("name");
        email        = b.getString("email");
        password     = b.getString("password", "");
        userType     = b.getString("userType");
        carModel     = b.getString("carModel", "");
        licensePlate = b.getString("licensePlate", "");
        carCategory  = b.getString("carCategory", "Economy");
        activeRole   = b.getString("activeRole", userType);
        roles        = b.getStringArrayList("roles");

        if (roles == null || roles.isEmpty()) {
            roles = new ArrayList<>();
            if (userType != null) roles.add(userType);
        }
    }

    private String toInternationalFormat(String rawPhone) {
        String clean = rawPhone.replaceAll("\\s+", "");
        if (clean.startsWith("+")) return clean;
        if (clean.startsWith("0")) clean = clean.substring(1);
        return "+374" + clean;
    }

    private void sendOtp(PhoneAuthProvider.ForceResendingToken token) {
        String formatted = toInternationalFormat(phone);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(formatted)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        handleCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Could not send code: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String id,
                                           @NonNull PhoneAuthProvider.ForceResendingToken t) {
                        verificationId = id;
                        resendToken    = t;
                        startCountdown();
                    }
                });

        if (token != null) {
            builder.setForceResendingToken(token);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private void verifyEnteredCode() {
        String code = etOtpCode.getText().toString().trim();

        if (code.length() != 6) {
            etOtpCode.setError("Enter the 6-digit code");
            return;
        }
        if (verificationId == null) {
            Toast.makeText(this, "Please wait - sending code...", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, code);
        handleCredential(credential);
    }

    private void handleCredential(PhoneAuthCredential credential) {
        btnVerify.setEnabled(false);

        if ("google".equals(mode)) {
            mAuth.getCurrentUser()
                    .linkWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveToFirestore(mAuth.getCurrentUser().getUid());
                        } else {
                            btnVerify.setEnabled(true);
                            Toast.makeText(PhoneVerificationActivity.this,
                                    "Wrong code. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mAuth.signOut();
                            createEmailAccount();
                        } else {
                            btnVerify.setEnabled(true);
                            Toast.makeText(PhoneVerificationActivity.this,
                                    "Wrong code. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void createEmailAccount() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveToFirestore(mAuth.getCurrentUser().getUid());
                    } else {
                        btnVerify.setEnabled(true);
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Email already in use: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveToFirestore(String uid) {
        User newUser = new User(uid, name, email, phone, activeRole, roles);

        if (roles.contains("Driver")) {
            newUser.setCarModel(carModel);
            newUser.setLicensePlate(licensePlate);
            newUser.setCarCategory(carCategory);
        }

        db.collection("users").document(uid).set(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Intent intent = "Driver".equals(activeRole)
                                ? new Intent(PhoneVerificationActivity.this, DriverHomeActivity.class)
                                : new Intent(PhoneVerificationActivity.this, PassengerHomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        btnVerify.setEnabled(true);
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Could not save profile: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();

        canResend = false;
        tvResend.setAlpha(0.35f);
        tvResend.setClickable(false);
        tvCountdown.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long ms) {
                tvCountdown.setText("Resend in " + (ms / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                canResend = true;
                tvResend.setAlpha(1f);
                tvResend.setClickable(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}