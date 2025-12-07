package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.poly.ban_giay_app.models.Product;

public class PaymentMethodActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView txtProductInfo;
    private LinearLayout paymentCreditCard, paymentATM, paymentCOD;
    private Product product;
    private int quantity;
    private String selectedSize;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_method);

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
        paymentCreditCard = findViewById(R.id.paymentCreditCard);
        paymentATM = findViewById(R.id.paymentATM);
        paymentCOD = findViewById(R.id.paymentCOD);
    }

    private void displayProductInfo() {
        if (product != null && txtProductInfo != null) {
            // Format product name and price
            String productName = product.name;
            
            // Tính giá tổng dựa trên số lượng
            String totalPrice = calculateTotalPrice();
            
            // Build info string: "Product Name • Price • Size"
            StringBuilder info = new StringBuilder();
            info.append(productName);
            
            if (totalPrice != null && !totalPrice.isEmpty()) {
                info.append(" • ").append(totalPrice);
            }
            
            if (selectedSize != null && !selectedSize.isEmpty()) {
                info.append(" • Size ").append(selectedSize);
            }
            
            txtProductInfo.setText(info.toString());
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
        // Loại bỏ ký tự ₫, dấu chấm, dấu phẩy, khoảng trắng
        String cleaned = priceString.replace("₫", "")
                                    .replace(".", "")
                                    .replace(",", "")
                                    .replace(" ", "")
                                    .trim();
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Format giá với ký tự ₫ (ví dụ: 500000 -> "500.000₫")
     */
    private String formatPrice(long price) {
        // Format với dấu chấm ngăn cách hàng nghìn
        String formatted = String.format("%,d", price).replace(",", ".");
        return formatted + "₫";
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());

        // Credit Card payment
        paymentCreditCard.setOnClickListener(v -> {
            // Navigate to credit card input screen
            Intent intent = new Intent(PaymentMethodActivity.this, CreditCardActivity.class);
            intent.putExtra("product", product);
            intent.putExtra("quantity", quantity);
            intent.putExtra("selectedSize", selectedSize);
            startActivity(intent);
        });

        // ATM Card payment
        paymentATM.setOnClickListener(v -> {
            // Navigate to ATM card input screen
            Intent intent = new Intent(PaymentMethodActivity.this, AtmCardActivity.class);
            intent.putExtra("product", product);
            intent.putExtra("quantity", quantity);
            intent.putExtra("selectedSize", selectedSize);
            startActivity(intent);
        });

        // Cash on Delivery payment - Navigate to bank payment
        paymentCOD.setOnClickListener(v -> {
            // Navigate to bank payment screen
            Intent intent = new Intent(PaymentMethodActivity.this, BankPaymentActivity.class);
            intent.putExtra("product", product);
            intent.putExtra("quantity", quantity);
            intent.putExtra("selectedSize", selectedSize);
            startActivity(intent);
        });
    }

    private void selectPaymentMethod(String method, LinearLayout layout) {
        // Reset all payment methods
        resetPaymentMethod(paymentCreditCard);
        resetPaymentMethod(paymentATM);
        resetPaymentMethod(paymentCOD);

        // Highlight selected payment method
        layout.setBackgroundResource(R.drawable.bg_payment_selected);
        
        // Show confirmation or proceed to next step
        String methodName = "";
        switch (method) {
            case "credit_card":
                methodName = "Thẻ tín dụng";
                break;
            case "atm_card":
                methodName = "Thẻ ATM";
                break;
            case "cod":
                methodName = "Thanh toán khi nhận hàng";
                break;
        }
        
        Toast.makeText(this, "Đã chọn: " + methodName, Toast.LENGTH_SHORT).show();
        
        // TODO: Navigate to checkout/order confirmation screen
        // Intent intent = new Intent(this, CheckoutActivity.class);
        // intent.putExtra("product", product);
        // intent.putExtra("quantity", quantity);
        // intent.putExtra("selectedSize", selectedSize);
        // intent.putExtra("paymentMethod", method);
        // startActivity(intent);
    }

    private void resetPaymentMethod(LinearLayout layout) {
        layout.setBackgroundResource(R.drawable.bg_payment_method);
    }

    private void setupBottomNavigation() {
        // Trang chủ
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(PaymentMethodActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Danh mục
        View navCategories = findViewById(R.id.navCategories);
        if (navCategories != null) {
            navCategories.setOnClickListener(v -> {
                Intent intent = new Intent(PaymentMethodActivity.this, CategoriesActivity.class);
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
                    Intent intent = new Intent(PaymentMethodActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(PaymentMethodActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}

