package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.poly.ban_giay_app.models.Product;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.request.PaymentRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreditCardActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView txtProductInfo;
    private AppCompatEditText edtCardholderName, edtCardNumber, edtExpiryDate;
    private Button btnContinue;
    private Product product;
    private int quantity;
    private String selectedSize;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_credit_card);

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get data from intent
        product = (Product) getIntent().getSerializableExtra("product");
        quantity = getIntent().getIntExtra("quantity", 1);
        selectedSize = getIntent().getStringExtra("selectedSize");

        if (product == null) {
            finish();
            return;
        }

        sessionManager = new SessionManager(this);

        initViews();
        bindActions();
        displayProductInfo();
        setupBottomNavigation();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtProductInfo = findViewById(R.id.txtProductInfo);
        edtCardholderName = findViewById(R.id.edtCardholderName);
        edtCardNumber = findViewById(R.id.edtCardNumber);
        edtExpiryDate = findViewById(R.id.edtExpiryDate);
        btnContinue = findViewById(R.id.btnContinue);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());

        // Format card number with spaces
        edtCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Tránh vòng lặp vô hạn
                if (isFormatting) {
                    return;
                }

                String input = s.toString().replaceAll(" ", "");
                // Chỉ format khi cần thiết (không phải khi đang xóa)
                if (input.length() > 0 && input.length() % 4 == 0 && input.length() < 16) {
                    isFormatting = true;
                    try {
                        StringBuilder formatted = new StringBuilder();
                        for (int i = 0; i < input.length(); i += 4) {
                            if (i > 0) formatted.append(" ");
                            formatted.append(input.substring(i, Math.min(i + 4, input.length())));
                        }
                        String formattedStr = formatted.toString();
                        
                        // Chỉ cập nhật nếu khác với text hiện tại
                        if (!formattedStr.equals(s.toString())) {
                            edtCardNumber.removeTextChangedListener(this);
                            edtCardNumber.setText(formattedStr);
                            edtCardNumber.setSelection(formattedStr.length());
                            edtCardNumber.addTextChangedListener(this);
                        }
                    } finally {
                        isFormatting = false;
                    }
                }
            }
        });

        // Format expiry date MM/YY
        edtExpiryDate.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Tránh vòng lặp vô hạn
                if (isFormatting) {
                    return;
                }

                String input = s.toString().replaceAll("/", "");
                if (input.length() >= 2 && input.length() <= 4) {
                    isFormatting = true;
                    try {
                        String formatted = input.substring(0, 2);
                        if (input.length() > 2) {
                            formatted += "/" + input.substring(2, Math.min(4, input.length()));
                        }
                        
                        // Chỉ cập nhật nếu khác với text hiện tại
                        if (!formatted.equals(s.toString())) {
                            edtExpiryDate.removeTextChangedListener(this);
                            edtExpiryDate.setText(formatted);
                            edtExpiryDate.setSelection(formatted.length());
                            edtExpiryDate.addTextChangedListener(this);
                        }
                    } finally {
                        isFormatting = false;
                    }
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            if (validateInput()) {
                processPayment("credit_card");
            }
        });
    }

    private boolean validateInput() {
        String cardholderName = edtCardholderName.getText().toString().trim();
        String cardNumber = edtCardNumber.getText().toString().replaceAll(" ", "").trim();
        String expiryDate = edtExpiryDate.getText().toString().trim();

        if (cardholderName.isEmpty()) {
            edtCardholderName.setError("Vui lòng nhập tên chủ thẻ");
            edtCardholderName.requestFocus();
            return false;
        }

        if (cardNumber.isEmpty() || cardNumber.length() < 16) {
            edtCardNumber.setError("Số thẻ không hợp lệ");
            edtCardNumber.requestFocus();
            return false;
        }

        if (expiryDate.isEmpty() || expiryDate.length() < 5) {
            edtExpiryDate.setError("Ngày hết hạn không hợp lệ");
            edtExpiryDate.requestFocus();
            return false;
        }

        return true;
    }

    private void processPayment(String paymentType) {
        String cardholderName = edtCardholderName.getText().toString().trim();
        String cardNumber = edtCardNumber.getText().toString().replaceAll(" ", "").trim();
        String expiryDate = edtExpiryDate.getText().toString().trim();

        // Get user info from session
        String userId = sessionManager.getUserId();
        String email = sessionManager.getEmail();

        // Get product info
        String productName = product != null ? product.name : "";
        // Tính giá tổng dựa trên số lượng
        String productPrice = calculateTotalPrice();
        if (productPrice.isEmpty()) {
            productPrice = product != null ? product.priceNew : "";
        }

        // Create payment request
        PaymentRequest paymentRequest = new PaymentRequest(
            userId,
            email,
            cardholderName,
            cardNumber,
            paymentType,
            productName,
            productPrice,
            quantity,
            selectedSize != null ? selectedSize : "",
            expiryDate
        );

        // Show loading
        btnContinue.setEnabled(false);
        btnContinue.setText("Đang xử lý...");
        Toast.makeText(this, "Đang xử lý thanh toán...", Toast.LENGTH_SHORT).show();

        // Debug log
        android.util.Log.d("Payment", "=== Payment Request ===");
        android.util.Log.d("Payment", "User ID: " + userId);
        android.util.Log.d("Payment", "Email: " + email);
        android.util.Log.d("Payment", "Cardholder: " + cardholderName);
        android.util.Log.d("Payment", "Card Number: " + cardNumber);
        android.util.Log.d("Payment", "Payment Type: " + paymentType);
        android.util.Log.d("Payment", "Product: " + productName);
        android.util.Log.d("Payment", "Price: " + productPrice);
        android.util.Log.d("Payment", "Quantity: " + quantity);
        android.util.Log.d("Payment", "Size: " + (selectedSize != null ? selectedSize : ""));
        android.util.Log.d("Payment", "Expiry Date: " + expiryDate);
        android.util.Log.d("Payment", "API Base URL: " + com.poly.ban_giay_app.BuildConfig.API_BASE_URL);

        // Call API
        ApiService apiService = ApiClient.getApiService();
        Call<BaseResponse<Object>> call = apiService.createPayment(paymentRequest);
        call.enqueue(new Callback<BaseResponse<Object>>() {
            @Override
            public void onResponse(Call<BaseResponse<Object>> call, Response<BaseResponse<Object>> response) {
                btnContinue.setEnabled(true);
                btnContinue.setText("TIẾP TỤC");

                android.util.Log.d("Payment", "=== Payment Response ===");
                android.util.Log.d("Payment", "Response Code: " + response.code());
                android.util.Log.d("Payment", "Is Successful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Object> baseResponse = response.body();
                    android.util.Log.d("Payment", "Response success: " + baseResponse.getSuccess());
                    android.util.Log.d("Payment", "Response message: " + baseResponse.getMessage());
                    
                    if (baseResponse.getSuccess()) {
                        android.util.Log.d("Payment", "Payment saved successfully!");
                        Toast.makeText(CreditCardActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                        // Navigate to success screen or back
                        finish();
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Thanh toán thất bại";
                        Toast.makeText(CreditCardActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        android.util.Log.e("Payment", "Payment failed: " + errorMsg);
                    }
                } else {
                    String errorBody = "Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("Payment", "Error reading error body", e);
                    }
                    android.util.Log.e("Payment", "Response not successful. Code: " + response.code() + ", Body: " + errorBody);
                    Toast.makeText(CreditCardActivity.this, "Lỗi khi xử lý thanh toán (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Object>> call, Throwable t) {
                btnContinue.setEnabled(true);
                btnContinue.setText("TIẾP TỤC");
                android.util.Log.e("Payment", "API call failed", t);
                Toast.makeText(CreditCardActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayProductInfo() {
        if (product != null && txtProductInfo != null) {
            try {
                String productName = product.name;
                
                // Tính giá tổng dựa trên số lượng
                String totalPrice = calculateTotalPrice();
                
                StringBuilder info = new StringBuilder();
                if (productName != null) {
                    info.append(productName);
                }
                
                if (totalPrice != null && !totalPrice.isEmpty()) {
                    if (info.length() > 0) {
                        info.append(" • ");
                    }
                    info.append(totalPrice);
                }
                
                if (selectedSize != null && !selectedSize.isEmpty()) {
                    if (info.length() > 0) {
                        info.append(" • ");
                    }
                    info.append("Size ").append(selectedSize);
                }
                
                txtProductInfo.setText(info.toString());
            } catch (Exception e) {
                android.util.Log.e("CreditCardActivity", "Error displaying product info", e);
                // Fallback: hiển thị tên sản phẩm đơn giản
                if (product.name != null) {
                    txtProductInfo.setText(product.name);
                }
            }
        }
    }
    
    /**
     * Tính giá tổng dựa trên số lượng
     */
    private String calculateTotalPrice() {
        if (product == null || product.priceNew == null || product.priceNew.isEmpty()) {
            return "";
        }
        
        // Parse giá từ string (loại bỏ ký tự ₫, dấu chấm, dấu phẩy)
        long priceValue = parsePrice(product.priceNew);
        
        // Nhân với số lượng
        long totalPrice = priceValue * quantity;
        
        // Format lại giá với ký tự ₫
        return formatPrice(totalPrice);
    }
    
    /**
     * Parse giá từ string (ví dụ: "500.000₫" -> 500000)
     */
    private long parsePrice(String priceString) {
        if (priceString == null || priceString.isEmpty()) {
            return 0;
        }
        try {
            // Loại bỏ ký tự ₫, dấu chấm, dấu phẩy, khoảng trắng - tối ưu hơn
            StringBuilder cleaned = new StringBuilder();
            for (char c : priceString.toCharArray()) {
                if (Character.isDigit(c)) {
                    cleaned.append(c);
                }
            }
            if (cleaned.length() == 0) {
                return 0;
            }
            return Long.parseLong(cleaned.toString());
        } catch (NumberFormatException e) {
            android.util.Log.e("CreditCardActivity", "Error parsing price: " + priceString, e);
            return 0;
        }
    }
    
    /**
     * Format giá với ký tự ₫ (ví dụ: 500000 -> "500.000₫")
     */
    private String formatPrice(long price) {
        if (price <= 0) {
            return "0₫";
        }
        try {
            // Format với dấu chấm ngăn cách hàng nghìn
            String formatted = String.format("%,d", price).replace(",", ".");
            return formatted + "₫";
        } catch (Exception e) {
            android.util.Log.e("CreditCardActivity", "Error formatting price: " + price, e);
            return String.valueOf(price) + "₫";
        }
    }

    private void setupBottomNavigation() {
        // Trang chủ
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(CreditCardActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Danh mục
        View navCategories = findViewById(R.id.navCategories);
        if (navCategories != null) {
            navCategories.setOnClickListener(v -> {
                Intent intent = new Intent(CreditCardActivity.this, CategoriesActivity.class);
                startActivity(intent);
            });
        }

        // Giỏ hàng
        View navCart = findViewById(R.id.navCart);
        if (navCart != null) {
            navCart.setOnClickListener(v -> {
                Toast.makeText(this, "Tính năng giỏ hàng đang phát triển", Toast.LENGTH_SHORT).show();
            });
        }

        // Trợ giúp
        View navHelp = findViewById(R.id.navHelp);
        if (navHelp != null) {
            navHelp.setOnClickListener(v -> {
                Toast.makeText(this, "Tính năng trợ giúp đang phát triển", Toast.LENGTH_SHORT).show();
            });
        }

        // Tài khoản
        View navAccount = findViewById(R.id.navAccount);
        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(CreditCardActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(CreditCardActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}

