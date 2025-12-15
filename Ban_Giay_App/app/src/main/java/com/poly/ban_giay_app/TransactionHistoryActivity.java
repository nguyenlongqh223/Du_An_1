package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.poly.ban_giay_app.adapter.TransactionHistoryAdapter;
import com.poly.ban_giay_app.models.TransactionHistory;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {
    private ImageView btnBack;
    private RecyclerView rvTransactions;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private TransactionHistoryAdapter adapter;
    private TransactionHistoryManager transactionManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaction_history);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        transactionManager = TransactionHistoryManager.getInstance(this);

        initViews();
        bindActions();
        loadTransactions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvTransactions = findViewById(R.id.rvTransactions);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        adapter = new TransactionHistoryAdapter();
        adapter.setOnTransactionClickListener(transaction -> {
            if (transaction.orderId != null && !transaction.orderId.isEmpty()) {
                Intent intent = new Intent(TransactionHistoryActivity.this, OrderDetailActivity.class);
                intent.putExtra("order_id", transaction.orderId);
                startActivity(intent);
            }
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void bindActions() {
        btnBack.setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(() -> {
            loadTransactions();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void loadTransactions() {
        List<TransactionHistory> transactions = transactionManager.getAllTransactions();
        
        if (transactions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            adapter.setTransactions(transactions);
        }
    }
}
