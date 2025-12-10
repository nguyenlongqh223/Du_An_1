package com.poly.ban_giay_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.poly.ban_giay_app.adapter.NotificationAdapter;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.NotificationListResponse;
import com.poly.ban_giay_app.network.model.NotificationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        sessionManager = new SessionManager(this);
        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new NotificationAdapter();
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            showEmpty(true);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        apiService.getNotificationsByUser(userId).enqueue(new Callback<BaseResponse<NotificationListResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<NotificationListResponse>> call, Response<BaseResponse<NotificationListResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    NotificationListResponse notificationListResponse = response.body().getData();
                    if (notificationListResponse != null) {
                        List<NotificationResponse> notifications = notificationListResponse.getNotifications();
                        adapter.setItems(notifications);
                        showEmpty(notifications == null || notifications.isEmpty());
                    } else {
                        showEmpty(true);
                    }
                } else {
                    showEmpty(true);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<NotificationListResponse>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showEmpty(true);
            }
        });
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
