package com.example.myway;

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

import com.example.myway.adapters.RequestAdapter;
import com.example.myway.models.PassengerRequest;
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
    private List<PassengerRequest> requestList;
    private ProgressBar progressRequests;
    private TextView tvEmpty;
    private ImageButton btnMore;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerViewRequests);
        progressRequests = findViewById(R.id.progressRequests);
        tvEmpty = findViewById(R.id.tvEmptyRequests);
        btnMore = findViewById(R.id.btnMore);

        requestList = new ArrayList<>();
        adapter = new RequestAdapter(requestList, this::confirmDelete);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupMoreButton(btnMore);
        loadMyRequests();
    }

    private void loadMyRequests() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        progressRequests.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("requests")
                .whereEqualTo("passengerId", uid)
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressRequests.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            requestList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                PassengerRequest req = doc.toObject(PassengerRequest.class);
                                if (req != null) requestList.add(req);
                            }
                            adapter.notifyDataSetChanged();

                            if (requestList.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                            }
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
                            loadMyRequests();
                        } else {
                            Toast.makeText(MyRequestsActivity.this,
                                    "Failed to delete: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}