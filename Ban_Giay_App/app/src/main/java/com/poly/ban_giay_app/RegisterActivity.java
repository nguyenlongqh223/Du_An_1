package com.poly.ban_giay_app;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private ProgressDialog progressDialog;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        apiService = ApiClient.getApiService();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.registering));
        progressDialog.setCancelable(false);

        initViews();
        bindActions();
    }

    private void initViews() {
        edtPhoneEmail = findViewById(R.id.edtPhoneEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String email = edtPhoneEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtPhoneEmail.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError(getString(R.string.error_password_required));
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.error_confirm_password));
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        String username = deriveUsername(email);
        String fullName = username;

        RegisterRequest request = new RegisterRequest(username, password, fullName, email);
        apiService.register(request).enqueue(new Callback<BaseResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<UserResponse>> call, Response<BaseResponse<UserResponse>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, NetworkUtils.extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<UserResponse>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
