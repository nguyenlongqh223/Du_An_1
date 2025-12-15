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

import com.poly.ban_giay_app.models.CartItem;
import com.poly.ban_giay_app.models.Product;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.OrderResponse;
import com.poly.ban_giay_app.network.request.OrderRequest;
import com.poly.ban_giay_app.network.request.PaymentRequest;
import com.poly.ban_giay_app.models.TransactionHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private boolean isFromCart;
    private CartManager cartManager;

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

        sessionManager = new SessionManager(this);
        cartManager = CartManager.getInstance();
        cartManager.setContext(this);

        // Get data from intent
        isFromCart = getIntent().getBooleanExtra("isFromCart", false);
        
        if (isFromCart) {
            // Từ cart - không cần product, sẽ lấy từ CartManager
        } else {
            // Từ buy now
            product = (Product) getIntent().getSerializableExtra("product");
            quantity = getIntent().getIntExtra("quantity", 1);
            selectedSize = getIntent().getStringExtra("selectedSize");

            if (product == null) {
                finish();
                return;
            }
        }

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
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        btnContinue.setEnabled(false);
        btnContinue.setText("Đang xử lý...");
        Toast.makeText(this, "Đang xử lý thanh toán...", Toast.LENGTH_SHORT).show();

        if (isFromCart) {
            // Xử lý thanh toán từ cart - tạo Order
            processOrderFromCart(cardholderName, cardNumber, expiryDate, paymentType);
        } else {
            // Xử lý thanh toán buy now - tạo Payment
            processPaymentBuyNow(cardholderName, cardNumber, expiryDate, paymentType);
        }
    }

    private void processOrderFromCart(String cardholderName, String cardNumber, String expiryDate, String paymentType) {
        List<CartItem> selectedItems = cartManager.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            btnContinue.setEnabled(true);
            btnContinue.setText("TIẾP TỤC");
            Toast.makeText(this, "Không có sản phẩm được chọn", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();
        OrderRequest request = new OrderRequest();
        request.setUserId(userId);
        
        List<OrderRequest.OrderItemRequest> orderItems = new ArrayList<>();
        long totalPrice = 0;
        
        for (CartItem cartItem : selectedItems) {
            if (cartItem == null || cartItem.product == null || cartItem.product.id == null) {
                continue;
            }
            
            long itemPrice = cartItem.gia > 0 ? cartItem.gia : 0;
            if (itemPrice == 0 && cartItem.product.priceNew != null) {
                String priceStr = cartItem.product.priceNew.replaceAll("[^0-9]", "");
                if (!priceStr.isEmpty()) {
                    itemPrice = Long.parseLong(priceStr);
                }
            }
            
            totalPrice += itemPrice * cartItem.quantity;
            
            OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(
                cartItem.product.id,
                cartItem.product.name,
                cartItem.quantity,
                cartItem.size,
                itemPrice
            );
            orderItems.add(orderItem);
        }
        
        request.setItems(orderItems);
        request.setTongTien(totalPrice);
        request.setDiaChiGiaoHang("");
        request.setSoDienThoai("");
        request.setGhiChu("Thanh toán bằng " + paymentType + ". Chủ thẻ: " + cardholderName);

        ApiService apiService = ApiClient.getApiService();
        apiService.createOrder(request).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                btnContinue.setEnabled(true);
                btnContinue.setText("TIẾP TỤC");

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<OrderResponse> body = response.body();
                    android.util.Log.d("CreditCardActivity", "Response success: " + body.getSuccess());
                    android.util.Log.d("CreditCardActivity", "Response message: " + body.getMessage());
                    
                    if (body.getSuccess()) {
                        OrderResponse orderResponse = body.getData();
                        if (orderResponse != null) {
                            android.util.Log.d("CreditCardActivity", "✅ Order created successfully!");
                            android.util.Log.d("CreditCardActivity", "Order ID: " + orderResponse.getId());
                            android.util.Log.d("CreditCardActivity", "Order Total: " + orderResponse.getTongTien());
                            
                            // Log order response JSON
                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                String orderJson = gson.toJson(orderResponse);
                                android.util.Log.d("CreditCardActivity", "Order Response JSON: " + orderJson);
                            } catch (Exception e) {
                                android.util.Log.e("CreditCardActivity", "Error serializing order response", e);
                            }
                            
                            Toast.makeText(CreditCardActivity.this, "Đặt hàng thành công! ID: " + orderResponse.getId(), Toast.LENGTH_LONG).show();
                            
                            // Save transaction history
                            saveTransactionHistory(orderResponse, "credit_card");
                        } else {
                            android.util.Log.w("CreditCardActivity", "⚠️ Order created but response data is null");
                            Toast.makeText(CreditCardActivity.this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                        }
                        
                        cartManager.removeSelectedItems();
                        Intent intent = new Intent(CreditCardActivity.this, OrderActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("shouldReload", true);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = body.getMessage() != null ? body.getMessage() : "Không thể tạo đơn hàng";
                        android.util.Log.e("CreditCardActivity", "❌ Order creation failed: " + errorMsg);
                        Toast.makeText(CreditCardActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    android.util.Log.e("CreditCardActivity", "❌ Response not successful. Code: " + response.code());
                    Toast.makeText(CreditCardActivity.this, "Lỗi khi xử lý thanh toán (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                btnContinue.setEnabled(true);
                btnContinue.setText("TIẾP TỤC");
                Toast.makeText(CreditCardActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processPaymentBuyNow(String cardholderName, String cardNumber, String expiryDate, String paymentType) {
        String userId = sessionManager.getUserId();
        String email = sessionManager.getEmail();

        String productName = product != null ? product.name : "";
        String productPrice = calculateTotalPrice();
        if (productPrice.isEmpty()) {
            productPrice = product != null ? product.priceNew : "";
        }

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

        ApiService apiService = ApiClient.getApiService();
        Call<BaseResponse<Object>> call = apiService.createPayment(paymentRequest);
        call.enqueue(new Callback<BaseResponse<Object>>() {
            @Override
            public void onResponse(Call<BaseResponse<Object>> call, Response<BaseResponse<Object>> response) {
                btnContinue.setEnabled(true);
                btnContinue.setText("TIẾP TỤC");

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Object> baseResponse = response.body();
                    if (baseResponse.getSuccess()) {
                        Toast.makeText(CreditCardActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Save transaction history for buy now
                        String productName = product != null ? product.name : "";
                        String productPrice = calculateTotalPrice();
                        saveTransactionHistoryBuyNow(productName, productPrice, "credit_card");
                        finish();
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Thanh toán thất bại";
                        Toast.makeText(CreditCardActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(CreditCardActivity.this, "Lỗi khi xử lý thanh toán (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Object>> call, Throwable t) {
                btnContinue.setEnabled(true);
                btnContinue.setText("TIẾP TỤC");
                Toast.makeText(CreditCardActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayProductInfo() {
        if (txtProductInfo == null) return;
        
        StringBuilder info = new StringBuilder();
        
        if (isFromCart) {
            // Hiển thị thông tin từ cart
            List<CartItem> selectedItems = cartManager.getSelectedItems();
            int itemCount = selectedItems != null ? selectedItems.size() : 0;
            long totalPrice = cartManager.getTotalPrice();
            
            info.append(itemCount).append(" sản phẩm");
            if (totalPrice > 0) {
                info.append(" • ").append(formatPrice(totalPrice));
            }
        } else {
            // Hiển thị thông tin từ product detail
            if (product != null) {
                String productName = product.name;
                String totalPrice = calculateTotalPrice();
                
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
            }
        }
        
        txtProductInfo.setText(info.toString());
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

    private void saveTransactionHistory(OrderResponse order, String paymentMethod) {
        try {
            TransactionHistoryManager transactionManager = TransactionHistoryManager.getInstance(this);
            SessionManager sessionManager = new SessionManager(this);
            String userName = sessionManager.getUserName();
            String productNames = "";
            long totalAmount = order.getTongTien() != null ? order.getTongTien() : 0;
            
            int totalQuantity = 0;
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                List<String> names = new ArrayList<>();
                for (OrderResponse.OrderItemResponse item : order.getItems()) {
                    names.add(item.getTenSanPham());
                    if (item.getSoLuong() != null) {
                        totalQuantity += item.getSoLuong();
                    }
                }
                productNames = String.join(", ", names);
            }
            
            String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
            String deliveryAddress = sessionManager.getDeliveryAddress();
            String phoneNumber = sessionManager.getPhone();
            String orderId = order.getId();
            TransactionHistory transaction = new TransactionHistory(
                userName,
                productNames.isEmpty() ? "Nhiều sản phẩm" : productNames,
                totalAmount,
                dateStr,
                paymentMethod,
                deliveryAddress != null ? deliveryAddress : "",
                phoneNumber != null ? phoneNumber : "",
                totalQuantity > 0 ? totalQuantity : 1,
                orderId != null ? orderId : ""
            );
            transactionManager.addTransaction(transaction);
        } catch (Exception e) {
            android.util.Log.e("CreditCardActivity", "Error saving transaction history", e);
        }
    }

    private void saveTransactionHistoryBuyNow(String productName, String productPrice, String paymentMethod) {
        try {
            TransactionHistoryManager transactionManager = TransactionHistoryManager.getInstance(this);
            SessionManager sessionManager = new SessionManager(this);
            String userName = sessionManager.getUserName();
            
            // Parse price
            long amount = 0;
            if (productPrice != null && !productPrice.isEmpty()) {
                String priceStr = productPrice.replaceAll("[^0-9]", "");
                if (!priceStr.isEmpty()) {
                    try {
                        amount = Long.parseLong(priceStr);
                    } catch (NumberFormatException e) {
                        android.util.Log.e("CreditCardActivity", "Error parsing price", e);
                    }
                }
            }
            
            String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
            String deliveryAddress = sessionManager.getDeliveryAddress();
            String phoneNumber = sessionManager.getPhone();
            // Buy now - lấy quantity từ intent hoặc mặc định là 1
            int quantity = getIntent().getIntExtra("quantity", 1);
            // Buy now không có orderId, để null
            TransactionHistory transaction = new TransactionHistory(
                userName,
                productName != null ? productName : "Sản phẩm",
                amount,
                dateStr,
                paymentMethod,
                deliveryAddress != null ? deliveryAddress : "",
                phoneNumber != null ? phoneNumber : "",
                quantity,
                null // Buy now không tạo order
            );
            transactionManager.addTransaction(transaction);
        } catch (Exception e) {
            android.util.Log.e("CreditCardActivity", "Error saving transaction history", e);
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
                Intent intent = new Intent(CreditCardActivity.this, CartActivity.class);
                startActivity(intent);
            });
        }

        // Trợ giúp
        View navHelp = findViewById(R.id.navHelp);
        if (navHelp != null) {
            navHelp.setOnClickListener(v -> {
                Intent intent = new Intent(CreditCardActivity.this, HelpActivity.class);
                startActivity(intent);
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

