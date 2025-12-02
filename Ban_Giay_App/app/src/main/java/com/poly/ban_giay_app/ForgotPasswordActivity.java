package com.poly.ban_giay_app;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.poly.ban_giay_app.network.request.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private View btnBack;
    private TextView txtDescription;
    private LinearLayout layoutOtpStep;
    private LinearLayout layoutNewPasswordStep;
    private EditText edtPhoneEmail;
    private EditText edtVerificationCode;
    private EditText edtNewPassword;
    private EditText edtConfirmNewPassword;
    private Button btnSendCode;
    private Button btnVerifyOtp;
    private Button btnResetPassword;
    private ProgressDialog progressDialog;
    private ApiService apiService;
    private String userEmail; // Lưu email để dùng cho verify OTP
    private String cachedOtp;

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
        progressDialog.setMessage(getString(R.string.forgot_password_processing));
        progressDialog.setCancelable(false);

        initViews();
        bindActions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtDescription = findViewById(R.id.txtDescription);
        layoutOtpStep = findViewById(R.id.layoutOtpStep);
        layoutNewPasswordStep = findViewById(R.id.layoutNewPasswordStep);
        edtPhoneEmail = findViewById(R.id.edtPhoneEmail);
        edtVerificationCode = findViewById(R.id.edtVerificationCode);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> requestForgotPassword());
        btnVerifyOtp.setOnClickListener(v -> onVerifyOtpClicked());
        btnResetPassword.setOnClickListener(v -> verifyOtpAndResetPassword());
    }

    private void requestForgotPassword() {
        String email = edtPhoneEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            edtPhoneEmail.setError(getString(R.string.error_email_required));
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Đang gửi mã xác nhận...");
        progressDialog.show();
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        apiService.forgotPassword(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    userEmail = email; // Lưu email để dùng cho verify OTP
                    cachedOtp = null;
                    Toast.makeText(ForgotPasswordActivity.this,
                            response.body().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Hiển thị form nhập OTP
                    enableOtpInput();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            NetworkUtils.extractErrorMessage(response),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this,
                        "Lỗi: " + (t.getLocalizedMessage() != null ? t.getLocalizedMessage() : "Không thể kết nối đến server"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableOtpInput() {
        edtVerificationCode.setEnabled(true);
        btnVerifyOtp.setEnabled(true);
        btnVerifyOtp.setAlpha(1f);
        edtPhoneEmail.setEnabled(false);
        Toast.makeText(this, R.string.forgot_password_otp_sent, Toast.LENGTH_SHORT).show();
    }

    private void onVerifyOtpClicked() {
        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, R.string.error_otp_not_requested, Toast.LENGTH_SHORT).show();
            return;
        }

        String otp = edtVerificationCode.getText().toString().trim();
        if (TextUtils.isEmpty(otp)) {
            edtVerificationCode.setError(getString(R.string.error_otp_required));
            return;
        }

        cachedOtp = otp;
        showNewPasswordForm();
    }

    private void showNewPasswordForm() {
        layoutOtpStep.setVisibility(View.GONE);
        layoutNewPasswordStep.setVisibility(View.VISIBLE);
        txtDescription.setText(R.string.forgot_password_step2_desc);
    }

    private void verifyOtpAndResetPassword() {
        String otp = !TextUtils.isEmpty(cachedOtp) ? cachedOtp : edtVerificationCode.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            Toast.makeText(this, R.string.error_otp_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError("Vui lòng nhập mật khẩu mới");
            return;
        }

        if (newPassword.length() < 6) {
            edtNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmNewPassword.setError("Vui lòng xác nhận mật khẩu");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            edtConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Lỗi: Không tìm thấy email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Đang đặt lại mật khẩu...");
        progressDialog.show();
        VerifyOtpRequest request = new VerifyOtpRequest(userEmail, otp, newPassword, confirmPassword);
        apiService.verifyOtpAndResetPassword(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            response.body().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish(); // Quay về màn hình đăng nhập
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            NetworkUtils.extractErrorMessage(response),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this,
                        "Lỗi: " + (t.getLocalizedMessage() != null ? t.getLocalizedMessage() : "Không thể kết nối đến server"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}