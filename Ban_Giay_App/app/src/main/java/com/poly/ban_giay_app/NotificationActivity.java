package com.poly.ban_giay_app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private ImageView btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        sessionManager = new SessionManager(this);
        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        btnBack = findViewById(R.id.btnBack);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);

        btnBack.setOnClickListener(v -> finish());

        adapter = new NotificationAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvNotifications.setLayoutManager(layoutManager);
        rvNotifications.setAdapter(adapter);
        rvNotifications.setHasFixedSize(false); // Đổi thành false để RecyclerView có thể tính toán size động
        rvNotifications.setNestedScrollingEnabled(true);
        
        Log.d(TAG, "RecyclerView setup complete:");
        Log.d(TAG, "  - LayoutManager: " + layoutManager);
        Log.d(TAG, "  - Adapter: " + adapter);
        Log.d(TAG, "  - Initial visibility: " + (rvNotifications.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d(TAG, "  - HasFixedSize: " + rvNotifications.hasFixedSize());
        Log.d(TAG, "  - NestedScrollingEnabled: " + rvNotifications.isNestedScrollingEnabled());

        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        String userId = sessionManager.getUserId();
        Log.d(TAG, "=== loadNotifications START ===");
        Log.d(TAG, "userId: " + userId);
        
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "userId is null or empty");
            showEmpty(true);
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        
        // Thử endpoint đầu tiên: /notification/{user_id}
        // Server trả về data là array trực tiếp
        Log.d(TAG, "Trying endpoint: /notification/" + userId);
        apiService.getNotificationsByUser(userId).enqueue(new Callback<BaseResponse<List<NotificationResponse>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<NotificationResponse>>> call, Response<BaseResponse<List<NotificationResponse>>> response) {
                Log.d(TAG, "=== RESPONSE RECEIVED ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response isSuccessful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<List<NotificationResponse>> body = response.body();
                    Log.d(TAG, "Response body success: " + body.getSuccess());
                    Log.d(TAG, "Response body message: " + body.getMessage());
                    
                    // Lấy danh sách từ data (BaseResponse.getData() sẽ ưu tiên data, sau đó notifications)
                    List<NotificationResponse> notifications = body.getData();
                    Log.d(TAG, "Notifications from getData(): " + (notifications != null ? notifications.size() : "null"));
                    
                    // Nếu vẫn null hoặc empty, thử lấy trực tiếp từ notifications field
                    if (notifications == null || notifications.isEmpty()) {
                        List<NotificationResponse> notificationsFromField = body.getNotifications();
                        if (notificationsFromField != null && !notificationsFromField.isEmpty()) {
                            notifications = notificationsFromField;
                            Log.d(TAG, "Found notifications from getNotifications(): " + notifications.size());
                        }
                    }
                    
                    final List<NotificationResponse> finalNotifications = notifications != null ? notifications : new ArrayList<>();
                    
                    // Log chi tiết từng notification
                    if (finalNotifications.size() > 0) {
                        Log.d(TAG, "=== NOTIFICATIONS DETAILS ===");
                        for (int i = 0; i < finalNotifications.size(); i++) {
                            NotificationResponse notif = finalNotifications.get(i);
                            Log.d(TAG, "Notification " + i + ":");
                            Log.d(TAG, "  - ID: " + notif.getId());
                            Log.d(TAG, "  - Title: " + notif.getTitle());
                            Log.d(TAG, "  - Message: " + notif.getMessage());
                            Log.d(TAG, "  - isRead: " + notif.isRead());
                            Log.d(TAG, "  - createdAt: " + notif.getCreatedAt());
                            Log.d(TAG, "  - Product Name: " + notif.getTenSanPham());
                            Log.d(TAG, "  - Cancellation Reason: " + notif.getLyDoHuy());
                            Log.d(TAG, "  - Metadata: " + notif.getMetadata());
                        }
                    }
                    
                    // Cập nhật UI trên main thread
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.d(TAG, "Setting adapter with " + finalNotifications.size() + " notifications");
                        
                        // Đảm bảo adapter được set lại
                        adapter.setItems(finalNotifications);
                        Log.d(TAG, "Adapter item count after setItems: " + adapter.getItemCount());
                        
                        // Hiển thị/ẩn RecyclerView và empty view
                        showEmpty(finalNotifications.isEmpty());
                        Log.d(TAG, "showEmpty called with: " + finalNotifications.isEmpty());
                        Log.d(TAG, "RecyclerView visibility: " + (rvNotifications.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                        Log.d(TAG, "Empty view visibility: " + (tvEmpty.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                        
                        // Force refresh RecyclerView
                        if (!finalNotifications.isEmpty()) {
                            rvNotifications.setVisibility(View.VISIBLE);
                            tvEmpty.setVisibility(View.GONE);
                            // Đảm bảo adapter được notify
                            adapter.notifyDataSetChanged();
                            // Force layout
                            rvNotifications.invalidate();
                            rvNotifications.requestLayout();
                            // Scroll to top
                            rvNotifications.scrollToPosition(0);
                            Log.d(TAG, "✅ Force refreshed RecyclerView with " + finalNotifications.size() + " notifications");
                        }
                    });
                } else {
                    Log.w(TAG, "Response not successful or body is null. Code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    tryAlternativeEndpoint(userId);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<NotificationResponse>>> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage(), t);
                tryAlternativeEndpoint(userId);
            }
        });
    }
    
    private void tryAlternativeEndpoint(String userId) {
        Log.d(TAG, "Trying alternative endpoint: /notification?user_id=" + userId);
        apiService.getNotifications(userId, null).enqueue(new Callback<BaseResponse<NotificationListResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<NotificationListResponse>> call, Response<BaseResponse<NotificationListResponse>> response) {
                Log.d(TAG, "Alternative endpoint response code: " + response.code());
                swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<NotificationListResponse> body = response.body();
                    Log.d(TAG, "Alternative endpoint body success: " + body.getSuccess());
                    
                    NotificationListResponse notificationListResponse = body.getData();
                    List<NotificationResponse> notifications = null;
                    
                    if (notificationListResponse != null) {
                        notifications = notificationListResponse.getNotifications();
                        Log.d(TAG, "Alternative endpoint notifications count: " + (notifications != null ? notifications.size() : "null"));
                    }
                    
                    // Thử parse trực tiếp
                    if (notifications == null || notifications.isEmpty()) {
                        try {
                            Object dataObj = body.getData();
                            if (dataObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<NotificationResponse> directList = (List<NotificationResponse>) dataObj;
                                notifications = directList;
                                Log.d(TAG, "Alternative endpoint direct list count: " + (notifications != null ? notifications.size() : "null"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing alternative endpoint: " + e.getMessage(), e);
                        }
                    }
                    
                    final List<NotificationResponse> finalNotifications = notifications != null ? notifications : new ArrayList<>();
                    
                    runOnUiThread(() -> {
                        adapter.setItems(finalNotifications);
                        showEmpty(finalNotifications.isEmpty());
                        rvNotifications.invalidate();
                        rvNotifications.requestLayout();
                        if (finalNotifications.isEmpty()) {
                            Log.w(TAG, "No notifications found from any endpoint");
                            Toast.makeText(NotificationActivity.this, "Không có thông báo", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "✅ Successfully loaded " + finalNotifications.size() + " notifications from alternative endpoint");
                        }
                    });
                } else {
                    Log.e(TAG, "Alternative endpoint failed. Response code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    runOnUiThread(() -> {
                        showEmpty(true);
                        Toast.makeText(NotificationActivity.this, "Không thể tải thông báo", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<NotificationListResponse>> call, Throwable t) {
                Log.e(TAG, "Alternative endpoint onFailure: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showEmpty(true);
                    Toast.makeText(NotificationActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showEmpty(boolean empty) {
        Log.d(TAG, "showEmpty: " + empty);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
