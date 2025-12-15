package com.poly.ban_giay_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChangePasswordActivity extends AppCompatActivity {
    private ImageView btnBack;
    private EditText edtOldPassword, edtNewPassword, edtConfirmPassword;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        bindActions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (validateForm()) {
                // TODO: Implement password change logic
                Toast.makeText(this, "Chức năng đổi mật khẩu đang được phát triển", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateForm() {
        String oldPassword = edtOldPassword.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(oldPassword)) {
            edtOldPassword.setError("Vui lòng nhập mật khẩu cũ");
            edtOldPassword.requestFocus();
            isValid = false;
        } else {
            edtOldPassword.setError(null);
        }

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError("Vui lòng nhập mật khẩu mới");
            edtNewPassword.requestFocus();
            isValid = false;
        } else if (newPassword.length() < 6) {
            edtNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtNewPassword.requestFocus();
            isValid = false;
        } else {
            edtNewPassword.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError("Vui lòng xác nhận mật khẩu mới");
            edtConfirmPassword.requestFocus();
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            isValid = false;
        } else {
            edtConfirmPassword.setError(null);
        }

        return isValid;
    }
}

