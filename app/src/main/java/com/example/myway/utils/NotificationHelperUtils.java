package com.example.myway.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.myway.R;
import com.example.myway.TripPassengersActivity;

public class NotificationHelperUtils {

    private static final String CHANNEL_ID   = "myway_bookings";
    private static final String CHANNEL_NAME = "New Bookings";

    public static void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts when a passenger books or cancels your ride");
            channel.enableVibration(true);
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void notifyNewBooking(Context context,
                                        String tripId,
                                        String passengerName,
                                        String route) {
        postNotification(
                context,
                tripId,
                "New Booking!",
                passengerName + " booked your trip: " + route,
                passengerName + " just booked a seat on your trip:\n" + route
                        + "\n\nTap to see passenger details."
        );
    }

    public static void notifyCancellation(Context context,
                                          String tripId,
                                          String passengerName,
                                          String route) {
        postNotification(
                context,
                tripId,
                "Booking Cancelled",
                passengerName + " cancelled their booking: " + route,
                passengerName + " cancelled their booking on your trip:\n" + route
                        + "\n\nTap to see passenger details."
        );
    }

    private static void postNotification(Context context,
                                         String tripId,
                                         String title,
                                         String shortText,
                                         String longText) {
        Intent intent = new Intent(context, TripPassengersActivity.class);
        intent.putExtra(TripPassengersActivity.EXTRA_TRIP_ID, tripId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                tripId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_more_vert)
                        .setContentTitle(title)
                        .setContentText(shortText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(longText))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(7000 + Math.abs(tripId.hashCode() % 1000), builder.build());
        }
    }
}