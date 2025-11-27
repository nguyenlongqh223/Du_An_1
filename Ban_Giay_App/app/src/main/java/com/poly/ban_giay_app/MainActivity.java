package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private View navAccount;
    private ImageView imgAccountIcon;
    private TextView tvAccountLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sessionManager = new SessionManager(this);
        initAccountNav();
        updateAccountNavUi();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateAccountNavUi();
    }

    private void initAccountNav() {
        navAccount = findViewById(R.id.navAccount);
        imgAccountIcon = findViewById(R.id.imgAccountIcon);
        tvAccountLabel = findViewById(R.id.tvAccountLabel);

        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(MainActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
    private void updateAccountNavUi() {
        if (tvAccountLabel != null) {
            if (sessionManager.isLoggedIn()) {
                tvAccountLabel.setText(sessionManager.getUserName());
            } else {
                tvAccountLabel.setText(R.string.account);
            }
        }

        if (imgAccountIcon != null) {
            imgAccountIcon.setImageResource(R.drawable.ic_user);
            int color = ContextCompat.getColor(this, sessionManager.isLoggedIn()
                    ? android.R.color.holo_green_dark
                    : android.R.color.black);
            imgAccountIcon.setColorFilter(color);
        }
    }
}