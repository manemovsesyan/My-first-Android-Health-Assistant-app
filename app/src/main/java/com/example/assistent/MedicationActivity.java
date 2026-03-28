package com.example.assistent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MedicationActivity extends AppCompatActivity {

    private EditText etMedicineName;
    private EditText etMedicineTime;
    private LinearLayout remindersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication);

        etMedicineName = findViewById(R.id.etMedicineName);
        etMedicineTime = findViewById(R.id.etMedicineTime);
        Button btnAddReminder = findViewById(R.id.btnAddReminder);
        remindersContainer = findViewById(R.id.remindersContainer);

        btnAddReminder.setOnClickListener(v -> addReminder());
    }

    private void addReminder() {
        String medicineName = etMedicineName.getText().toString().trim();
        String medicineTime = etMedicineTime.getText().toString().trim();

        if (medicineName.isEmpty() || medicineTime.isEmpty()) {
            Toast.makeText(this, "Please enter medicine name and time", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView reminderItem = new TextView(this);
        reminderItem.setText("• " + medicineName + " - " + medicineTime);
        reminderItem.setTextSize(16f);
        reminderItem.setTextColor(getResources().getColor(android.R.color.black));
        reminderItem.setPadding(8, 8, 8, 8);
        remindersContainer.addView(reminderItem);

        etMedicineName.setText("");
        etMedicineTime.setText("");

        Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show();
    }
}
