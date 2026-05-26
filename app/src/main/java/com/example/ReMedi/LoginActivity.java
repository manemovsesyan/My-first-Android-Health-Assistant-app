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

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_remedi_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_APP_LANGUAGE = "app_language";

    private EditText etLoginValue;
    private EditText etPassword;
    private RadioGroup loginTypeGroup;
    private RadioButton rbEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        applySavedLanguage(preferences.getString(KEY_APP_LANGUAGE, "en"));
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

        findViewById(R.id.btnLangHy).setOnClickListener(v -> setLanguage("hy"));
        findViewById(R.id.btnLangEn).setOnClickListener(v -> setLanguage("en"));
        findViewById(R.id.btnLangRu).setOnClickListener(v -> setLanguage("ru"));

        loginTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbEmail) {
                etLoginValue.setHint(R.string.login_enter_email);
                etLoginValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                etLoginValue.setHint(R.string.login_enter_phone);
                etLoginValue.setInputType(InputType.TYPE_CLASS_PHONE);
            }
        });

        btnLogin.setOnClickListener(v -> {
            String value = etLoginValue.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean isEmailSelected = loginTypeGroup.getCheckedRadioButtonId() == rbEmail.getId();

            if (value.isEmpty()) {
                Toast.makeText(LoginActivity.this, R.string.login_fill_field, Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(LoginActivity.this, R.string.login_password_length, Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEmailSelected) {
                if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    Toast.makeText(LoginActivity.this, R.string.login_valid_email, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                String normalizedPhone = value.replaceAll("[^0-9+]", "");
                if (normalizedPhone.length() < 8 || normalizedPhone.length() > 15) {
                    Toast.makeText(LoginActivity.this, R.string.login_valid_phone, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            preferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }

    private void setLanguage(String languageCode) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putString(KEY_APP_LANGUAGE, languageCode).apply();
        applySavedLanguage(languageCode);
        recreate();
    }

    private void applySavedLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

}
