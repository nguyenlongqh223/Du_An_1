package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {
    private TextView txtOrderId, txtOrderStatus, txtOrderDate, txtOrderTotal;
    private TextView txtDeliveryAddress, txtPhoneNumber, txtNote;
    private RecyclerView rvOrderItems;
    private Button btnCancelOrder;
    private ImageView btnBack;
    private LinearLayout layoutOrderInfo, layoutProducts, layoutDelivery, layoutTotal;
    private ProgressBar progressBar;
    private TextView txtEmpty;
    
    private String orderId;
    private OrderResponse order;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        // Lấy order_id từ intent
        orderId = getIntent().getStringExtra("order_id");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        initViews();
        setupNavigation();
        loadOrderDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtOrderId = findViewById(R.id.txtOrderId);
        txtOrderStatus = findViewById(R.id.txtOrderStatus);
        txtOrderDate = findViewById(R.id.txtOrderDate);
        txtOrderTotal = findViewById(R.id.txtOrderTotal);
        txtDeliveryAddress = findViewById(R.id.txtDeliveryAddress);
        txtPhoneNumber = findViewById(R.id.txtPhoneNumber);
        txtNote = findViewById(R.id.txtNote);
        rvOrderItems = findViewById(R.id.rvOrderItems);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        progressBar = findViewById(R.id.progressBar);
        txtEmpty = findViewById(R.id.txtEmpty);
        
        layoutOrderInfo = findViewById(R.id.layoutOrderInfo);
        layoutProducts = findViewById(R.id.layoutProducts);
        layoutDelivery = findViewById(R.id.layoutDelivery);
        layoutTotal = findViewById(R.id.layoutTotal);

        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupNavigation() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnCancelOrder != null) {
            btnCancelOrder.setOnClickListener(v -> {
                if (order != null) {
                    showCancelOrderDialog();
                }
            });
        }
    }

    private void loadOrderDetails() {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            showEmpty(true);
            return;
        }

        showLoading(true);
        
        apiService.getOrderById(orderId).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    order = response.body().getData();
                    if (order != null) {
                        displayOrderDetails();
                    } else {
                        showEmpty(true);
                        Toast.makeText(OrderDetailActivity.this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showEmpty(true);
                    String errorMsg = NetworkUtils.extractErrorMessage(response);
                    Toast.makeText(OrderDetailActivity.this, errorMsg != null ? errorMsg : "Không thể tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                showLoading(false);
                showEmpty(true);
                Log.e("OrderDetailActivity", "Error loading order details", t);
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayOrderDetails() {
        if (order == null) return;

        showEmpty(false);

        // Mã đơn hàng
        String orderIdDisplay = order.getId();
        if (orderIdDisplay != null && orderIdDisplay.length() > 8) {
            orderIdDisplay = orderIdDisplay.substring(orderIdDisplay.length() - 8).toUpperCase();
        }
        txtOrderId.setText("Đơn hàng #" + (orderIdDisplay != null ? orderIdDisplay : "N/A"));

        // Trạng thái
        String trangThai = order.getTrangThai();
        String trangThaiDisplay = order.getTrangThaiDisplay();
        txtOrderStatus.setText(trangThaiDisplay);
        
        // Màu sắc cho trạng thái
        int statusColor;
        switch (trangThai != null ? trangThai : "") {
            case "pending":
                statusColor = ContextCompat.getColor(this, android.R.color.holo_orange_dark);
                break;
            case "confirmed":
                statusColor = ContextCompat.getColor(this, android.R.color.holo_blue_dark);
                break;
            case "shipping":
                statusColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);
                break;
            case "delivered":
                statusColor = ContextCompat.getColor(this, android.R.color.holo_green_dark);
                break;
            case "cancelled":
                statusColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
                break;
            default:
                statusColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        }
        txtOrderStatus.setTextColor(statusColor);

        // Ngày đặt hàng
        if (order.getCreatedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(order.getCreatedAt());
                if (date != null) {
                    txtOrderDate.setText("Ngày đặt: " + outputFormat.format(date));
                } else {
                    txtOrderDate.setText("Ngày đặt: " + order.getCreatedAt());
                }
            } catch (Exception e) {
                txtOrderDate.setText("Ngày đặt: " + order.getCreatedAt());
            }
        } else {
            txtOrderDate.setText("Ngày đặt: N/A");
        }

        // Tổng tiền
        if (order.getTongTien() != null) {
            txtOrderTotal.setText(formatPrice(order.getTongTien()));
        } else {
            txtOrderTotal.setText("0₫");
        }

        // Địa chỉ giao hàng
        String diaChi = order.getDiaChiGiaoHang();
        if (diaChi != null && !diaChi.isEmpty()) {
            txtDeliveryAddress.setText(diaChi);
            txtDeliveryAddress.setVisibility(View.VISIBLE);
        } else {
            txtDeliveryAddress.setText("Chưa có thông tin");
            txtDeliveryAddress.setVisibility(View.VISIBLE);
        }

        // Số điện thoại
        String phone = order.getSoDienThoai();
        if (phone != null && !phone.isEmpty()) {
            txtPhoneNumber.setText(phone);
            txtPhoneNumber.setVisibility(View.VISIBLE);
        } else {
            txtPhoneNumber.setText("Chưa có thông tin");
            txtPhoneNumber.setVisibility(View.VISIBLE);
        }

        // Ghi chú
        String ghiChu = order.getGhiChu();
        if (ghiChu != null && !ghiChu.isEmpty()) {
            txtNote.setText(ghiChu);
            txtNote.setVisibility(View.VISIBLE);
        } else {
            txtNote.setVisibility(View.GONE);
        }

        // Danh sách sản phẩm
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(order.getItems());
            rvOrderItems.setAdapter(adapter);
            rvOrderItems.setVisibility(View.VISIBLE);
        } else {
            rvOrderItems.setVisibility(View.GONE);
        }

        // Hiển thị nút hủy đơn chỉ khi ở trạng thái pending hoặc confirmed
        if ("pending".equals(trangThai) || "confirmed".equals(trangThai)) {
            btnCancelOrder.setVisibility(View.VISIBLE);
        } else {
            btnCancelOrder.setVisibility(View.GONE);
        }
    }

    private void showCancelOrderDialog() {
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
            cancelOrder(lyDo);
        });

        dialog.show();
    }

    private void cancelOrder(String lyDo) {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        CancelOrderRequest request = new CancelOrderRequest(lyDo);
        apiService.cancelOrder(orderId, request).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    // Reload để cập nhật trạng thái
                    loadOrderDetails();
                } else {
                    String errorMsg = NetworkUtils.extractErrorMessage(response);
                    Toast.makeText(OrderDetailActivity.this, errorMsg != null ? errorMsg : "Không thể hủy đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            showEmpty(false);
        }
    }

    private void showEmpty(boolean empty) {
        if (txtEmpty != null) {
            txtEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (layoutOrderInfo != null) {
            layoutOrderInfo.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        if (layoutProducts != null) {
            layoutProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        if (layoutDelivery != null) {
            layoutDelivery.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        if (layoutTotal != null) {
            layoutTotal.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    private String formatPrice(long price) {
        return String.format("%,d₫", price).replace(",", ".");
    }
}
