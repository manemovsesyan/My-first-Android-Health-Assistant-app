package com.example.assistent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TreeSet;

public class CalendarActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_assistant_prefs";
    private static final String KEY_CALENDAR_TASKS = "calendar_tasks";
    private static final String NOTIFICATION_TITLE = "Calendar reminder";

    private TextView tvSelectedDate;
    private TextView tvCalendarTaskDates;
    private EditText etCalendarTask;
    private EditText etCalendarTime;
    private ArrayAdapter<CalendarTask> tasksAdapter;
    private final ArrayList<CalendarTask> allTasks = new ArrayList<>();
    private final ArrayList<CalendarTask> selectedDateTasks = new ArrayList<>();
    private final Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        CalendarView calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvCalendarTaskDates = findViewById(R.id.tvCalendarTaskDates);
        etCalendarTask = findViewById(R.id.etCalendarTask);
        etCalendarTime = findViewById(R.id.etCalendarTime);
        Button btnSaveCalendarTask = findViewById(R.id.btnSaveCalendarTask);
        ListView listCalendarTasks = findViewById(R.id.listCalendarTasks);

        tasksAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, selectedDateTasks);
        listCalendarTasks.setAdapter(tasksAdapter);

        loadSavedTasks();
        updateSelectedDateLabel();
        refreshSelectedDateTasks();
        updateTaskDatesLabel();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateSelectedDateLabel();
            refreshSelectedDateTasks();
        });

        etCalendarTime.setFocusable(false);
        etCalendarTime.setOnClickListener(v -> showTimePicker());
        btnSaveCalendarTask.setOnClickListener(v -> saveCalendarTask());
        listCalendarTasks.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteTask(position);
            return true;
        });
    }

    private void saveCalendarTask() {
        String taskText = etCalendarTask.getText().toString().trim();
        String time = etCalendarTime.getText().toString().trim();

        if (taskText.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please write a task and choose time", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar reminderDateTime = createReminderDateTime(time);
        if (reminderDateTime == null) {
            Toast.makeText(this, "Please choose a valid time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (reminderDateTime.before(Calendar.getInstance())) {
            Toast.makeText(this, "Please choose a future date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        int requestCode = createRequestCode();
        String dateKey = getSelectedDateKey();
        String message = formatDisplayDate(selectedDate) + " at " + time + ": " + taskText;
        scheduleTaskReminder(requestCode, taskText, message, reminderDateTime);

        allTasks.add(new CalendarTask(taskText, dateKey, time, requestCode, message));
        saveTasks();
        refreshSelectedDateTasks();
        updateTaskDatesLabel();
        etCalendarTask.setText("");
        etCalendarTime.setText("");

        Toast.makeText(this, "Calendar reminder saved. Long press it to delete.", Toast.LENGTH_SHORT).show();
    }

    private void deleteTask(int position) {
        CalendarTask task = selectedDateTasks.get(position);
        allTasks.remove(task);
        cancelTaskReminder(task);
        saveTasks();
        refreshSelectedDateTasks();
        updateTaskDatesLabel();
        Toast.makeText(this, "Calendar task deleted", Toast.LENGTH_SHORT).show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> etCalendarTime.setText(formatTime(hourOfDay, minute)),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        dialog.show();
    }

    private Calendar createReminderDateTime(String time) {
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

            Calendar reminder = (Calendar) selectedDate.clone();
            reminder.set(Calendar.HOUR_OF_DAY, hour);
            reminder.set(Calendar.MINUTE, minute);
            reminder.set(Calendar.SECOND, 0);
            reminder.set(Calendar.MILLISECOND, 0);
            return reminder;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private int createRequestCode() {
        return (int) (System.currentTimeMillis() & 0xfffffff);
    }

    private void scheduleTaskReminder(int requestCode, String taskText, String message, Calendar reminderDateTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createTaskPendingIntent(requestCode, taskText, message);
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderDateTime.getTimeInMillis(),
                pendingIntent
        );
    }

    private void cancelTaskReminder(CalendarTask task) {
        if (task.requestCode == 0) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = createTaskPendingIntent(task.requestCode, task.text, task.notificationMessage);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private PendingIntent createTaskPendingIntent(int requestCode, String taskText, String message) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, taskText);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, NOTIFICATION_TITLE);
        intent.putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, message);

        return PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void updateSelectedDateLabel() {
        tvSelectedDate.setText("Selected: " + formatDisplayDate(selectedDate));
    }

    private void refreshSelectedDateTasks() {
        String selectedKey = getSelectedDateKey();
        selectedDateTasks.clear();
        for (CalendarTask task : allTasks) {
            if (selectedKey.equals(task.dateKey)) {
                selectedDateTasks.add(task);
            }
        }
        tasksAdapter.notifyDataSetChanged();
    }

    private void updateTaskDatesLabel() {
        TreeSet<String> dates = new TreeSet<>();
        for (CalendarTask task : allTasks) {
            if (!task.dateKey.isEmpty()) {
                dates.add(task.dateKey);
            }
        }

        if (dates.isEmpty()) {
            tvCalendarTaskDates.setText("Days with reminders: none");
        } else {
            StringBuilder datesText = new StringBuilder();
            for (String date : dates) {
                if (datesText.length() > 0) {
                    datesText.append(", ");
                }
                datesText.append(date);
            }
            tvCalendarTaskDates.setText("Days with reminders: " + datesText);
        }
    }

    private String getSelectedDateKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.getTime());
    }

    private String formatDisplayDate(Calendar calendar) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(calendar.getTime());
    }

    private void loadSavedTasks() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        allTasks.clear();
        try {
            JSONArray savedItems = new JSONArray(preferences.getString(KEY_CALENDAR_TASKS, "[]"));
            for (int i = 0; i < savedItems.length(); i++) {
                JSONObject item = savedItems.optJSONObject(i);
                if (item != null) {
                    allTasks.add(CalendarTask.fromJson(item));
                }
            }
        } catch (JSONException ex) {
            preferences.edit().remove(KEY_CALENDAR_TASKS).apply();
        }
    }

    private void saveTasks() {
        JSONArray savedItems = new JSONArray();
        for (CalendarTask task : allTasks) {
            savedItems.put(task.toJson());
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_CALENDAR_TASKS, savedItems.toString())
                .apply();
    }

    private static class CalendarTask {
        private final String text;
        private final String dateKey;
        private final String time;
        private final int requestCode;
        private final String notificationMessage;

        CalendarTask(String text, String dateKey, String time, int requestCode, String notificationMessage) {
            this.text = text;
            this.dateKey = dateKey;
            this.time = time;
            this.requestCode = requestCode;
            this.notificationMessage = notificationMessage;
        }

        static CalendarTask fromJson(JSONObject item) {
            String text = item.optString("text");
            String time = item.optString("time");
            String message = item.optString("notificationMessage", time + ": " + text);
            return new CalendarTask(
                    text,
                    item.optString("dateKey"),
                    time,
                    item.optInt("requestCode"),
                    message
            );
        }

        JSONObject toJson() {
            JSONObject item = new JSONObject();
            try {
                item.put("text", text);
                item.put("dateKey", dateKey);
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
            return String.format(Locale.US, "%s - %s", time, text);
        }
    }
}
