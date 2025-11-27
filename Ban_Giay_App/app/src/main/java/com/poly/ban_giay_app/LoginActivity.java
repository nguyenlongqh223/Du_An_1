package com.poly.ban_giay_app;

import android.app.ProgressDialog;
import android.content.Intent;
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
import com.poly.ban_giay_app.network.model.AuthResponse;
import com.poly.ban_giay_app.network.request.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText edtPhoneEmail, edtPassword;
    private Button btnLogin, btnRegister, btnForgotPassword;
    private View btnBack;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.logging_in));
        progressDialog.setCancelable(false);

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
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = edtPhoneEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtPhoneEmail.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError(getString(R.string.error_password_required));
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    sessionManager.saveAuthSession(body.getToken(), body.getUser());
                    Toast.makeText(LoginActivity.this, body.getMessage(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, AccountActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, NetworkUtils.extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}