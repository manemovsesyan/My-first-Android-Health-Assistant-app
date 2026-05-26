package com.example.ReMedi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
    private static final String ACTION_TAKEN = "com.example.ReMedi.ACTION_TAKEN";
    private static final String ACTION_SNOOZE = "com.example.ReMedi.ACTION_SNOOZE";
    private static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    private static final int SNOOZE_MINUTES = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_TAKEN.equals(action)) {
            int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
            NotificationManagerCompat.from(context).cancel(notificationId);
            return;
        }

        if (ACTION_SNOOZE.equals(action)) {
            int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
            NotificationManagerCompat.from(context).cancel(notificationId);
            scheduleSnooze(context, intent);
            return;
        }

        String fallbackName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            fallbackName = context.getString(R.string.reminder_fallback_name);
        }

        String title = intent.getStringExtra(EXTRA_REMINDER_TITLE);
        if (title == null || title.trim().isEmpty()) {
            title = context.getString(R.string.reminder_title_generic);
        }

        String message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE);
        if (message == null || message.trim().isEmpty()) {
            message = context.getString(R.string.reminder_time_for, fallbackName);
        }

        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int notificationId = (int) System.currentTimeMillis();
        PendingIntent takenPendingIntent = createActionPendingIntent(context, intent, ACTION_TAKEN, notificationId);
        PendingIntent snoozePendingIntent = createActionPendingIntent(context, intent, ACTION_SNOOZE, notificationId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.health_icon_purple)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(true)
                .setTimeoutAfter(2 * 60 * 1000L)
                .addAction(0, context.getString(R.string.action_done), takenPendingIntent)
                .addAction(0, context.getString(R.string.action_remind_10), snoozePendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private PendingIntent createActionPendingIntent(Context context, Intent sourceIntent, String action, int notificationId) {
        Intent actionIntent = new Intent(context, ReminderReceiver.class);
        actionIntent.setAction(action);
        actionIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        actionIntent.putExtra(EXTRA_MEDICINE_NAME, sourceIntent.getStringExtra(EXTRA_MEDICINE_NAME));
        actionIntent.putExtra(EXTRA_REMINDER_TITLE, sourceIntent.getStringExtra(EXTRA_REMINDER_TITLE));
        actionIntent.putExtra(EXTRA_REMINDER_MESSAGE, sourceIntent.getStringExtra(EXTRA_REMINDER_MESSAGE));

        int requestCode = notificationId + (ACTION_SNOOZE.equals(action) ? 1 : 0);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void scheduleSnooze(Context context, Intent sourceIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.putExtra(EXTRA_MEDICINE_NAME, sourceIntent.getStringExtra(EXTRA_MEDICINE_NAME));
        snoozeIntent.putExtra(EXTRA_REMINDER_TITLE, sourceIntent.getStringExtra(EXTRA_REMINDER_TITLE));
        snoozeIntent.putExtra(EXTRA_REMINDER_MESSAGE, sourceIntent.getStringExtra(EXTRA_REMINDER_MESSAGE));

        int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAt = System.currentTimeMillis() + (SNOOZE_MINUTES * 60 * 1000L);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.reminder_channel_desc));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
