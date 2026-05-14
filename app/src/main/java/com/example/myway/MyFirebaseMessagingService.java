package com.example.myway;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myway.utils.NotificationHelperUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().isEmpty()) return;

        String tripId        = remoteMessage.getData().get("tripId");
        String passengerName = remoteMessage.getData().get("passengerName");
        String route         = remoteMessage.getData().get("route");
        String type          = remoteMessage.getData().get("type");

        if (tripId == null || passengerName == null || route == null) return;

        NotificationHelperUtils.init(this);

        if ("cancellation".equals(type)) {
            NotificationHelperUtils.notifyCancellation(this, tripId, passengerName, route);
        } else {
            NotificationHelperUtils.notifyNewBooking(this, tripId, passengerName, route);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("fcmToken", token)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save FCM token: " + e.getMessage()));
    }
}