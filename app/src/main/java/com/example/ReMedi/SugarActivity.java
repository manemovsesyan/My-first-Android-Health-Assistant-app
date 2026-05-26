package com.example.ReMedi;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private static final String PREFS_NAME = "health_remedi_prefs";
    private static final String KEY_HEALTH_REMINDERS = "health_reminders";
    private static final String KEY_HEALTH_MEASUREMENTS = "health_measurements";

    private EditText etSugarTime;
    private EditText etPressureTime;
    private EditText etSugarValue;
    private EditText etSystolicPressure;
    private EditText etDiastolicPressure;
    private EditText etWeightValue;
    private EditText etWaistValue;
    private EditText etMeasurementNote;
    private EditText etAgeValue;
    private AutoCompleteTextView atvMetricChoose;
    private LinearLayout layoutPressureInputs;
    private LinearLayout layoutWeightInputs;
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
        etWeightValue = findViewById(R.id.etWeightValue);
        etWaistValue = findViewById(R.id.etWaistValue);
        etMeasurementNote = findViewById(R.id.etMeasurementNote);
        etAgeValue = findViewById(R.id.etAgeValue);
        atvMetricChoose = findViewById(R.id.atvMetricChoose);
        layoutPressureInputs = findViewById(R.id.layoutPressureInputs);
        layoutWeightInputs = findViewById(R.id.layoutWeightInputs);

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

        setupMetricDropdown();

        btnSugarReminder.setOnClickListener(v -> addHealthReminder(
                getString(R.string.reminder_title_sugar_check),
                getString(R.string.reminder_message_sugar_check),
                etSugarTime.getText().toString().trim(),
                etSugarTime
        ));

        btnPressureReminder.setOnClickListener(v -> addHealthReminder(
                getString(R.string.reminder_title_pressure_check),
                getString(R.string.reminder_message_pressure_check),
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

    private void setupMetricDropdown() {
        ArrayAdapter<String> metricAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{getString(R.string.metric_sugar), getString(R.string.metric_pressure), getString(R.string.metric_weight)}
        );
        atvMetricChoose.setAdapter(metricAdapter);
        atvMetricChoose.setText(getString(R.string.metric_sugar), false);
        updateMetricInputVisibility(getString(R.string.metric_sugar));
        atvMetricChoose.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            updateMetricInputVisibility(selected);
        });
    }

    private void updateMetricInputVisibility(String selectedMetric) {
        etSugarValue.setVisibility(getString(R.string.metric_sugar).equals(selectedMetric) ? View.VISIBLE : View.GONE);
        layoutPressureInputs.setVisibility(getString(R.string.metric_pressure).equals(selectedMetric) ? View.VISIBLE : View.GONE);
        boolean isWeight = getString(R.string.metric_weight).equals(selectedMetric);
        layoutWeightInputs.setVisibility(isWeight ? View.VISIBLE : View.GONE);
        etAgeValue.setVisibility(isWeight ? View.VISIBLE : View.GONE);
    }

    private void saveMeasurement() {
        String selectedMetric = atvMetricChoose.getText().toString().trim();
        String sugarValue = etSugarValue.getText().toString().trim();
        String systolic = etSystolicPressure.getText().toString().trim();
        String diastolic = etDiastolicPressure.getText().toString().trim();
        String weight = etWeightValue.getText().toString().trim();
        String waist = etWaistValue.getText().toString().trim();
        String note = etMeasurementNote.getText().toString().trim();
        String age = etAgeValue.getText().toString().trim();

        if (selectedMetric.isEmpty()) {
            showHealthDialog(getString(R.string.hs_select_metric));
            return;
        }

        StringBuilder warningMessage = new StringBuilder();
        boolean hasValidationError = false;

        if (getString(R.string.metric_sugar).equals(selectedMetric)) {
            if (sugarValue.isEmpty()) {
                hasValidationError = true;
                warningMessage.append(getString(R.string.hs_enter_sugar));
            } else if (!isPositiveNumber(sugarValue)) {
                hasValidationError = true;
                warningMessage.append(getString(R.string.hs_positive_sugar));
            } else {
                double sugar = Double.parseDouble(sugarValue);
                if (sugar >= 4.0 && sugar <= 8.0) {
                    warningMessage.append(getString(R.string.hs_sugar_normal));
                } else {
                    warningMessage.append(getString(R.string.hs_sugar_abnormal));
                }
            }
        }

        if (getString(R.string.metric_pressure).equals(selectedMetric)) {
            if (systolic.isEmpty() || diastolic.isEmpty()) {
                hasValidationError = true;
                warningMessage.append(getString(R.string.hs_enter_pressure));
            } else if (!isPositiveNumber(systolic) || !isPositiveNumber(diastolic)) {
                hasValidationError = true;
                warningMessage.append(getString(R.string.hs_positive_pressure));
            } else {
                double sys = Double.parseDouble(systolic);
                double dia = Double.parseDouble(diastolic);
                if (sys < 120 && dia < 80 && sys >= 90 && dia >= 60) {
                    warningMessage.append(getString(R.string.hs_pressure_normal));
                } else {
                    warningMessage.append(getString(R.string.hs_pressure_abnormal));
                }
            }
        }

        if (getString(R.string.metric_weight).equals(selectedMetric)) {
            if (weight.isEmpty() || waist.isEmpty() || age.isEmpty()) {
                hasValidationError = true;
                warningMessage.append(getString(R.string.hs_enter_weight_waist_age));
            } else if (!isPositiveNumber(weight) || !isPositiveNumber(waist) || !isPositiveInteger(age)) {
                hasValidationError = true;
                warningMessage.append(getString(R.string.hs_positive_weight_waist_age));
            } else {
                double weightKg = Double.parseDouble(weight);
                double waistCm = Double.parseDouble(waist);
                int ageYears = Integer.parseInt(age);
                double bmi = weightKg / (1.75 * 1.75);

                boolean bmiNormal = bmi >= 18.5 && bmi <= 24.9;
                boolean waistNormal = isWaistNormal(waistCm);

                if (bmi < 18.5) {
                    warningMessage.append(getString(R.string.hs_bmi_underweight));
                } else if (bmi <= 24.9) {
                    if (waistNormal) {
                        warningMessage.append(getString(R.string.hs_bmi_normal));
                    }
                } else if (bmi <= 29.9) {
                    warningMessage.append(getString(R.string.hs_bmi_overweight));
                } else {
                    warningMessage.append(getString(R.string.hs_bmi_obesity));
                }

                if (ageYears >= 40 && (bmi >= 25 || !waistNormal)) {
                    warningMessage.append("\n\n").append(getString(R.string.hs_age_risk));
                }

                if (!waistNormal) {
                    if (warningMessage.length() > 0) {
                        warningMessage.append("\n\n");
                    }
                    warningMessage.append(getWaistAdvice(waistCm));
                } else if (!bmiNormal) {
                    // keep silent if waist normal but bmi not normal (bmi message already exists)
                }
            }
        }

        if (warningMessage.length() == 0 && !hasValidationError) {
            warningMessage.append(getString(R.string.hs_saved));
        }

        showHealthDialog(warningMessage.toString());

        if (hasValidationError) {
            return;
        }

        String finalNote = note;
        if (!weight.isEmpty() || !waist.isEmpty()) {
            String weightWaistText = getString(R.string.hs_weight_waist_age_note, weight, waist, age);
            finalNote = finalNote.isEmpty() ? weightWaistText : finalNote + " | " + weightWaistText;
        }

        String measuredAt = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(new Date());
        measurements.add(0, new HealthMeasurement(sugarValue, systolic, diastolic, finalNote, measuredAt));
        measurementsAdapter.notifyDataSetChanged();
        saveMeasurements();

        etSugarValue.setText("");
        etSystolicPressure.setText("");
        etDiastolicPressure.setText("");
        etWeightValue.setText("");
        etWaistValue.setText("");
        etMeasurementNote.setText("");
        etAgeValue.setText("");
    }

    private boolean isWaistNormal(double waistCm) {
        String gender = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("gender", "male");
        if ("female".equalsIgnoreCase(gender)) {
            return waistCm <= 80;
        }
        return waistCm <= 94;
    }

    private String getWaistAdvice(double waistCm) {
        String gender = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("gender", "male");
        boolean female = "female".equalsIgnoreCase(gender);

        if (female) {
            if (waistCm <= 88) {
                return getString(R.string.hs_waist_female_mid);
            }
            return getString(R.string.hs_waist_female_high);
        }

        if (waistCm <= 102) {
            return getString(R.string.hs_waist_male_mid);
        }
        return getString(R.string.hs_waist_male_high);
    }

    private void showHealthDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.hs_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private boolean isPositiveInteger(String value) {
        try { return Integer.parseInt(value) > 0; } catch (Exception ex) { return false; }
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

    // --- existing methods unchanged ---
    private void addHealthReminder(String reminderTitle, String reminderMessage, String timeInput, EditText sourceInput) {
        if (timeInput.isEmpty()) { Toast.makeText(this, getString(R.string.toast_choose_time), Toast.LENGTH_SHORT).show(); return; }
        Calendar reminderTime = parseReminderTime(timeInput);
        if (reminderTime == null) { Toast.makeText(this, getString(R.string.toast_choose_valid_time), Toast.LENGTH_SHORT).show(); return; }
        int requestCode = createRequestCode();
        scheduleReminder(requestCode, reminderTitle, reminderMessage, reminderTime);
        reminders.add(new HealthReminderItem(reminderTitle, timeInput, requestCode, reminderMessage));
        remindersAdapter.notifyDataSetChanged(); saveReminders(); sourceInput.setText("");
        Toast.makeText(this, getString(R.string.toast_daily_reminder_scheduled, reminderTitle), Toast.LENGTH_SHORT).show();
    }
    private void deleteReminder(int position) { HealthReminderItem reminder = reminders.remove(position); if (reminder.requestCode != 0) { cancelReminder(reminder);} remindersAdapter.notifyDataSetChanged(); saveReminders(); Toast.makeText(this, getString(R.string.toast_reminder_deleted), Toast.LENGTH_SHORT).show(); }
    private void deleteMeasurement(int position) { measurements.remove(position); measurementsAdapter.notifyDataSetChanged(); saveMeasurements(); Toast.makeText(this, getString(R.string.toast_measurement_deleted), Toast.LENGTH_SHORT).show(); }
    private void showTimePicker(EditText targetInput) { Calendar now = Calendar.getInstance(); TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> targetInput.setText(formatTime(hourOfDay, minute)), now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true); dialog.show(); }
    private String formatTime(int hour, int minute) { return String.format(Locale.US, "%02d:%02d", hour, minute); }
    private Calendar parseReminderTime(String input) { String[] parts = input.split(":"); if (parts.length != 2) { return null; } try { int hour = Integer.parseInt(parts[0]); int minute = Integer.parseInt(parts[1]); if (hour < 0 || hour > 23 || minute < 0 || minute > 59) { return null; } Calendar calendar = Calendar.getInstance(); calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0); if (calendar.before(Calendar.getInstance())) { calendar.add(Calendar.DAY_OF_MONTH, 1);} return calendar; } catch (NumberFormatException ex) { return null; } }
    private int createRequestCode() { return (int) (System.currentTimeMillis() & 0xfffffff); }
    private void scheduleReminder(int requestCode, String reminderTitle, String reminderMessage, Calendar reminderTime) { AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE); if (alarmManager == null) { return; } PendingIntent pendingIntent = createReminderPendingIntent(requestCode, reminderTitle, reminderMessage); alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent); }
    private void cancelReminder(HealthReminderItem reminder) { AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE); if (alarmManager == null) { return; } PendingIntent pendingIntent = createReminderPendingIntent(reminder.requestCode, reminder.title, reminder.notificationMessage); alarmManager.cancel(pendingIntent); pendingIntent.cancel(); }
    private PendingIntent createReminderPendingIntent(int requestCode, String reminderTitle, String reminderMessage) { Intent intent = new Intent(this, ReminderReceiver.class); intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminderTitle); intent.putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, reminderMessage); intent.putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, reminderTitle); return PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); }
    private void loadSavedReminders() { SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE); reminders.clear(); try { JSONArray savedItems = new JSONArray(preferences.getString(KEY_HEALTH_REMINDERS, "[]")); for (int i = 0; i < savedItems.length(); i++) { reminders.add(HealthReminderItem.fromJson(savedItems, i)); } } catch (JSONException ex) { preferences.edit().remove(KEY_HEALTH_REMINDERS).apply(); } remindersAdapter.notifyDataSetChanged(); }
    private void saveReminders() { JSONArray savedItems = new JSONArray(); for (HealthReminderItem reminder : reminders) { savedItems.put(reminder.toJson()); } getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_HEALTH_REMINDERS, savedItems.toString()).apply(); }
    private void loadSavedMeasurements() { SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE); measurements.clear(); try { JSONArray savedItems = new JSONArray(preferences.getString(KEY_HEALTH_MEASUREMENTS, "[]")); for (int i = 0; i < savedItems.length(); i++) { JSONObject item = savedItems.optJSONObject(i); if (item != null) { measurements.add(HealthMeasurement.fromJson(item)); } } } catch (JSONException ex) { preferences.edit().remove(KEY_HEALTH_MEASUREMENTS).apply(); } measurementsAdapter.notifyDataSetChanged(); }
    private void saveMeasurements() { JSONArray savedItems = new JSONArray(); for (HealthMeasurement measurement : measurements) { savedItems.put(measurement.toJson()); } getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_HEALTH_MEASUREMENTS, savedItems.toString()).apply(); }

    private static class HealthReminderItem { private final String title; private final String time; private final int requestCode; private final String notificationMessage; HealthReminderItem(String title, String time, int requestCode, String notificationMessage) { this.title = title; this.time = time; this.requestCode = requestCode; this.notificationMessage = notificationMessage; } static HealthReminderItem fromJson(JSONArray savedItems, int index) { JSONObject item = savedItems.optJSONObject(index); if (item == null) { return new HealthReminderItem(savedItems.optString(index), "", 0, ""); } String title = item.optString("title"); String message = item.optString("notificationMessage", "Time for " + title); return new HealthReminderItem(title, item.optString("time"), item.optInt("requestCode"), message); } JSONObject toJson() { JSONObject item = new JSONObject(); try { item.put("title", title); item.put("time", time); item.put("requestCode", requestCode); item.put("notificationMessage", notificationMessage); } catch (JSONException ignored) {} return item; } @Override public String toString() { if (time.isEmpty()) { return title; } return String.format(Locale.US, "%s - %s", title, time); } }

    private static class HealthMeasurement { private final String sugarValue; private final String systolicPressure; private final String diastolicPressure; private final String note; private final String measuredAt; HealthMeasurement(String sugarValue, String systolicPressure, String diastolicPressure, String note, String measuredAt) { this.sugarValue = sugarValue; this.systolicPressure = systolicPressure; this.diastolicPressure = diastolicPressure; this.note = note; this.measuredAt = measuredAt; } static HealthMeasurement fromJson(JSONObject item) { return new HealthMeasurement(item.optString("sugarValue"), item.optString("systolicPressure"), item.optString("diastolicPressure"), item.optString("note"), item.optString("measuredAt")); } JSONObject toJson() { JSONObject item = new JSONObject(); try { item.put("sugarValue", sugarValue); item.put("systolicPressure", systolicPressure); item.put("diastolicPressure", diastolicPressure); item.put("note", note); item.put("measuredAt", measuredAt); } catch (JSONException ignored) {} return item; } @Override public String toString() { ArrayList<String> parts = new ArrayList<>(); if (!sugarValue.isEmpty()) { parts.add("Sugar: " + sugarValue);} if (!systolicPressure.isEmpty() && !diastolicPressure.isEmpty()) { parts.add("Pressure: " + systolicPressure + "/" + diastolicPressure);} if (!note.isEmpty()) { parts.add("Note: " + note);} StringBuilder details = new StringBuilder(); for (int i = 0; i < parts.size(); i++) { if (i > 0) { details.append(" | "); } details.append(parts.get(i)); } return measuredAt + "\n" + details; } }
}
