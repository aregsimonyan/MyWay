package com.example.myway;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.myway.utils.NotificationHelperUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingListenerService extends Service {

    private static final String TAG = "BookingListenerService";

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    private final List<ListenerRegistration> listeners       = new ArrayList<>();
    private final Map<String, List<String>>  knownPassengers = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        NotificationHelperUtils.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        attachListeners(user.getUid());
        return START_NOT_STICKY;
    }

    private void attachListeners(String driverId) {
        removeAllListeners();

        db.collection("trips")
                .whereEqualTo("driverId", driverId)
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(tripSnap -> {
                    for (QueryDocumentSnapshot tripDoc : tripSnap) {
                        attachSingleTripListener(tripDoc.getId());
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Could not fetch trips: " + e.getMessage()));
    }

    private void attachSingleTripListener(String tripId) {
        ListenerRegistration reg = db.collection("trips")
                .document(tripId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    List<String> currentIds = castToStringList(snapshot.get("passengerIds"));
                    List<String> previously = knownPassengers.get(tripId);

                    if (previously == null) {
                        knownPassengers.put(tripId, new ArrayList<>(currentIds));
                        return;
                    }

                    List<String> newIds     = new ArrayList<>();
                    List<String> removedIds = new ArrayList<>();

                    for (String uid : currentIds) {
                        if (!previously.contains(uid)) newIds.add(uid);
                    }
                    for (String uid : previously) {
                        if (!currentIds.contains(uid)) removedIds.add(uid);
                    }

                    for (String uid : newIds) {
                        handleNewBooking(tripId, uid, snapshot);
                    }
                    for (String uid : removedIds) {
                        handleCancellation(tripId, uid, snapshot);
                    }

                    knownPassengers.put(tripId, new ArrayList<>(currentIds));
                });

        listeners.add(reg);
    }

    private void handleNewBooking(String tripId,
                                  String passengerUid,
                                  com.google.firebase.firestore.DocumentSnapshot tripSnapshot) {
        String route = buildRoute(tripSnapshot);

        db.collection("users").document(passengerUid).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.getString("name");
                    if (name == null || name.isEmpty()) name = "A passenger";
                    NotificationHelperUtils.notifyNewBooking(this, tripId, name, route);
                })
                .addOnFailureListener(e ->
                        NotificationHelperUtils.notifyNewBooking(this, tripId, "A new passenger", route));
    }

    private void handleCancellation(String tripId,
                                    String passengerUid,
                                    com.google.firebase.firestore.DocumentSnapshot tripSnapshot) {
        String route = buildRoute(tripSnapshot);

        db.collection("users").document(passengerUid).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.getString("name");
                    if (name == null || name.isEmpty()) name = "A passenger";
                    NotificationHelperUtils.notifyCancellation(this, tripId, name, route);
                })
                .addOnFailureListener(e ->
                        NotificationHelperUtils.notifyCancellation(this, tripId, "A passenger", route));
    }

    private String buildRoute(com.google.firebase.firestore.DocumentSnapshot tripSnapshot) {
        String from = tripSnapshot.getString("fromLocation");
        String to   = tripSnapshot.getString("toLocation");
        return (from != null ? from : "?") + " → " + (to != null ? to : "?");
    }

    @SuppressWarnings("unchecked")
    private List<String> castToStringList(Object raw) {
        if (raw instanceof List) {
            try { return (List<String>) raw; } catch (ClassCastException ignored) {}
        }
        return new ArrayList<>();
    }

    private void removeAllListeners() {
        for (ListenerRegistration reg : listeners) reg.remove();
        listeners.clear();
        knownPassengers.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeAllListeners();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}