package com.example.assistent;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MedicationActivity extends AppCompatActivity {

    private EditText etMedicineName;
    private EditText etMedicineTime;
    private ArrayAdapter<String> remindersAdapter;
    private final ArrayList<String> reminders = new ArrayList<>();

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

        btnAddReminder.setOnClickListener(v -> addReminder());
    }

    private void addReminder() {
        String medicineName = etMedicineName.getText().toString().trim();
        String medicineTime = etMedicineTime.getText().toString().trim();

        if (medicineName.isEmpty() || medicineTime.isEmpty()) {
            Toast.makeText(this, "Please enter medicine name and time", Toast.LENGTH_SHORT).show();
            return;
        }

        reminders.add(medicineName + " - " + medicineTime);
        remindersAdapter.notifyDataSetChanged();

        etMedicineName.setText("");
        etMedicineTime.setText("");

        Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show();
    }
}
