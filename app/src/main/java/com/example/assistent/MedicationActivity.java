package com.example.assistent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MedicationActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_assistant_prefs";
    private static final String KEY_MEDICATION_REMINDERS = "medication_reminders";
    private static final String NOTIFICATION_TITLE = "Medication reminder";

    private EditText etMedicineName;
    private EditText etMedicineTime;
    private ArrayAdapter<ReminderItem> remindersAdapter;
    private final ArrayList<ReminderItem> reminders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication);

        etMedicineName = findViewById(R.id.etMedicineName);
        etMedicineTime = findViewById(R.id.etMedicineTime);
        Button btnAddReminder = findViewById(R.id.btnAddReminder);
        ListView listReminders = findViewById(R.id.listReminders);

        remindersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reminders);
        listReminders.setAdapter(remindersAdapter);
        loadSavedReminders();

        etMedicineTime.setFocusable(false);
        etMedicineTime.setOnClickListener(v -> showTimePicker(etMedicineTime));
        btnAddReminder.setOnClickListener(v -> addReminder());
        listReminders.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteReminder(position);
            return true;
        });
    }

    private void addReminder() {
        String medicineName = etMedicineName.getText().toString().trim();
        String medicineTime = etMedicineTime.getText().toString().trim();

        if (medicineName.isEmpty() || medicineTime.isEmpty()) {
            Toast.makeText(this, "Please enter medicine name and time", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar reminderTime = parseReminderTime(medicineTime);
        if (reminderTime == null) {
            Toast.makeText(this, "Please choose a valid time", Toast.LENGTH_SHORT).show();
            return;
        }

        int requestCode = createRequestCode();
        String message = "Time to take: " + medicineName;
        scheduleReminder(requestCode, medicineName, NOTIFICATION_TITLE, message, reminderTime);

        reminders.add(new ReminderItem(medicineName, medicineTime, requestCode, NOTIFICATION_TITLE, message));
        remindersAdapter.notifyDataSetChanged();
        saveReminders();

        etMedicineName.setText("");
        etMedicineTime.setText("");

        Toast.makeText(this, "Daily reminder scheduled. Long press it to delete.", Toast.LENGTH_SHORT).show();
    }

    private void deleteReminder(int position) {
        ReminderItem reminder = reminders.remove(position);
        if (reminder.requestCode != 0) {
            cancelReminder(reminder);
        }
        remindersAdapter.notifyDataSetChanged();
        saveReminders();
        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
    }

    private void showTimePicker(EditText targetInput) {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> targetInput.setText(formatTime(hourOfDay, minute)),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        dialog.show();
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private Calendar parseReminderTime(String input) {
        String[] parts = input.split(":");
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

    private int createRequestCode() {
        return (int) (System.currentTimeMillis() & 0xfffffff);
    }

    private void scheduleReminder(int requestCode, String reminderName, String title, String message, Calendar reminderTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createReminderPendingIntent(requestCode, reminderName, title, message);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                reminderTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void cancelReminder(ReminderItem reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createReminderPendingIntent(
                reminder.requestCode,
                reminder.name,
                reminder.notificationTitle,
                reminder.notificationMessage
        );
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private PendingIntent createReminderPendingIntent(int requestCode, String reminderName, String title, String message) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, reminderName);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, message);

        return PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void loadSavedReminders() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        reminders.clear();
        try {
            JSONArray savedItems = new JSONArray(preferences.getString(KEY_MEDICATION_REMINDERS, "[]"));
            for (int i = 0; i < savedItems.length(); i++) {
                reminders.add(ReminderItem.fromJson(savedItems, i));
            }
        } catch (JSONException ex) {
            preferences.edit().remove(KEY_MEDICATION_REMINDERS).apply();
        }
        remindersAdapter.notifyDataSetChanged();
    }

    private void saveReminders() {
        JSONArray savedItems = new JSONArray();
        for (ReminderItem reminder : reminders) {
            savedItems.put(reminder.toJson());
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_MEDICATION_REMINDERS, savedItems.toString())
                .apply();
    }

    private static class ReminderItem {
        private final String name;
        private final String time;
        private final int requestCode;
        private final String notificationTitle;
        private final String notificationMessage;

        ReminderItem(String name, String time, int requestCode, String notificationTitle, String notificationMessage) {
            this.name = name;
            this.time = time;
            this.requestCode = requestCode;
            this.notificationTitle = notificationTitle;
            this.notificationMessage = notificationMessage;
        }

        static ReminderItem fromJson(JSONArray savedItems, int index) {
            JSONObject item = savedItems.optJSONObject(index);
            if (item == null) {
                return new ReminderItem(savedItems.optString(index), "", 0, NOTIFICATION_TITLE, "");
            }

            String name = item.optString("name");
            String time = item.optString("time");
            String title = item.optString("notificationTitle", NOTIFICATION_TITLE);
            String message = item.optString("notificationMessage", "Time to take: " + name);
            return new ReminderItem(name, time, item.optInt("requestCode"), title, message);
        }

        JSONObject toJson() {
            JSONObject item = new JSONObject();
            try {
                item.put("name", name);
                item.put("time", time);
                item.put("requestCode", requestCode);
                item.put("notificationTitle", notificationTitle);
                item.put("notificationMessage", notificationMessage);
            } catch (JSONException ignored) {
                // JSONObject with primitive values should not fail here.
            }
            return item;
        }

        @Override
        public String toString() {
            if (time.isEmpty()) {
                return name;
            }
            return String.format(Locale.US, "%s - %s", name, time);
        }
    }
}
