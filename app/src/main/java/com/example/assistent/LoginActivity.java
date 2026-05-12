package com.example.assistent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "health_assistant_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_REGISTERED_LOGIN_TYPE = "registered_login_type";
    private static final String KEY_REGISTERED_LOGIN_VALUE = "registered_login_value";
    private static final String KEY_REGISTERED_PASSWORD = "registered_password";

    private EditText etLoginValue;
    private EditText etPassword;
    private RadioGroup loginTypeGroup;
    private RadioButton rbEmail;
    private TextView tvLoginValueLabel;

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
        tvLoginValueLabel = findViewById(R.id.tvLoginValueLabel);

        loginTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            etLoginValue.setText("");
            etLoginValue.setError(null);
            if (checkedId == R.id.rbEmail) {
                tvLoginValueLabel.setText("Email address");
                etLoginValue.setHint("example@mail.com");
                etLoginValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                tvLoginValueLabel.setText("Phone number");
                etLoginValue.setHint("+374 00 000000");
                etLoginValue.setInputType(InputType.TYPE_CLASS_PHONE);
            }
        });

        btnLogin.setOnClickListener(v -> {
            String value = etLoginValue.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean isEmailSelected = loginTypeGroup.getCheckedRadioButtonId() == rbEmail.getId();

            etLoginValue.setError(null);
            etPassword.setError(null);

            if (value.isEmpty()) {
                etLoginValue.setError("Please enter your email or phone");
                etLoginValue.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }

            String loginType;
            String normalizedValue;
            if (isEmailSelected) {
                if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    etLoginValue.setError("Please enter a valid email");
                    etLoginValue.requestFocus();
                    return;
                }
                loginType = "email";
                normalizedValue = value.toLowerCase();
            } else {
                String normalizedPhone = value.replaceAll("[^0-9+]", "");
                if (normalizedPhone.length() < 8 || normalizedPhone.length() > 15) {
                    etLoginValue.setError("Please enter a valid phone number");
                    etLoginValue.requestFocus();
                    return;
                }
                loginType = "phone";
                normalizedValue = normalizedPhone;
            }

            String savedLoginType = preferences.getString(KEY_REGISTERED_LOGIN_TYPE, null);
            String savedLoginValue = preferences.getString(KEY_REGISTERED_LOGIN_VALUE, null);
            String savedPassword = preferences.getString(KEY_REGISTERED_PASSWORD, null);

            if (savedLoginType == null || savedLoginValue == null || savedPassword == null) {
                preferences.edit()
                        .putString(KEY_REGISTERED_LOGIN_TYPE, loginType)
                        .putString(KEY_REGISTERED_LOGIN_VALUE, normalizedValue)
                        .putString(KEY_REGISTERED_PASSWORD, password)
                        .putBoolean(KEY_IS_LOGGED_IN, true)
                        .apply();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                return;
            }

            if (!savedLoginType.equals(loginType)
                    || !savedLoginValue.equals(normalizedValue)
                    || !savedPassword.equals(password)) {
                etLoginValue.setError("This account was not found on this device");
                etPassword.setError("Check your password");
                etLoginValue.requestFocus();
                return;
            }

            preferences.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }
}
