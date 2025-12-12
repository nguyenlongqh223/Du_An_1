package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poly.ban_giay_app.adapter.OrderAdapter;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.OrderResponse;
import com.poly.ban_giay_app.network.request.UpdateOrderStatusRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderActivity extends AppCompatActivity {
    private RecyclerView rvOrders;
    private OrderAdapter orderAdapter;
    private List<OrderResponse> allOrders;
    private List<OrderResponse> filteredOrders;
    private Button btnTabAll, btnTabPending, btnTabShipping, btnTabDelivered, btnTabCancelled;
    private LinearLayout layoutEmptyOrder;
    private Button btnShopNow;
    private ImageView btnBack;
    private View navAccount, navHome, navCart;
    private ImageView imgAccountIcon;
    private TextView tvAccountLabel;
    private SessionManager sessionManager;
    private ApiService apiService;
    private String currentFilter = "all"; // "all", "pending", "shipping", "delivered", "cancelled"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order);

        sessionManager = new SessionManager(this);
        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        allOrders = new ArrayList<>();
        filteredOrders = new ArrayList<>();

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initAccountNav();
        updateAccountNavUi();
        setupRecyclerView();
        setupNavigation();
        setupTabs();
        updateTabButtons(); // Highlight tab "Tất cả" ngay từ đầu
        
        // Kiểm tra xem có cần reload ngay không (từ CartActivity sau khi thanh toán)
        boolean shouldReload = getIntent().getBooleanExtra("shouldReload", false);
        if (shouldReload) {
            // Delay nhỏ để đảm bảo UI đã render xong, sau đó reload
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Log.d("OrderActivity", "Reloading orders after order creation");
                loadOrders();
            }, 300);
        } else {
            loadOrders();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
        updateAccountNavUi();
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rvOrders);
        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabPending = findViewById(R.id.btnTabPending);
        btnTabShipping = findViewById(R.id.btnTabShipping);
        btnTabDelivered = findViewById(R.id.btnTabDelivered);
        btnTabCancelled = findViewById(R.id.btnTabCancelled);
        layoutEmptyOrder = findViewById(R.id.layoutEmptyOrder);
        btnShopNow = findViewById(R.id.btnShopNow);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupNavigation() {
        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Home navigation
        navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(OrderActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        // Cart navigation
        navCart = findViewById(R.id.navCart);
        if (navCart != null) {
            navCart.setOnClickListener(v -> {
                Intent intent = new Intent(OrderActivity.this, CartActivity.class);
                startActivity(intent);
            });
        }

        // Shop now button
        if (btnShopNow != null) {
            btnShopNow.setOnClickListener(v -> {
                Intent intent = new Intent(OrderActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }
    }

    private void initAccountNav() {
        navAccount = findViewById(R.id.navAccount);
        imgAccountIcon = findViewById(R.id.imgAccountIcon);
        tvAccountLabel = findViewById(R.id.tvAccountLabel);

        if (navAccount != null) {
            navAccount.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(OrderActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(OrderActivity.this, LoginActivity.class);
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

    private void setupRecyclerView() {
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(filteredOrders, new OrderAdapter.OnOrderListener() {
            @Override
            public void onViewDetail(OrderResponse order) {
                Intent intent = new Intent(OrderActivity.this, OrderDetailActivity.class);
                intent.putExtra("order_id", order.getId());
                startActivity(intent);
            }

            @Override
            public void onCancelOrder(OrderResponse order) {
                cancelOrder(order);
            }
        }, this);
        rvOrders.setAdapter(orderAdapter);
    }

    private void setupTabs() {
        btnTabAll.setOnClickListener(v -> filterOrders("all"));
        btnTabPending.setOnClickListener(v -> filterOrders("pending"));
        btnTabShipping.setOnClickListener(v -> filterOrders("shipping"));
        btnTabDelivered.setOnClickListener(v -> filterOrders("delivered"));
        btnTabCancelled.setOnClickListener(v -> filterOrders("cancelled"));
    }

    private void filterOrders(String filter) {
        currentFilter = filter;
        filteredOrders.clear();

        if ("all".equals(filter)) {
            filteredOrders.addAll(allOrders);
        } else {
            for (OrderResponse order : allOrders) {
                if (filter.equals(order.getTrangThai())) {
                    filteredOrders.add(order);
                }
            }
        }

        updateTabButtons();
        updateUI();
    }

    private void updateTabButtons() {
        // Reset all buttons
        resetTabButton(btnTabAll);
        resetTabButton(btnTabPending);
        resetTabButton(btnTabShipping);
        resetTabButton(btnTabDelivered);
        resetTabButton(btnTabCancelled);

        // Highlight selected button
        Button selectedButton = null;
        switch (currentFilter) {
            case "all":
                selectedButton = btnTabAll;
                break;
            case "pending":
                selectedButton = btnTabPending;
                break;
            case "shipping":
                selectedButton = btnTabShipping;
                break;
            case "delivered":
                selectedButton = btnTabDelivered;
                break;
            case "cancelled":
                selectedButton = btnTabCancelled;
                break;
        }

        if (selectedButton != null) {
            selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.teal_700));
            selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void resetTabButton(Button button) {
        if (button != null) {
            // Set background màu xám nhạt
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            // Set text color đậm để dễ nhìn
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private String formatPrice(long price) {
        return String.format("%,d₫", price).replace(",", ".");
    }

    private void loadOrders() {
        if (!sessionManager.isLoggedIn()) {
            updateUI();
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w("OrderActivity", "User ID is empty");
            updateUI();
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            updateUI();
            return;
        }

        Log.d("OrderActivity", "Loading orders for user: " + userId);

        apiService.getOrders(userId, null).enqueue(new Callback<BaseResponse<List<OrderResponse>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<OrderResponse>>> call, Response<BaseResponse<List<OrderResponse>>> response) {
                runOnUiThread(() -> {
                    try {
                        Log.d("OrderActivity", "=== API RESPONSE RECEIVED ===");
                        Log.d("OrderActivity", "Response code: " + response.code());
                        Log.d("OrderActivity", "Response isSuccessful: " + response.isSuccessful());
                        Log.d("OrderActivity", "Response body is null: " + (response.body() == null));
                        
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<List<OrderResponse>> body = response.body();
                            Log.d("OrderActivity", "Response success: " + body.getSuccess());
                            Log.d("OrderActivity", "Response data is null: " + (body.getData() == null));
                            
                            if (body.getSuccess() && body.getData() != null) {
                                allOrders.clear();
                                allOrders.addAll(body.getData());
                                Log.d("OrderActivity", "✅ Loaded " + allOrders.size() + " orders");
                                
                                // Log từng đơn hàng để debug
                                for (int i = 0; i < allOrders.size(); i++) {
                                    OrderResponse order = allOrders.get(i);
                                    Log.d("OrderActivity", "  Order " + i + ": ID=" + order.getId() + 
                                          ", Status=" + order.getTrangThai() + 
                                          ", Total=" + order.getTongTien() + 
                                          ", Items=" + (order.getItems() != null ? order.getItems().size() : 0));
                                }
                                
                                filterOrders(currentFilter);
                            } else {
                                Log.w("OrderActivity", "⚠️ No orders found or success=false");
                                if (body.getData() == null) {
                                    Log.w("OrderActivity", "Response data is null");
                                }
                                allOrders.clear();
                                filterOrders(currentFilter);
                            }
                        } else {
                            String errorMsg = NetworkUtils.extractErrorMessage(response);
                            Log.e("OrderActivity", "❌ Failed to load orders: " + errorMsg);
                            
                            // Log error body nếu có
                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    Log.e("OrderActivity", "Error body: " + errorBody);
                                } catch (Exception e) {
                                    Log.e("OrderActivity", "Error reading error body", e);
                                }
                            }
                            
                            allOrders.clear();
                            filterOrders(currentFilter);
                        }
                    } catch (Exception e) {
                        Log.e("OrderActivity", "❌ Exception in onResponse", e);
                        allOrders.clear();
                        filterOrders(currentFilter);
                    }
                });
            }

            @Override
            public void onFailure(Call<BaseResponse<List<OrderResponse>>> call, Throwable t) {
                runOnUiThread(() -> {
                    Log.e("OrderActivity", "❌ Error loading orders: " + t.getMessage(), t);
                    allOrders.clear();
                    filterOrders(currentFilter);
                });
            }
        });
    }

    private void cancelOrder(OrderResponse order) {
        showCancelOrderDialog(order);
    }

    private void showCancelOrderDialog(OrderResponse order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_order, null);
        builder.setView(dialogView);

        RadioGroup radioGroupReasons = dialogView.findViewById(R.id.radioGroupReasons);
        TextView btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        // Đóng dialog khi click nút X
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Đóng dialog khi click "Không"
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Enable/disable nút xác nhận dựa trên việc chọn lý do
        radioGroupReasons.setOnCheckedChangeListener((group, checkedId) -> {
            btnConfirm.setEnabled(checkedId != -1);
            if (checkedId != -1) {
                btnConfirm.setAlpha(1.0f);
            } else {
                btnConfirm.setAlpha(0.5f);
            }
        });

        // Xác nhận hủy đơn
        btnConfirm.setOnClickListener(v -> {
            int selectedId = radioGroupReasons.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn lý do hủy đơn hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadio = dialogView.findViewById(selectedId);
            String lyDo = selectedRadio.getText().toString();
            dialog.dismiss();
            performCancelOrder(order, lyDo);
        });

        dialog.show();
    }

    private void performCancelOrder(OrderResponse order, String lyDo) {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        com.poly.ban_giay_app.network.request.CancelOrderRequest cancelRequest = 
            new com.poly.ban_giay_app.network.request.CancelOrderRequest(lyDo);
        
        apiService.cancelOrder(order.getId(), cancelRequest).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<OrderResponse> body = response.body();
                    if (body.getSuccess()) {
                        Toast.makeText(OrderActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        loadOrders(); // Reload orders
                    } else {
                        Toast.makeText(OrderActivity.this, body.getMessage() != null ? body.getMessage() : "Không thể hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OrderActivity.this, NetworkUtils.extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                Toast.makeText(OrderActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (filteredOrders.isEmpty()) {
            layoutEmptyOrder.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
        } else {
            layoutEmptyOrder.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
            orderAdapter.notifyDataSetChanged();
        }
    }
}

