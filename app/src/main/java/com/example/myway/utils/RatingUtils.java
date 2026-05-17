package com.example.myway.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.myway.RatingNotificationReceiver;

public class RatingUtils {

    public static void scheduleRatingNotification(Context context,
                                                  String tripId,
                                                  String ratedUserId,
                                                  String ratedUserName,
                                                  String raterType,
                                                  long tripTimeMillis) {
        long fireAt = tripTimeMillis;
        if (fireAt < System.currentTimeMillis()) {
            fireAt = System.currentTimeMillis() + 2000;
        }

        Intent intent = new Intent(context, RatingNotificationReceiver.class);
        intent.putExtra(RatingNotificationReceiver.EXTRA_TRIP_ID,    tripId);
        intent.putExtra(RatingNotificationReceiver.EXTRA_USER_ID,    ratedUserId);
        intent.putExtra(RatingNotificationReceiver.EXTRA_USER_NAME,  ratedUserName);
        intent.putExtra(RatingNotificationReceiver.EXTRA_RATER_TYPE, raterType);

        int requestCode = (tripId + ratedUserId).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
            }
        }
    }

    public static String buildStarsText(double averageRating, int ratingCount) {
        if (ratingCount == 0) return "";
        StringBuilder sb = new StringBuilder();
        int full  = (int) averageRating;
        boolean half = (averageRating - full) >= 0.25 && (averageRating - full) < 0.75;
        boolean roundUp = (averageRating - full) >= 0.75;
        if (roundUp) full++;

        for (int i = 0; i < 5; i++) {
            if (i < full)                   sb.append("★");
            else if (i == full && half)     sb.append("⯨");
            else                            sb.append("☆");
        }
        sb.append("  ").append(String.format("%.1f", averageRating));
        sb.append("  (").append(ratingCount).append(")");
        return sb.toString();
    }
}