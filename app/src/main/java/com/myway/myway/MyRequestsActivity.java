package com.myway.myway;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.myway.myway.adapters.RequestAdapter;
import com.myway.myway.models.PassengerRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyRequestsActivity extends MenuActivity {

    private RecyclerView recyclerView;
    private RequestAdapter adapter;

    private List<PassengerRequest> displayList;
    private List<PassengerRequest> allActive;
    private List<PassengerRequest> allPast;

    private ProgressBar progressRequests;
    private TextView tvEmpty;
    private TextView tabActive;
    private TextView tabPast;
    private View tabIndicator;
    private ImageButton btnMore;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private boolean showingActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView      = findViewById(R.id.recyclerViewRequests);
        progressRequests  = findViewById(R.id.progressRequests);
        tvEmpty           = findViewById(R.id.tvEmptyRequests);
        tabActive         = findViewById(R.id.tabActive);
        tabPast           = findViewById(R.id.tabPast);
        tabIndicator      = findViewById(R.id.tabIndicator);
        btnMore           = findViewById(R.id.btnMore);

        displayList = new ArrayList<>();
        allActive   = new ArrayList<>();
        allPast     = new ArrayList<>();

        adapter = new RequestAdapter(displayList, this::confirmDelete);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupMoreButton(btnMore);

        tabActive.setOnClickListener(v -> switchTab(true));
        tabPast.setOnClickListener(v -> switchTab(false));

        loadAllMyRequests();
    }

    private void switchTab(boolean active) {
        showingActive = active;

        if (active) {
            tabActive.setTextColor(getResources().getColor(R.color.colorPrimaryText));
            tabActive.setTypeface(null, Typeface.BOLD);
            tabPast.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            tabPast.setTypeface(null, Typeface.NORMAL);
            tabIndicator.setTranslationX(0);
            displayList.clear();
            displayList.addAll(allActive);
        } else {
            tabPast.setTextColor(getResources().getColor(R.color.colorPrimaryText));
            tabPast.setTypeface(null, Typeface.BOLD);
            tabActive.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            tabActive.setTypeface(null, Typeface.NORMAL);
            tabIndicator.setTranslationX(tabActive.getWidth());
            displayList.clear();
            displayList.addAll(allPast);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (displayList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(showingActive
                    ? "You have no active ride requests."
                    : "You have no past ride requests.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void loadAllMyRequests() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        long now = System.currentTimeMillis();

        progressRequests.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("requests")
                .whereEqualTo("passengerId", uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressRequests.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            allActive.clear();
                            allPast.clear();

                            for (DocumentSnapshot doc : task.getResult()) {
                                PassengerRequest req = doc.toObject(PassengerRequest.class);
                                if (req == null) continue;

                                if (req.getDateTime() > now) {
                                    allActive.add(req);
                                } else {
                                    allPast.add(req);
                                }
                            }

                            allActive.sort((a, b) -> Long.compare(a.getDateTime(), b.getDateTime()));
                            allPast.sort((a, b) -> Long.compare(b.getDateTime(), a.getDateTime()));

                            switchTab(true);
                        } else {
                            Toast.makeText(MyRequestsActivity.this,
                                    "Failed to load requests", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void confirmDelete(PassengerRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Request")
                .setMessage("Delete your ride request from "
                        + request.getFromLocation() + " to " + request.getToLocation() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRequest(request))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRequest(PassengerRequest request) {
        db.collection("requests").document(request.getRequestId()).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MyRequestsActivity.this,
                                    "Request deleted", Toast.LENGTH_SHORT).show();
                            loadAllMyRequests();
                        } else {
                            Toast.makeText(MyRequestsActivity.this,
                                    "Failed to delete: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}