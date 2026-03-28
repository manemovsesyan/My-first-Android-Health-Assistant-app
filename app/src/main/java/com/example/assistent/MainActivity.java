package com.example.assistent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnMedication, btnSugar, btnEmergency, btnNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMedication = findViewById(R.id.btnMedication);
        btnSugar = findViewById(R.id.btnSugar);
        btnEmergency = findViewById(R.id.btnEmergency);
        btnNotes = findViewById(R.id.btnNotes);

        btnMedication.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MedicationActivity.class);
            startActivity(intent);
        });

        btnSugar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SugarActivity.class);
            startActivity(intent);
        });

        btnEmergency.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmergencyActivity.class);
            startActivity(intent);
        });

        btnNotes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotesActivity.class);
            startActivity(intent);
        });
    }
}
