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
import com.poly.ban_giay_app.network.request.ResetPasswordRequest;
import com.poly.ban_giay_app.network.request.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {
    private View btnBack;
    private View layoutRequestOtp;
    private View layoutResetPassword;
    private EditText edtPhoneEmail;
    private EditText edtVerificationCode;
    private EditText edtNewPassword;
    private EditText edtConfirmNewPassword;
    private Button btnSendCode;
    private Button btnVerifyOtp;
    private Button btnResetPassword;
    private ProgressDialog progressDialog;
    private ApiService apiService;
    private String verifiedEmail;
    private String verifiedOtp;

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
        layoutRequestOtp = findViewById(R.id.layoutRequestOtp);
        layoutResetPassword = findViewById(R.id.layoutResetPassword);
        edtPhoneEmail = findViewById(R.id.edtPhoneEmail);
        edtVerificationCode = findViewById(R.id.edtVerificationCode);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        showRequestOtpStep();
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void showRequestOtpStep() {
        layoutRequestOtp.setVisibility(View.VISIBLE);
        layoutResetPassword.setVisibility(View.GONE);
    }

    private void showResetPasswordStep() {
        layoutRequestOtp.setVisibility(View.GONE);
        layoutResetPassword.setVisibility(View.VISIBLE);
    }

    private void sendOtp() {
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
                    edtVerificationCode.requestFocus();
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

    private void verifyOtp() {
        String email = edtPhoneEmail.getText().toString().trim();
        String otp = edtVerificationCode.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtPhoneEmail.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(otp)) {
            edtVerificationCode.setError(getString(R.string.error_otp_required));
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        apiService.verifyOtp(new VerifyOtpRequest(email, otp)).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    verifiedEmail = email;
                    verifiedOtp = otp;
                    showResetPasswordStep();
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

    private void resetPassword() {
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError(getString(R.string.error_password_required));
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmNewPassword.setError(getString(R.string.error_confirm_password));
            return;
        }

        if (!TextUtils.equals(newPassword, confirmPassword)) {
            edtConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        if (TextUtils.isEmpty(verifiedEmail) || TextUtils.isEmpty(verifiedOtp)) {
            Toast.makeText(this, R.string.error_missing_otp, Toast.LENGTH_SHORT).show();
            showRequestOtpStep();
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        apiService.resetPassword(new ResetPasswordRequest(verifiedEmail, verifiedOtp, newPassword))
                .enqueue(new Callback<BaseResponse<Void>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(ForgotPasswordActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
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
