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
import com.poly.ban_giay_app.network.request.ForgotPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {
    private View btnBack;
    private EditText edtPhoneEmail;
    private Button btnSendCode;
    private ProgressDialog progressDialog;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        apiService = ApiClient.getApiService();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.sending_request));
        progressDialog.setCancelable(false);

        initViews();
        bindActions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtPhoneEmail = findViewById(R.id.edtPhoneEmail);
        btnSendCode = findViewById(R.id.btnSendCode);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendRequest());
    }

    private void sendRequest() {
        String email = edtPhoneEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            edtPhoneEmail.setError(getString(R.string.error_email_required));
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        apiService.forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ForgotPasswordActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, NetworkUtils.extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
