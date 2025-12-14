package com.poly.ban_giay_app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poly.ban_giay_app.adapter.OrderItemAdapter;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.OrderResponse;
import com.poly.ban_giay_app.network.request.CancelOrderRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {
    private static final String TAG = "OrderDetailActivity";
    
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private CardView cardOrderInfo, cardProducts, cardDelivery;
    private TextView txtOrderId, txtOrderStatus, txtOrderDate, txtOrderTotal;
    private TextView txtCustomerName, txtDeliveryAddress, txtDeliveryPhone, txtNote;
    private RecyclerView rvOrderItems;
    private Button btnCancelOrder;
    
    private ApiService apiService;
    private String orderId;
    private OrderItemAdapter orderItemAdapter;
    private OrderResponse currentOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Get order ID from intent
        orderId = getIntent().getStringExtra("order_id");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadOrderDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        cardOrderInfo = findViewById(R.id.cardOrderInfo);
        cardProducts = findViewById(R.id.cardProducts);
        cardDelivery = findViewById(R.id.cardDelivery);
        
        txtOrderId = findViewById(R.id.txtOrderId);
        txtOrderStatus = findViewById(R.id.txtOrderStatus);
        txtOrderDate = findViewById(R.id.txtOrderDate);
        txtOrderTotal = findViewById(R.id.txtOrderTotal);
        
        txtCustomerName = findViewById(R.id.txtCustomerName);
        txtDeliveryAddress = findViewById(R.id.txtDeliveryAddress);
        txtDeliveryPhone = findViewById(R.id.txtDeliveryPhone);
        txtNote = findViewById(R.id.txtNote);
        
        rvOrderItems = findViewById(R.id.rvOrderItems);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        
        btnCancelOrder.setOnClickListener(v -> {
            if (currentOrder != null) {
                showCancelOrderDialog(currentOrder);
            } else {
                Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        orderItemAdapter = new OrderItemAdapter();
        rvOrderItems.setAdapter(orderItemAdapter);
    }

    private void loadOrderDetails() {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            showEmpty();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        hideAllCards();

        Log.d(TAG, "Loading order details for ID: " + orderId);
        
        apiService.getOrderById(orderId).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<OrderResponse> body = response.body();
                    if (body.getSuccess() && body.getData() != null) {
                        OrderResponse order = body.getData();
                        displayOrderDetails(order);
                    } else {
                        Log.e(TAG, "Failed to load order: " + body.getMessage());
                        Toast.makeText(OrderDetailActivity.this, 
                            body.getMessage() != null ? body.getMessage() : "Không thể tải thông tin đơn hàng",
                            Toast.LENGTH_SHORT).show();
                        showEmpty();
                    }
                } else {
                    Log.e(TAG, "Response not successful. Code: " + response.code());
                    String errorMsg = NetworkUtils.extractErrorMessage(response);
                    Toast.makeText(OrderDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading order details", t);
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private void displayOrderDetails(OrderResponse order) {
        // Save current order for cancel action
        currentOrder = order;
        // Order ID
        String orderIdShort = order.getId();
        if (orderIdShort != null && orderIdShort.length() > 8) {
            orderIdShort = orderIdShort.substring(orderIdShort.length() - 8).toUpperCase();
        }
        txtOrderId.setText("Đơn hàng #" + (orderIdShort != null ? orderIdShort : "N/A"));

        // Status
        String trangThai = order.getTrangThai();
        String trangThaiDisplay = order.getTrangThaiDisplay();
        txtOrderStatus.setText(trangThaiDisplay);
        
        // Set status color
        int statusColor;
        if (trangThai != null) {
            switch (trangThai) {
                case "pending":
                    statusColor = getResources().getColor(android.R.color.holo_orange_dark, null);
                    break;
                case "confirmed":
                    statusColor = getResources().getColor(android.R.color.holo_blue_dark, null);
                    break;
                case "shipping":
                    statusColor = getResources().getColor(android.R.color.holo_blue_light, null);
                    break;
                case "delivered":
                    statusColor = getResources().getColor(android.R.color.holo_green_dark, null);
                    break;
                case "cancelled":
                    statusColor = getResources().getColor(android.R.color.holo_red_dark, null);
                    break;
                default:
                    statusColor = getResources().getColor(android.R.color.darker_gray, null);
            }
        } else {
            statusColor = getResources().getColor(android.R.color.darker_gray, null);
        }
        txtOrderStatus.setTextColor(statusColor);

        // Date
        String dateStr = formatDate(order.getCreatedAt());
        txtOrderDate.setText("Ngày đặt: " + dateStr);

        // Total
        Integer tongTien = order.getTongTien();
        if (tongTien != null) {
            txtOrderTotal.setText(formatPrice(tongTien));
        } else {
            txtOrderTotal.setText("0₫");
        }

        // Products
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            orderItemAdapter.setItems(order.getItems());
            cardProducts.setVisibility(View.VISIBLE);
        } else {
            cardProducts.setVisibility(View.GONE);
        }

        // Delivery info
        String tenKhachHang = order.getTenKhachHang();
        String diaChi = order.getDiaChiGiaoHang();
        String soDienThoai = order.getSoDienThoai();
        String ghiChu = order.getGhiChu();

        // Customer name - ưu tiên từ order, nếu không có thì lấy từ SessionManager
        if (tenKhachHang != null && !tenKhachHang.isEmpty()) {
            txtCustomerName.setText("Tên khách hàng: " + tenKhachHang);
        } else {
            // Thử lấy từ SessionManager nếu đơn hàng là của user hiện tại
            SessionManager sessionManager = new SessionManager(this);
            if (sessionManager.isLoggedIn()) {
                String currentUserId = sessionManager.getUserId();
                String orderUserId = order.getUserId();
                if (currentUserId != null && orderUserId != null && currentUserId.equals(orderUserId)) {
                    String userName = sessionManager.getUserName();
                    if (userName != null && !userName.isEmpty()) {
                        txtCustomerName.setText("Tên khách hàng: " + userName);
                    } else {
                        txtCustomerName.setText("Tên khách hàng: Chưa cập nhật");
                    }
                } else {
                    txtCustomerName.setText("Tên khách hàng: Chưa cập nhật");
                }
            } else {
                txtCustomerName.setText("Tên khách hàng: Chưa cập nhật");
            }
        }

        if (diaChi != null && !diaChi.isEmpty()) {
            txtDeliveryAddress.setText("Địa chỉ: " + diaChi);
        } else {
            txtDeliveryAddress.setText("Địa chỉ: Chưa cập nhật");
        }

        if (soDienThoai != null && !soDienThoai.isEmpty()) {
            txtDeliveryPhone.setText("Số điện thoại: " + soDienThoai);
        } else {
            txtDeliveryPhone.setText("Số điện thoại: Chưa cập nhật");
        }

        if (ghiChu != null && !ghiChu.isEmpty()) {
            txtNote.setText("Ghi chú: " + ghiChu);
            txtNote.setVisibility(View.VISIBLE);
        } else {
            txtNote.setVisibility(View.GONE);
        }

        // Show cancel button only for pending or confirmed orders
        if (trangThai != null && (trangThai.equals("pending") || trangThai.equals("confirmed"))) {
            btnCancelOrder.setVisibility(View.VISIBLE);
        } else {
            btnCancelOrder.setVisibility(View.GONE);
        }

        // Show all cards
        cardOrderInfo.setVisibility(View.VISIBLE);
        cardDelivery.setVisibility(View.VISIBLE);
    }

    private void hideAllCards() {
        cardOrderInfo.setVisibility(View.GONE);
        cardProducts.setVisibility(View.GONE);
        cardDelivery.setVisibility(View.GONE);
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        hideAllCards();
    }

    private String formatDate(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return "N/A";
        }
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = iso.parse(isoString);
            if (date == null) {
                // Try without milliseconds
                iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                date = iso.parse(isoString);
            }
            if (date != null) {
                SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return out.format(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + isoString, e);
        }
        return isoString;
    }

    private String formatPrice(long price) {
        return String.format("%,d₫", price).replace(",", ".");
    }

    private void showCancelOrderDialog(OrderResponse order) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_order, null);

        RadioGroup radioGroupReasons = dialogView.findViewById(R.id.radioGroupReasons);
        Button btnConfirmCancel = dialogView.findViewById(R.id.btnConfirmCancel);
        TextView btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        radioGroupReasons.setOnCheckedChangeListener((group, checkedId) -> {
            btnConfirmCancel.setEnabled(checkedId != -1);
            btnConfirmCancel.setBackgroundTintList(ContextCompat.getColorStateList(this, checkedId != -1 ? R.color.red : R.color.grey));
        });

        btnConfirmCancel.setOnClickListener(v -> {
            int selectedId = radioGroupReasons.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn lý do hủy đơn hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadio = dialogView.findViewById(selectedId);
            String lyDo = selectedRadio.getText().toString();

            Log.d(TAG, "Lý do hủy đơn hàng: " + lyDo);

            dialog.dismiss();
            cancelOrder(order, lyDo);
        });

        dialog.show();
    }

    private void cancelOrder(OrderResponse order, String lyDo) {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        CancelOrderRequest cancelRequest = new CancelOrderRequest(lyDo);

        Log.d(TAG, "Gửi yêu cầu hủy đơn hàng với lý do: " + lyDo);

        apiService.cancelOrder(order.getId(), cancelRequest).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<OrderResponse> body = response.body();
                    if (body.getSuccess()) {
                        Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        // Reload order details to update status
                        loadOrderDetails();
                    } else {
                        Toast.makeText(OrderDetailActivity.this,
                            body.getMessage() != null ? body.getMessage() : "Không thể hủy đơn hàng",
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(OrderDetailActivity.this, NetworkUtils.extractErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
