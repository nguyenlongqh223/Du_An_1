package com.poly.ban_giay_app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BankPaymentActivity extends AppCompatActivity {
    private LinearLayout btnBack;
    private ImageView imgQRCode;
    private TextView txtBankName, txtAccountName, txtAccountNumber, txtAmount;
    private Button btnCopyAccountNumber, btnConfirmPayment;
    private Product product;
    private int quantity;
    private String selectedSize;
    private SessionManager sessionManager;
    private String amountText; // Store calculated amount
    private boolean isFromCart;
    private CartManager cartManager;

    // Bank account information
    private static final String BANK_NAME = "Vietcombank";
    private static final String ACCOUNT_NAME = "CÔNG TY TNHH SNEAKER UNIVERSE";
    private static final String ACCOUNT_NUMBER = "1234567890";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bank_payment);

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
        displayBankInfo();
        generateQRCode();
        setupBottomNavigation();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgQRCode = findViewById(R.id.imgQRCode);
        txtBankName = findViewById(R.id.txtBankName);
        txtAccountName = findViewById(R.id.txtAccountName);
        txtAccountNumber = findViewById(R.id.txtAccountNumber);
        txtAmount = findViewById(R.id.txtAmount);
        btnCopyAccountNumber = findViewById(R.id.btnCopyAccountNumber);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());

        btnCopyAccountNumber.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account Number", ACCOUNT_NUMBER);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
        });

        btnConfirmPayment.setOnClickListener(v -> {
            processPayment("bank_transfer");
        });
    }

    private void processPayment(String paymentType) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("Đang xử lý...");
        Toast.makeText(this, "Đang xử lý thanh toán...", Toast.LENGTH_SHORT).show();

        if (isFromCart) {
            processOrderFromCart(paymentType);
        } else {
            processPaymentBuyNow(paymentType);
        }
    }

    private void processOrderFromCart(String paymentType) {
        List<CartItem> selectedItems = cartManager.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            btnConfirmPayment.setEnabled(true);
            btnConfirmPayment.setText("Xác nhận đã thanh toán");
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
        request.setGhiChu("Thanh toán chuyển khoản ngân hàng. STK: " + ACCOUNT_NUMBER);

        ApiService apiService = ApiClient.getApiService();
        apiService.createOrder(request).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                btnConfirmPayment.setEnabled(true);
                btnConfirmPayment.setText("Xác nhận đã thanh toán");

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<OrderResponse> body = response.body();
                    android.util.Log.d("BankPaymentActivity", "Response success: " + body.getSuccess());
                    android.util.Log.d("BankPaymentActivity", "Response message: " + body.getMessage());
                    
                    if (body.getSuccess()) {
                        OrderResponse orderResponse = body.getData();
                        if (orderResponse != null) {
                            android.util.Log.d("BankPaymentActivity", "✅ Order created successfully!");
                            android.util.Log.d("BankPaymentActivity", "Order ID: " + orderResponse.getId());
                            android.util.Log.d("BankPaymentActivity", "Order Total: " + orderResponse.getTongTien());
                            
                            // Log order response JSON
                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                String orderJson = gson.toJson(orderResponse);
                                android.util.Log.d("BankPaymentActivity", "Order Response JSON: " + orderJson);
                            } catch (Exception e) {
                                android.util.Log.e("BankPaymentActivity", "Error serializing order response", e);
                            }
                            
                            Toast.makeText(BankPaymentActivity.this, "Đặt hàng thành công! ID: " + orderResponse.getId(), Toast.LENGTH_LONG).show();
                        } else {
                            android.util.Log.w("BankPaymentActivity", "⚠️ Order created but response data is null");
                            Toast.makeText(BankPaymentActivity.this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                        }
                        
                        cartManager.removeSelectedItems();
                        Intent intent = new Intent(BankPaymentActivity.this, OrderActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("shouldReload", true);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = body.getMessage() != null ? body.getMessage() : "Không thể tạo đơn hàng";
                        android.util.Log.e("BankPaymentActivity", "❌ Order creation failed: " + errorMsg);
                        Toast.makeText(BankPaymentActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    android.util.Log.e("BankPaymentActivity", "❌ Response not successful. Code: " + response.code());
                    Toast.makeText(BankPaymentActivity.this, "Lỗi khi xử lý thanh toán (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                btnConfirmPayment.setEnabled(true);
                btnConfirmPayment.setText("Xác nhận đã thanh toán");
                Toast.makeText(BankPaymentActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processPaymentBuyNow(String paymentType) {
        String userId = sessionManager.getUserId();
        String email = sessionManager.getEmail();

        String productName = product != null ? product.name : "";
        String productPrice = amountText != null ? amountText : (product != null ? product.priceNew : "");

        PaymentRequest paymentRequest = new PaymentRequest(
            userId,
            email,
            "Chuyển khoản ngân hàng",
            ACCOUNT_NUMBER,
            paymentType,
            productName,
            productPrice,
            quantity,
            selectedSize != null ? selectedSize : "",
            ""
        );

        ApiService apiService = ApiClient.getApiService();
        Call<BaseResponse<Object>> call = apiService.createPayment(paymentRequest);
        call.enqueue(new Callback<BaseResponse<Object>>() {
            @Override
            public void onResponse(Call<BaseResponse<Object>> call, Response<BaseResponse<Object>> response) {
                btnConfirmPayment.setEnabled(true);
                btnConfirmPayment.setText("Xác nhận đã thanh toán");

                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Object> baseResponse = response.body();
                    if (baseResponse.getSuccess()) {
                        Toast.makeText(BankPaymentActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMsg = baseResponse.getMessage() != null ? baseResponse.getMessage() : "Thanh toán thất bại";
                        Toast.makeText(BankPaymentActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(BankPaymentActivity.this, "Lỗi khi xử lý thanh toán (Code: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Object>> call, Throwable t) {
                btnConfirmPayment.setEnabled(true);
                btnConfirmPayment.setText("Xác nhận đã thanh toán");
                Toast.makeText(BankPaymentActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayBankInfo() {
        txtBankName.setText(BANK_NAME);
        txtAccountName.setText(ACCOUNT_NAME);
        txtAccountNumber.setText(ACCOUNT_NUMBER);

        // Calculate and display amount
        if (isFromCart) {
            long totalPrice = cartManager.getTotalPrice();
            amountText = formatPrice(totalPrice);
            txtAmount.setText(amountText);
        } else {
            if (product != null && product.priceNew != null) {
                String priceStr = product.priceNew.replace("₫", "").replace(".", "").replace(",", "").trim();
                try {
                    long price = Long.parseLong(priceStr);
                    long totalAmount = price * quantity;
                    amountText = formatPrice(totalAmount);
                    txtAmount.setText(amountText);
                } catch (NumberFormatException e) {
                    amountText = product.priceNew;
                    txtAmount.setText(amountText);
                }
            } else {
                amountText = "0₫";
                txtAmount.setText(amountText);
            }
        }
    }

    private String formatPrice(long price) {
        if (price <= 0) {
            return "0₫";
        }
        return String.format("%,d₫", price).replace(",", ".");
    }

    private String formatCurrency(int amount) {
        return String.format("%,d₫", amount).replace(",", ".");
    }

    private void generateQRCode() {
        try {
            // Ensure amount is calculated
            if (amountText == null || amountText.isEmpty()) {
                amountText = txtAmount.getText() != null ? txtAmount.getText().toString() : "0₫";
            }

            // Create QR code content with bank transfer information
            String qrContent = String.format(
                "bank://transfer?bank=%s&account=%s&name=%s&amount=%s",
                BANK_NAME,
                ACCOUNT_NUMBER,
                ACCOUNT_NAME,
                amountText
            );

            // Generate QR code using QRCodeWriter
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            imgQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể tạo mã QR", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể tạo mã QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        // Trang chủ
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(BankPaymentActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Danh mục
        View navCategories = findViewById(R.id.navCategories);
        if (navCategories != null) {
            navCategories.setOnClickListener(v -> {
                Intent intent = new Intent(BankPaymentActivity.this, CategoriesActivity.class);
                startActivity(intent);
            });
        }

        // Giỏ hàng
        View navCart = findViewById(R.id.navCart);
        if (navCart != null) {
            navCart.setOnClickListener(v -> {
                Intent intent = new Intent(BankPaymentActivity.this, CartActivity.class);
                startActivity(intent);
            });
        }

        // Trợ giúp
        View navHelp = findViewById(R.id.navHelp);
        if (navHelp != null) {
            navHelp.setOnClickListener(v -> {
                Intent intent = new Intent(BankPaymentActivity.this, HelpActivity.class);
                startActivity(intent);
            });
        }

        // Tài khoản
        View navAccount = findViewById(R.id.navAccount);
        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(BankPaymentActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(BankPaymentActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}

