package com.example.assistent;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_assistant_prefs";
    private static final String KEY_DAILY_NOTES = "daily_notes";

    private EditText etDailyNote;
    private ArrayAdapter<String> notesAdapter;
    private final ArrayList<String> notes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        etDailyNote = findViewById(R.id.etDailyNote);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);
        ListView listNotes = findViewById(R.id.listNotes);

        notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notes);
        listNotes.setAdapter(notesAdapter);
        loadSavedNotes();

        btnSaveNote.setOnClickListener(v -> saveNote());
        listNotes.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteNote(position);
            return true;
        });
    }

    private void saveNote() {
        String noteText = etDailyNote.getText().toString().trim();
        if (noteText.isEmpty()) {
            Toast.makeText(this, "Please write a note first", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(new Date());
        notes.add(0, date + "\n" + noteText);
        notesAdapter.notifyDataSetChanged();
        saveNotes();
        etDailyNote.setText("");

        Toast.makeText(this, "Note saved. Long press it to delete.", Toast.LENGTH_SHORT).show();
    }

    private void deleteNote(int position) {
        notes.remove(position);
        notesAdapter.notifyDataSetChanged();
        saveNotes();
        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedNotes() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        notes.clear();
        try {
            JSONArray savedItems = new JSONArray(preferences.getString(KEY_DAILY_NOTES, "[]"));
            for (int i = 0; i < savedItems.length(); i++) {
                notes.add(savedItems.optString(i));
            }
        } catch (JSONException ex) {
            preferences.edit().remove(KEY_DAILY_NOTES).apply();
        }
        notesAdapter.notifyDataSetChanged();
    }

    private void saveNotes() {
        JSONArray savedItems = new JSONArray();
        for (String note : notes) {
            savedItems.put(note);
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_DAILY_NOTES, savedItems.toString())
                .apply();
    }
}
