package com.poly.ban_giay_app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.AuthResponse;
import com.poly.ban_giay_app.network.request.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edtPhoneEmail, edtPassword;
    private Button btnLogin, btnRegister, btnForgotPassword;
    private View btnBack;
    private ImageView btnTogglePassword;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private ApiService apiService;
    private boolean isPasswordVisible = false;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Safely dismiss progress dialog
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error in onDestroy", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        
        // Apply insets asynchronously to avoid blocking
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        
        // Initialize ProgressDialog with proper configuration
        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.logging_in));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
        } catch (Exception e) {
            // Fallback if ProgressDialog fails
            android.util.Log.e("LoginActivity", "Error creating ProgressDialog", e);
        }

        initViews();
        bindActions();
    }

    private void initViews() {
        edtPhoneEmail = findViewById(R.id.edtPhoneEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnBack = findViewById(R.id.btnBack);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnLogin.setOnClickListener(v -> attemptLogin());
        
        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
    }
    
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = false;
        } else {
            // Show password
            edtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
            isPasswordVisible = true;
        }
        // Move cursor to end
        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void attemptLogin() {
        String email = edtPhoneEmail != null ? edtPhoneEmail.getText().toString().trim() : "";
        String password = edtPassword != null ? edtPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            if (edtPhoneEmail != null) {
                edtPhoneEmail.setError(getString(R.string.error_email_required));
            }
            return;
        }

        if (TextUtils.isEmpty(password)) {
            if (edtPassword != null) {
                edtPassword.setError(getString(R.string.error_password_required));
            }
            return;
        }

        // Check network connection - non-blocking
        try {
            if (!NetworkUtils.isConnected(this)) {
                Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error checking network", e);
            Toast.makeText(this, "Lỗi kiểm tra kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog safely
        try {
            if (progressDialog != null && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error showing progress dialog", e);
        }

        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                // Dismiss progress dialog safely
                try {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    android.util.Log.e("LoginActivity", "Error dismissing progress dialog", e);
                }

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        AuthResponse body = response.body();
                        sessionManager.saveAuthSession(body.getToken(), body.getUser());
                        Toast.makeText(LoginActivity.this, body.getMessage(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, AccountActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        android.util.Log.e("LoginActivity", "Error processing login response", e);
                        Toast.makeText(LoginActivity.this, "Lỗi xử lý đăng nhập", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = NetworkUtils.extractErrorMessage(response);
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Dismiss progress dialog safely
                try {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    android.util.Log.e("LoginActivity", "Error dismissing progress dialog", e);
                }

                String errorMsg = t != null && t.getLocalizedMessage() != null 
                    ? t.getLocalizedMessage() 
                    : "Không thể kết nối đến server";
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                android.util.Log.e("LoginActivity", "Login failed", t);
            }
        });
    }
}