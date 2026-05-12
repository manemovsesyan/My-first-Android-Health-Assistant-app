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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SugarActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_assistant_prefs";
    private static final String KEY_HEALTH_REMINDERS = "health_reminders";
    private static final String KEY_HEALTH_MEASUREMENTS = "health_measurements";

    private EditText etSugarTime;
    private EditText etPressureTime;
    private EditText etSugarValue;
    private EditText etSystolicPressure;
    private EditText etDiastolicPressure;
    private EditText etMeasurementNote;
    private ArrayAdapter<HealthReminderItem> remindersAdapter;
    private ArrayAdapter<HealthMeasurement> measurementsAdapter;
    private final ArrayList<HealthReminderItem> reminders = new ArrayList<>();
    private final ArrayList<HealthMeasurement> measurements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugar);

        etSugarTime = findViewById(R.id.etSugarTime);
        etPressureTime = findViewById(R.id.etPressureTime);
        etSugarValue = findViewById(R.id.etSugarValue);
        etSystolicPressure = findViewById(R.id.etSystolicPressure);
        etDiastolicPressure = findViewById(R.id.etDiastolicPressure);
        etMeasurementNote = findViewById(R.id.etMeasurementNote);
        Button btnSugarReminder = findViewById(R.id.btnSugarReminder);
        Button btnPressureReminder = findViewById(R.id.btnPressureReminder);
        Button btnSaveMeasurement = findViewById(R.id.btnSaveMeasurement);
        ListView listHealthReminders = findViewById(R.id.listHealthReminders);
        ListView listHealthMeasurements = findViewById(R.id.listHealthMeasurements);

        remindersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reminders);
        listHealthReminders.setAdapter(remindersAdapter);
        loadSavedReminders();

        measurementsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, measurements);
        listHealthMeasurements.setAdapter(measurementsAdapter);
        loadSavedMeasurements();

        etSugarTime.setFocusable(false);
        etPressureTime.setFocusable(false);
        etSugarTime.setOnClickListener(v -> showTimePicker(etSugarTime));
        etPressureTime.setOnClickListener(v -> showTimePicker(etPressureTime));

        btnSugarReminder.setOnClickListener(v -> addHealthReminder(
                "Blood sugar check",
                "Time to check your blood sugar",
                etSugarTime.getText().toString().trim(),
                etSugarTime
        ));

        btnPressureReminder.setOnClickListener(v -> addHealthReminder(
                "Blood pressure check",
                "Time to check your blood pressure",
                etPressureTime.getText().toString().trim(),
                etPressureTime
        ));

        btnSaveMeasurement.setOnClickListener(v -> saveMeasurement());

        listHealthReminders.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteReminder(position);
            return true;
        });
        listHealthMeasurements.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteMeasurement(position);
            return true;
        });
    }

    private void addHealthReminder(String reminderTitle, String reminderMessage, String timeInput, EditText sourceInput) {
        if (timeInput.isEmpty()) {
            Toast.makeText(this, "Please choose time", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar reminderTime = parseReminderTime(timeInput);
        if (reminderTime == null) {
            Toast.makeText(this, "Please choose a valid time", Toast.LENGTH_SHORT).show();
            return;
        }

        int requestCode = createRequestCode();
        scheduleReminder(requestCode, reminderTitle, reminderMessage, reminderTime);
        reminders.add(new HealthReminderItem(reminderTitle, timeInput, requestCode, reminderMessage));
        remindersAdapter.notifyDataSetChanged();
        saveReminders();
        sourceInput.setText("");

        Toast.makeText(this, reminderTitle + " daily reminder scheduled. Long press it to delete.", Toast.LENGTH_SHORT).show();
    }

    private void deleteReminder(int position) {
        HealthReminderItem reminder = reminders.remove(position);
        if (reminder.requestCode != 0) {
            cancelReminder(reminder);
        }
        remindersAdapter.notifyDataSetChanged();
        saveReminders();
        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
    }

    private void saveMeasurement() {
        String sugarValue = etSugarValue.getText().toString().trim();
        String systolic = etSystolicPressure.getText().toString().trim();
        String diastolic = etDiastolicPressure.getText().toString().trim();
        String note = etMeasurementNote.getText().toString().trim();

        if (sugarValue.isEmpty() && (systolic.isEmpty() || diastolic.isEmpty())) {
            Toast.makeText(this, "Enter sugar value or both pressure values", Toast.LENGTH_SHORT).show();
            return;
        }
        if ((!systolic.isEmpty() && diastolic.isEmpty()) || (systolic.isEmpty() && !diastolic.isEmpty())) {
            Toast.makeText(this, "Enter both systolic and diastolic pressure", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isPositiveNumber(sugarValue) || !isPositiveNumber(systolic) || !isPositiveNumber(diastolic)) {
            Toast.makeText(this, "Values must be positive numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        String measuredAt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(new Date());
        measurements.add(0, new HealthMeasurement(sugarValue, systolic, diastolic, note, measuredAt));
        measurementsAdapter.notifyDataSetChanged();
        saveMeasurements();

        etSugarValue.setText("");
        etSystolicPressure.setText("");
        etDiastolicPressure.setText("");
        etMeasurementNote.setText("");

        Toast.makeText(this, "Measurement saved for doctor review", Toast.LENGTH_SHORT).show();
    }

    private boolean isPositiveNumber(String value) {
        if (value.isEmpty()) {
            return true;
        }

        try {
            return Double.parseDouble(value) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void deleteMeasurement(int position) {
        measurements.remove(position);
        measurementsAdapter.notifyDataSetChanged();
        saveMeasurements();
        Toast.makeText(this, "Measurement deleted", Toast.LENGTH_SHORT).show();
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

    private void scheduleReminder(int requestCode, String reminderTitle, String reminderMessage, Calendar reminderTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createReminderPendingIntent(requestCode, reminderTitle, reminderMessage);
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                reminderTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void cancelReminder(HealthReminderItem reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createReminderPendingIntent(
                reminder.requestCode,
                reminder.title,
                reminder.notificationMessage
        );
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private PendingIntent createReminderPendingIntent(int requestCode, String reminderTitle, String reminderMessage) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminderTitle);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, reminderMessage);
        intent.putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, reminderTitle);

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
            JSONArray savedItems = new JSONArray(preferences.getString(KEY_HEALTH_REMINDERS, "[]"));
            for (int i = 0; i < savedItems.length(); i++) {
                reminders.add(HealthReminderItem.fromJson(savedItems, i));
            }
        } catch (JSONException ex) {
            preferences.edit().remove(KEY_HEALTH_REMINDERS).apply();
        }
        remindersAdapter.notifyDataSetChanged();
    }

    private void saveReminders() {
        JSONArray savedItems = new JSONArray();
        for (HealthReminderItem reminder : reminders) {
            savedItems.put(reminder.toJson());
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_HEALTH_REMINDERS, savedItems.toString())
                .apply();
    }

    private void loadSavedMeasurements() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        measurements.clear();
        try {
            JSONArray savedItems = new JSONArray(preferences.getString(KEY_HEALTH_MEASUREMENTS, "[]"));
            for (int i = 0; i < savedItems.length(); i++) {
                JSONObject item = savedItems.optJSONObject(i);
                if (item != null) {
                    measurements.add(HealthMeasurement.fromJson(item));
                }
            }
        } catch (JSONException ex) {
            preferences.edit().remove(KEY_HEALTH_MEASUREMENTS).apply();
        }
        measurementsAdapter.notifyDataSetChanged();
    }

    private void saveMeasurements() {
        JSONArray savedItems = new JSONArray();
        for (HealthMeasurement measurement : measurements) {
            savedItems.put(measurement.toJson());
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_HEALTH_MEASUREMENTS, savedItems.toString())
                .apply();
    }

    private static class HealthReminderItem {
        private final String title;
        private final String time;
        private final int requestCode;
        private final String notificationMessage;

        HealthReminderItem(String title, String time, int requestCode, String notificationMessage) {
            this.title = title;
            this.time = time;
            this.requestCode = requestCode;
            this.notificationMessage = notificationMessage;
        }

        static HealthReminderItem fromJson(JSONArray savedItems, int index) {
            JSONObject item = savedItems.optJSONObject(index);
            if (item == null) {
                return new HealthReminderItem(savedItems.optString(index), "", 0, "");
            }

            String title = item.optString("title");
            String message = item.optString("notificationMessage", "Time for " + title);
            return new HealthReminderItem(
                    title,
                    item.optString("time"),
                    item.optInt("requestCode"),
                    message
            );
        }

        JSONObject toJson() {
            JSONObject item = new JSONObject();
            try {
                item.put("title", title);
                item.put("time", time);
                item.put("requestCode", requestCode);
                item.put("notificationMessage", notificationMessage);
            } catch (JSONException ignored) {
                // JSONObject with primitive values should not fail here.
            }
            return item;
        }

        @Override
        public String toString() {
            if (time.isEmpty()) {
                return title;
            }
            return String.format(Locale.US, "%s - %s", title, time);
        }
    }

    private static class HealthMeasurement {
        private final String sugarValue;
        private final String systolicPressure;
        private final String diastolicPressure;
        private final String note;
        private final String measuredAt;

        HealthMeasurement(String sugarValue, String systolicPressure, String diastolicPressure, String note, String measuredAt) {
            this.sugarValue = sugarValue;
            this.systolicPressure = systolicPressure;
            this.diastolicPressure = diastolicPressure;
            this.note = note;
            this.measuredAt = measuredAt;
        }

        static HealthMeasurement fromJson(JSONObject item) {
            return new HealthMeasurement(
                    item.optString("sugarValue"),
                    item.optString("systolicPressure"),
                    item.optString("diastolicPressure"),
                    item.optString("note"),
                    item.optString("measuredAt")
            );
        }

        JSONObject toJson() {
            JSONObject item = new JSONObject();
            try {
                item.put("sugarValue", sugarValue);
                item.put("systolicPressure", systolicPressure);
                item.put("diastolicPressure", diastolicPressure);
                item.put("note", note);
                item.put("measuredAt", measuredAt);
            } catch (JSONException ignored) {
                // JSONObject with primitive values should not fail here.
            }
            return item;
        }

        @Override
        public String toString() {
            ArrayList<String> parts = new ArrayList<>();
            if (!sugarValue.isEmpty()) {
                parts.add("Sugar: " + sugarValue);
            }
            if (!systolicPressure.isEmpty() && !diastolicPressure.isEmpty()) {
                parts.add("Pressure: " + systolicPressure + "/" + diastolicPressure);
            }
            if (!note.isEmpty()) {
                parts.add("Note: " + note);
            }
            StringBuilder details = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    details.append(" | ");
                }
                details.append(parts.get(i));
            }
            return measuredAt + "\n" + details;
        }
    }
}
