package com.example.myway;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class RatingNotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID      = "myway_ratings";
    public static final String EXTRA_TRIP_ID   = "tripId";
    public static final String EXTRA_USER_ID   = "ratedUserId";
    public static final String EXTRA_USER_NAME = "ratedUserName";
    public static final String EXTRA_RATER_TYPE = "raterType";

    @Override
    public void onReceive(Context context, Intent intent) {
        String tripId        = intent.getStringExtra(EXTRA_TRIP_ID);
        String ratedUserId   = intent.getStringExtra(EXTRA_USER_ID);
        String ratedUserName = intent.getStringExtra(EXTRA_USER_NAME);
        String raterType     = intent.getStringExtra(EXTRA_RATER_TYPE);

        createNotificationChannel(context);

        Intent ratingIntent = new Intent(context, RatingActivity.class);
        ratingIntent.putExtra(RatingActivity.EXTRA_TRIP_ID,         tripId);
        ratingIntent.putExtra(RatingActivity.EXTRA_RATED_USER_ID,   ratedUserId);
        ratingIntent.putExtra(RatingActivity.EXTRA_RATED_USER_NAME, ratedUserName);
        ratingIntent.putExtra(RatingActivity.EXTRA_RATER_TYPE,      raterType);
        ratingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = (tripId + ratedUserId).hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                ratingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "passenger".equals(raterType)
                ? "How was your trip with " + ratedUserName + "?"
                : "Rate your passenger " + ratedUserName;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_more_vert)
                .setContentTitle("Rate your experience")
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        title + "\n\nTap to leave a quick rating."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(8000 + Math.abs(requestCode % 1000), builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Trip Ratings",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminds you to rate your driver or passenger after a trip");
            channel.enableVibration(true);
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}