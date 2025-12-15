package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.UserResponse;
import com.poly.ban_giay_app.network.request.UpdateProfileRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateProfileActivity extends AppCompatActivity {
    private ImageView btnBack;
    private EditText edtFullName, edtPhone, edtAddress, edtDeliveryAddress;
    private Button btnSave;
    private SessionManager sessionManager;
    private ApiService apiService;
    private boolean isFromPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_profile);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        isFromPayment = getIntent().getBooleanExtra("isFromPayment", false);

        initViews();
        bindActions();
        loadCurrentProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtDeliveryAddress = findViewById(R.id.edtDeliveryAddress);
        btnSave = findViewById(R.id.btnSave);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (validateForm()) {
                saveProfile();
            }
        });
    }

    private void loadCurrentProfile() {
        String fullName = sessionManager.getUserName();
        String phone = sessionManager.getPhone();
        String address = sessionManager.getAddress();
        String deliveryAddress = sessionManager.getDeliveryAddress();

        if (!TextUtils.isEmpty(fullName)) {
            edtFullName.setText(fullName);
        }
        if (!TextUtils.isEmpty(phone)) {
            edtPhone.setText(phone);
        }
        if (!TextUtils.isEmpty(address)) {
            edtAddress.setText(address);
        }
        if (!TextUtils.isEmpty(deliveryAddress)) {
            edtDeliveryAddress.setText(deliveryAddress);
        }
    }

    private boolean validateForm() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String deliveryAddress = edtDeliveryAddress.getText().toString().trim();

        boolean isValid = true;

        // Validate tên
        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập tên");
            edtFullName.requestFocus();
            isValid = false;
        } else {
            edtFullName.setError(null);
        }

        // Validate số điện thoại
        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            edtPhone.requestFocus();
            isValid = false;
        } else if (phone.length() < 10) {
            edtPhone.setError("Số điện thoại phải có ít nhất 10 số");
            edtPhone.requestFocus();
            isValid = false;
        } else {
            edtPhone.setError(null);
        }

        // Validate địa chỉ email
        if (TextUtils.isEmpty(address)) {
            edtAddress.setError("Vui lòng nhập địa chỉ email");
            edtAddress.requestFocus();
            isValid = false;
        } else if (!address.contains("@gmail.com")) {
            edtAddress.setError("Địa chỉ email phải chứa @gmail.com");
            edtAddress.requestFocus();
            isValid = false;
        } else {
            edtAddress.setError(null);
        }

        // Validate địa chỉ nhận hàng
        if (TextUtils.isEmpty(deliveryAddress)) {
            edtDeliveryAddress.setError("Vui lòng nhập địa chỉ nhận hàng");
            edtDeliveryAddress.requestFocus();
            isValid = false;
        } else {
            edtDeliveryAddress.setError(null);
        }

        return isValid;
    }

    private void saveProfile() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String deliveryAddress = edtDeliveryAddress.getText().toString().trim();

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        // Lưu thông tin vào SessionManager (local storage)
        // Backend API endpoint không tồn tại, nên lưu local để sử dụng cho thanh toán
        sessionManager.updateProfileInfo(fullName, phone, address, deliveryAddress);
        
        // Simulate a short delay for better UX
        btnSave.postDelayed(() -> {
            btnSave.setEnabled(true);
            btnSave.setText("Lưu thông tin");
            
            Toast.makeText(UpdateProfileActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
            
            if (isFromPayment) {
                // Quay lại màn hình thanh toán
                setResult(RESULT_OK);
                finish();
            } else {
                finish();
            }
        }, 500);
    }
}

