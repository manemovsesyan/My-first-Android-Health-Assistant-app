package com.example.assistent;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_REMINDER_TITLE = "extra_reminder_title";
    public static final String EXTRA_REMINDER_MESSAGE = "extra_reminder_message";
    public static final String EXTRA_MEDICINE_NAME = "extra_medicine_name";
    private static final String CHANNEL_ID = "health_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String fallbackName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            fallbackName = "your health reminder";
        }

        String title = intent.getStringExtra(EXTRA_REMINDER_TITLE);
        if (title == null || title.trim().isEmpty()) {
            title = "Health reminder";
        }

        String message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE);
        if (message == null || message.trim().isEmpty()) {
            message = "Time for " + fallbackName;
        }

        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.health_icon_purple)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Health reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for medication, sugar, and pressure reminders");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
