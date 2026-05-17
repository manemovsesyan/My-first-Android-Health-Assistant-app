package com.example.ReMedi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class EmergencyActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_remedi_prefs";
    private static final String KEY_EMERGENCY_CONTACTS = "emergency_contacts";
    private static final int CALL_PERMISSION_REQUEST_CODE = 2001;

    private EditText etContactName;
    private EditText etContactPhone;
    private ArrayAdapter<EmergencyContact> contactsAdapter;
    private final ArrayList<EmergencyContact> contacts = new ArrayList<>();
    private String pendingCallNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        etContactName = findViewById(R.id.etContactName);
        etContactPhone = findViewById(R.id.etContactPhone);
        Button btnSaveContact = findViewById(R.id.btnSaveContact);
        Button btnCallPrimaryContact = findViewById(R.id.btnCallPrimaryContact);
        ListView listEmergencyContacts = findViewById(R.id.listEmergencyContacts);

        contactsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contacts);
        listEmergencyContacts.setAdapter(contactsAdapter);
        loadSavedContacts();

        btnSaveContact.setOnClickListener(v -> saveContact());
        btnCallPrimaryContact.setOnClickListener(v -> callPrimaryContact());
        listEmergencyContacts.setOnItemClickListener((parent, view, position, id) -> callContact(contacts.get(position)));
        listEmergencyContacts.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteContact(position);
            return true;
        });
    }

    private void saveContact() {
        String name = etContactName.getText().toString().trim();
        String phone = normalizePhone(etContactPhone.getText().toString().trim());

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter contact name and phone", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.length() < 7 || phone.length() > 15) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        contacts.add(new EmergencyContact(name, phone));
        contactsAdapter.notifyDataSetChanged();
        saveContacts();
        etContactName.setText("");
        etContactPhone.setText("");

        Toast.makeText(this, "Emergency contact saved", Toast.LENGTH_SHORT).show();
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9+]", "");
    }

    private void callPrimaryContact() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Please save an emergency contact first", Toast.LENGTH_SHORT).show();
            return;
        }
        callContact(contacts.get(0));
    }

    private void callContact(EmergencyContact contact) {
        requestCall(contact.phone);
    }

    private void requestCall(String phoneNumber) {
        pendingCallNumber = phoneNumber;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            startEmergencyCall(phoneNumber);
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PERMISSION_REQUEST_CODE
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void startEmergencyCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && pendingCallNumber != null) {
            startEmergencyCall(pendingCallNumber);
        } else if (requestCode == CALL_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Call permission is needed for one-tap emergency calls", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteContact(int position) {
        contacts.remove(position);
        contactsAdapter.notifyDataSetChanged();
        saveContacts();
        Toast.makeText(this, "Emergency contact deleted", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedContacts() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        contacts.clear();
        try {
            JSONArray savedItems = new JSONArray(preferences.getString(KEY_EMERGENCY_CONTACTS, "[]"));
            for (int i = 0; i < savedItems.length(); i++) {
                JSONObject item = savedItems.optJSONObject(i);
                if (item != null) {
                    contacts.add(EmergencyContact.fromJson(item));
                }
            }
        } catch (JSONException ex) {
            preferences.edit().remove(KEY_EMERGENCY_CONTACTS).apply();
        }
        contactsAdapter.notifyDataSetChanged();
    }

    private void saveContacts() {
        JSONArray savedItems = new JSONArray();
        for (EmergencyContact contact : contacts) {
            savedItems.put(contact.toJson());
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_EMERGENCY_CONTACTS, savedItems.toString())
                .apply();
    }

    private static class EmergencyContact {
        private final String name;
        private final String phone;

        EmergencyContact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        static EmergencyContact fromJson(JSONObject item) {
            return new EmergencyContact(
                    item.optString("name"),
                    item.optString("phone")
            );
        }

        JSONObject toJson() {
            JSONObject item = new JSONObject();
            try {
                item.put("name", name);
                item.put("phone", phone);
            } catch (JSONException ignored) {

            }
            return item;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%s - %s", name, phone);
        }
    }
}
