package com.poly.ban_giay_app;

import android.app.ProgressDialog;
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
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.UserResponse;
import com.poly.ban_giay_app.network.request.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtPhoneEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private View btnBack;
    private ImageView btnTogglePassword, btnToggleConfirmPassword;
    private ProgressDialog progressDialog;
    private ApiService apiService;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        
        // Apply insets asynchronously to avoid blocking
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        apiService = ApiClient.getApiService();
        
        // Initialize ProgressDialog with proper configuration
        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.registering));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
        } catch (Exception e) {
            // Fallback if ProgressDialog fails
            android.util.Log.e("RegisterActivity", "Error creating ProgressDialog", e);
        }

        initViews();
        bindActions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Safely dismiss progress dialog
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            android.util.Log.e("RegisterActivity", "Error in onDestroy", e);
        }
    }

    private void initViews() {
        edtPhoneEmail = findViewById(R.id.edtPhoneEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());
        
        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());
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
    
    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            // Hide password
            edtConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off);
            isConfirmPasswordVisible = false;
        } else {
            // Show password
            edtConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_eye);
            isConfirmPasswordVisible = true;
        }
        // Move cursor to end
        edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
    }

    private void attemptRegister() {
        String email = edtPhoneEmail != null ? edtPhoneEmail.getText().toString().trim() : "";
        String password = edtPassword != null ? edtPassword.getText().toString().trim() : "";
        String confirmPassword = edtConfirmPassword != null ? edtConfirmPassword.getText().toString().trim() : "";

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

        if (TextUtils.isEmpty(confirmPassword)) {
            if (edtConfirmPassword != null) {
                edtConfirmPassword.setError(getString(R.string.error_confirm_password));
            }
            return;
        }

        if (!password.equals(confirmPassword)) {
            if (edtConfirmPassword != null) {
                edtConfirmPassword.setError(getString(R.string.error_password_mismatch));
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
            android.util.Log.e("RegisterActivity", "Error checking network", e);
            Toast.makeText(this, "Lỗi kiểm tra kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog safely
        try {
            if (progressDialog != null && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            android.util.Log.e("RegisterActivity", "Error showing progress dialog", e);
        }

        String username = deriveUsername(email);
        String fullName = username;

        RegisterRequest request = new RegisterRequest(username, password, fullName, email);
        apiService.register(request).enqueue(new Callback<BaseResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<UserResponse>> call, Response<BaseResponse<UserResponse>> response) {
                // Dismiss progress dialog safely
                try {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    android.util.Log.e("RegisterActivity", "Error dismissing progress dialog", e);
                }

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Toast.makeText(RegisterActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e) {
                        android.util.Log.e("RegisterActivity", "Error processing register response", e);
                        Toast.makeText(RegisterActivity.this, "Lỗi xử lý đăng ký", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = NetworkUtils.extractErrorMessage(response);
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<UserResponse>> call, Throwable t) {
                // Dismiss progress dialog safely
                try {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    android.util.Log.e("RegisterActivity", "Error dismissing progress dialog", e);
                }

                String errorMsg = t != null && t.getLocalizedMessage() != null 
                    ? t.getLocalizedMessage() 
                    : "Không thể kết nối đến server";
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                android.util.Log.e("RegisterActivity", "Register failed", t);
            }
        });
    }

    private String deriveUsername(String email) {
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            return "user_" + System.currentTimeMillis();
        }
        return email.substring(0, email.indexOf("@"));
    }
}
