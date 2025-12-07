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

import com.poly.ban_giay_app.models.Product;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class BankPaymentActivity extends AppCompatActivity {
    private LinearLayout btnBack;
    private ImageView imgQRCode;
    private TextView txtBankName, txtAccountName, txtAccountNumber, txtAmount;
    private Button btnCopyAccountNumber;
    private Product product;
    private int quantity;
    private String selectedSize;
    private SessionManager sessionManager;
    private String amountText; // Store calculated amount

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
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());

        btnCopyAccountNumber.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account Number", ACCOUNT_NUMBER);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
        });
    }

    private void displayBankInfo() {
        txtBankName.setText(BANK_NAME);
        txtAccountName.setText(ACCOUNT_NAME);
        txtAccountNumber.setText(ACCOUNT_NUMBER);

        // Calculate and display amount
        if (product != null && product.priceNew != null) {
            // Remove currency symbols and format
            String priceStr = product.priceNew.replace("₫", "").replace(".", "").replace(",", "").trim();
            try {
                int price = Integer.parseInt(priceStr);
                int totalAmount = price * quantity;
                amountText = formatCurrency(totalAmount);
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

