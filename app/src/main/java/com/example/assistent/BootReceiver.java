package com.example.assistent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BootReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "health_assistant_prefs";
    private static final String KEY_MEDICATION_REMINDERS = "medication_reminders";
    private static final String KEY_HEALTH_REMINDERS = "health_reminders";
    private static final String KEY_CALENDAR_TASKS = "calendar_tasks";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        restoreDailyMedicationReminders(context);
        restoreDailyHealthReminders(context);
        restoreFutureCalendarTasks(context);
    }

    private void restoreDailyMedicationReminders(Context context) {
        JSONArray reminders = getSavedArray(context, KEY_MEDICATION_REMINDERS);
        for (int i = 0; i < reminders.length(); i++) {
            JSONObject item = reminders.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String name = item.optString("name");
            String time = item.optString("time");
            int requestCode = item.optInt("requestCode");
            String title = item.optString("notificationTitle", "Medication reminder");
            String message = item.optString("notificationMessage", "Time to take: " + name);
            Calendar reminderTime = nextDailyTime(time);
            if (requestCode != 0 && reminderTime != null) {
                scheduleDaily(context, requestCode, name, title, message, reminderTime);
            }
        }
    }

    private void restoreDailyHealthReminders(Context context) {
        JSONArray reminders = getSavedArray(context, KEY_HEALTH_REMINDERS);
        for (int i = 0; i < reminders.length(); i++) {
            JSONObject item = reminders.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String title = item.optString("title");
            String time = item.optString("time");
            int requestCode = item.optInt("requestCode");
            String message = item.optString("notificationMessage", "Time for " + title);
            Calendar reminderTime = nextDailyTime(time);
            if (requestCode != 0 && reminderTime != null) {
                scheduleDaily(context, requestCode, title, title, message, reminderTime);
            }
        }
    }

    private void restoreFutureCalendarTasks(Context context) {
        JSONArray tasks = getSavedArray(context, KEY_CALENDAR_TASKS);
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject item = tasks.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String text = item.optString("text");
            String dateKey = item.optString("dateKey");
            String time = item.optString("time");
            int requestCode = item.optInt("requestCode");
            String message = item.optString("notificationMessage", dateKey + " at " + time + ": " + text);
            Calendar reminderTime = dateTime(dateKey, time);
            if (requestCode != 0 && reminderTime != null && reminderTime.after(Calendar.getInstance())) {
                scheduleOnce(context, requestCode, text, "Calendar reminder", message, reminderTime);
            }
        }
    }

    private JSONArray getSavedArray(Context context, String key) {
        try {
            String value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, "[]");
            return new JSONArray(value);
        } catch (JSONException ex) {
            return new JSONArray();
        }
    }

    private Calendar nextDailyTime(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            return calendar;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Calendar dateTime(String dateKey, String time) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(dateKey + " " + time));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        } catch (ParseException ex) {
            return null;
        }
    }

    private void scheduleDaily(Context context, int requestCode, String fallbackName, String title, String message, Calendar reminderTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                reminderTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                createPendingIntent(context, requestCode, fallbackName, title, message)
        );
    }

    private void scheduleOnce(Context context, int requestCode, String fallbackName, String title, String message, Calendar reminderTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.getTimeInMillis(),
                createPendingIntent(context, requestCode, fallbackName, title, message)
        );
    }

    private PendingIntent createPendingIntent(Context context, int requestCode, String fallbackName, String title, String message) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, fallbackName);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, message);

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
