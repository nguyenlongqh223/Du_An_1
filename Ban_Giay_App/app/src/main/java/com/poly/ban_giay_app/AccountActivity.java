package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AccountActivity extends AppCompatActivity {
    private View btnBack;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private LinearLayout layoutLogout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        initViews();
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        populateUserInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        layoutLogout = findViewById(R.id.layoutLogout);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        layoutLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });
    }

    private void populateUserInfo() {
        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getEmail());
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
