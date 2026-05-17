package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myway.models.Rating;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RatingActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "tripId";
    public static final String EXTRA_RATED_USER_ID = "ratedUserId";
    public static final String EXTRA_RATED_USER_NAME = "ratedUserName";
    public static final String EXTRA_RATER_TYPE = "raterType";

    private TextView tvTitle;
    private TextView tvSubtitle;
    private ImageView ivAvatar;
    private TextView tvAvatarInitials;
    private RatingBar ratingBar;
    private TextView tvRatingLabel;
    private ProgressBar progressSubmit;
    private View btnSubmit;
    private View btnSkip;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String tripId;
    private String ratedUserId;
    private String ratedUserName;
    private String raterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        ratedUserId = getIntent().getStringExtra(EXTRA_RATED_USER_ID);
        ratedUserName = getIntent().getStringExtra(EXTRA_RATED_USER_NAME);
        raterType = getIntent().getStringExtra(EXTRA_RATER_TYPE);

        if (tripId == null || ratedUserId == null) {
            finish();
            return;
        }

        tvTitle = findViewById(R.id.tvRatingTitle);
        tvSubtitle = findViewById(R.id.tvRatingSubtitle);
        ivAvatar = findViewById(R.id.ivRatingAvatar);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        ratingBar = findViewById(R.id.ratingBar);
        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        progressSubmit = findViewById(R.id.progressSubmit);
        btnSubmit = findViewById(R.id.btnSubmitRating);
        btnSkip = findViewById(R.id.btnSkipRating);

        String name = ratedUserName != null ? ratedUserName : "the user";

        if ("passenger".equals(raterType)) {
            tvTitle.setText("How was your driver?");
            tvSubtitle.setText("Rate your experience with " + name);
        } else {
            tvTitle.setText("How was your passenger?");
            tvSubtitle.setText("Rate your experience with " + name);
        }

        if (ratedUserName != null && !ratedUserName.isEmpty()) {
            String initials = getInitials(ratedUserName);
            tvAvatarInitials.setText(initials);
        }

        ratingBar.setStepSize(0.5f);

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (rating < 1) {
                ratingBar.setRating(1);
                return;
            }

            tvRatingLabel.setText(getLabelForStars(rating));
            tvRatingLabel.setVisibility(View.VISIBLE);
        });

        btnSubmit.setOnClickListener(v -> submitRating());

        btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });

        checkAlreadyRated();
    }

    private void checkAlreadyRated() {
        if (mAuth.getCurrentUser() == null) return;

        String raterId = mAuth.getCurrentUser().getUid();

        db.collection("ratings")
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("raterId", raterId)
                .whereEqualTo("ratedUserId", ratedUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        tvSubtitle.setText("You already rated this user.");
                        ratingBar.setIsIndicator(true);

                        float existing = snap.getDocuments()
                                .get(0)
                                .getDouble("stars")
                                .floatValue();

                        ratingBar.setRating(existing);

                        tvRatingLabel.setText(getLabelForStars(existing));
                        tvRatingLabel.setVisibility(View.VISIBLE);

                        btnSubmit.setVisibility(View.GONE);
                    }
                });
    }

    private void submitRating() {
        float stars = ratingBar.getRating();

        if (stars < 1) {
            Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;

        String raterId = mAuth.getCurrentUser().getUid();

        progressSubmit.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        String ratingId = db.collection("ratings").document().getId();

        Rating rating = new Rating(
                ratingId,
                tripId,
                raterId,
                ratedUserId,
                raterType,
                stars
        );

        db.collection("ratings")
                .document(ratingId)
                .set(rating)
                .addOnSuccessListener(unused -> {
                    updateAverageRating(ratedUserId, stars);
                })
                .addOnFailureListener(e -> {
                    progressSubmit.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Could not submit rating.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAverageRating(String userId, float newStars) {

        db.collection("ratings")
                .whereEqualTo("ratedUserId", userId)
                .get()
                .addOnSuccessListener(snap -> {

                    double total = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Double s = doc.getDouble("stars");

                        if (s != null) {
                            total += s;
                        }
                    }

                    double avg = snap.size() > 0
                            ? total / snap.size()
                            : newStars;

                    double rounded = Math.round(avg * 10.0) / 10.0;

                    db.collection("users")
                            .document(userId)
                            .update(
                                    "averageRating", rounded,
                                    "ratingCount", snap.size()
                            )
                            .addOnSuccessListener(v2 -> {
                                progressSubmit.setVisibility(View.GONE);

                                Toast.makeText(
                                        this,
                                        "Rating submitted. Thank you!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressSubmit.setVisibility(View.GONE);

                                Toast.makeText(
                                        this,
                                        "Rating saved.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    progressSubmit.setVisibility(View.GONE);
                    finish();
                });
    }

    private String getInitials(String name) {

        String[] parts = name.trim().split("\\s+");

        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }

        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getLabelForStars(float stars) {

        if (stars >= 0.5f && stars < 1.5f) {
            return "Poor";
        }

        if (stars >= 1.5f && stars < 2.5f) {
            return "Fair";
        }

        if (stars >= 2.5f && stars < 3.5f) {
            return "Good";
        }

        if (stars >= 3.5f && stars < 4.5f) {
            return "Very good";
        }

        return "Excellent";
    }
}