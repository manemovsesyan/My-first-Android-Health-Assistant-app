package com.example.ReMedi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_remedi_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private EditText etLoginValue;
    private EditText etPassword;
    private RadioGroup loginTypeGroup;
    private RadioButton rbEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        Button btnLogin = findViewById(R.id.btnLogin);
        etLoginValue = findViewById(R.id.etLoginValue);
        etPassword = findViewById(R.id.etPassword);
        loginTypeGroup = findViewById(R.id.loginTypeGroup);
        rbEmail = findViewById(R.id.rbEmail);

        loginTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbEmail) {
                etLoginValue.setHint("Enter your email");
                etLoginValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                etLoginValue.setHint("Enter your phone number");
                etLoginValue.setInputType(InputType.TYPE_CLASS_PHONE);
            }
        });

        btnLogin.setOnClickListener(v -> {
            String value = etLoginValue.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean isEmailSelected = loginTypeGroup.getCheckedRadioButtonId() == rbEmail.getId();

            if (value.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in the field", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(LoginActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEmailSelected) {
                if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    Toast.makeText(LoginActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                String normalizedPhone = value.replaceAll("[^0-9+]", "");
                if (normalizedPhone.length() < 8 || normalizedPhone.length() > 15) {
                    Toast.makeText(LoginActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            preferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }
}
