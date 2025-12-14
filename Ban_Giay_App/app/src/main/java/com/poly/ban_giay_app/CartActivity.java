package com.poly.ban_giay_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poly.ban_giay_app.adapter.CartAdapter;
import com.poly.ban_giay_app.models.CartItem;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.models.Product;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.CartResponse;
import com.poly.ban_giay_app.network.model.NotificationListResponse;
import com.poly.ban_giay_app.network.model.OrderResponse;
import com.poly.ban_giay_app.network.model.ProductResponse;
import com.poly.ban_giay_app.network.request.OrderRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CartActivity extends AppCompatActivity {
    private RecyclerView rvCartItems;
    private CartAdapter cartAdapter;
    private CartManager cartManager;
    private CheckBox checkBoxSelectAll;
    private TextView txtTotalPrice;
    private Button btnCheckout;
    private LinearLayout layoutSelectAll, layoutBottom, layoutEmptyCart;
    private EditText edtSearch;
    private ImageView imgBell;
    private TextView txtNotificationBadge;
    private ImageView btnBack;
    private ImageView btnViewOrders;
    private View navAccount;
    private ImageView imgAccountIcon;
    private TextView tvAccountLabel;
    private SessionManager sessionManager;
    private ApiService apiService;
    private BroadcastReceiver cartUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_cart);

            sessionManager = new SessionManager(this);
            cartManager = CartManager.getInstance();
            cartManager.setContext(this);
            ApiClient.init(this);
            apiService = ApiClient.getApiService();

            // Apply insets
            View mainView = findViewById(R.id.main);
            if (mainView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
            }

            initViews();
            initAccountNav();
            updateAccountNavUi();
            setupRecyclerView();
            setupNavigation();
            setupCartUpdateReceiver();
            // Chỉ load từ API, không dùng local cart
            loadCartFromServer();
        } catch (Exception e) {
            Log.e("CartActivity", "Error in onCreate", e);
            Toast.makeText(this, "Lỗi khi khởi tạo giỏ hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Chỉ load từ API, không dùng local cart
        loadCartFromServer();
        updateAccountNavUi();
        loadNotificationCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver
        if (cartUpdateReceiver != null) {
            try {
                unregisterReceiver(cartUpdateReceiver);
            } catch (Exception e) {
                Log.e("CartActivity", "Error unregistering receiver", e);
            }
        }
    }

    private void setupCartUpdateReceiver() {
        cartUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.poly.ban_giay_app.CART_UPDATED".equals(intent.getAction())) {
                    Log.d("CartActivity", "✅ Cart updated broadcast received, reloading from API...");
                    // Reload ngay lập tức để hiển thị sản phẩm mới
                    // Delay ngắn để đảm bảo server đã lưu xong
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Log.d("CartActivity", "🔄 Reloading cart from API after broadcast...");
                        loadCartFromServer();
                    }, 300); // Giảm delay xuống 300ms để reload nhanh hơn
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.poly.ban_giay_app.CART_UPDATED");
        filter.setPriority(1000); // Đặt priority cao để nhận broadcast sớm
        
        // Android 13+ (API 33+) yêu cầu chỉ định RECEIVER_EXPORTED hoặc RECEIVER_NOT_EXPORTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cartUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(cartUpdateReceiver, filter);
        }
        Log.d("CartActivity", "✅ Cart update receiver registered");
    }

    private void initViews() {
        rvCartItems = findViewById(R.id.rvCartItems);
        checkBoxSelectAll = findViewById(R.id.checkBoxSelectAll);
        txtTotalPrice = findViewById(R.id.txtTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);
        layoutSelectAll = findViewById(R.id.layoutSelectAll);
        layoutBottom = findViewById(R.id.layoutBottom);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        edtSearch = findViewById(R.id.edtSearch);
        imgBell = findViewById(R.id.imgBell);
        txtNotificationBadge = findViewById(R.id.txtNotificationBadge);
        btnBack = findViewById(R.id.btnBack);
        btnViewOrders = findViewById(R.id.btnViewOrders);
    }

    private void setupNavigation() {
        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish(); // Quay về màn hình trước
            });
        }

        // Notification bell icon
        if (imgBell != null) {
            imgBell.setOnClickListener(v -> {
                Intent intent = new Intent(CartActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }

        // Home navigation
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(CartActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        // View Orders button
        if (btnViewOrders != null) {
            btnViewOrders.setOnClickListener(v -> {
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(CartActivity.this, OrderActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Vui lòng đăng nhập để xem đơn hàng", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
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
                    Intent intent = new Intent(CartActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(CartActivity.this, LoginActivity.class);
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
        if (rvCartItems == null) {
            Log.e("CartActivity", "rvCartItems is null!");
            return;
        }
        
        try {
            rvCartItems.setLayoutManager(new LinearLayoutManager(this));
            cartAdapter = new CartAdapter(cartManager.getCartItems(), new CartAdapter.OnCartItemListener() {
                @Override
                public void onItemSelectedChanged(int position, boolean isSelected) {
                    cartManager.setItemSelected(position, isSelected);
                    updateTotalPrice();
                    updateSelectAllCheckbox();
                }

                @Override
                public void onItemRemoved(int position) {
                    if (cartManager == null || cartManager.getCartItems().isEmpty() || position < 0 || position >= cartManager.getCartItems().size()) {
                        return;
                    }

                    CartItem item = cartManager.getCartItems().get(position);
                    // Xóa ngay trên UI để người dùng thấy phản hồi
                    cartManager.removeFromCart(position);
                    if (cartAdapter != null) {
                        cartAdapter.notifyDataSetChanged();
                    }
                    updateUI();

                    // Đồng bộ xóa với server
                    cartManager.removeItemFromServer(item, new CartManager.CartCallback() {
                        @Override
                        public void onSuccess(String message) {
                            Toast.makeText(CartActivity.this, message, Toast.LENGTH_SHORT).show();
                            // Reload để chắc chắn đồng bộ với server
                            loadCartFromServer();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(CartActivity.this, error, Toast.LENGTH_SHORT).show();
                            // Nếu lỗi, reload lại từ server để trạng thái nhất quán
                            loadCartFromServer();
                        }
                    });
                }
            });
            rvCartItems.setAdapter(cartAdapter);

            // Select all checkbox
            if (checkBoxSelectAll != null) {
                checkBoxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    cartManager.selectAll(isChecked);
                    if (cartAdapter != null) {
                        cartAdapter.notifyDataSetChanged();
                    }
                    updateTotalPrice();
                });
            }

            // Checkout button
            if (btnCheckout != null) {
                btnCheckout.setEnabled(true);
                btnCheckout.setClickable(true);
                btnCheckout.setFocusable(true);
                Log.d("CartActivity", "✅ Checkout button initialized and enabled");
                btnCheckout.setOnClickListener(v -> {
                    Log.d("CartActivity", "=== Checkout button clicked ===");
                    try {
                        if (cartManager == null) {
                            Log.e("CartActivity", "cartManager is null!");
                            Toast.makeText(this, "Lỗi: Giỏ hàng chưa được khởi tạo", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        int selectedCount = cartManager.getSelectedCount();
                        Log.d("CartActivity", "Selected items count: " + selectedCount);
                        
                        if (selectedCount == 0) {
                            Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        if (sessionManager == null || !sessionManager.isLoggedIn()) {
                            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                            startActivity(intent);
                            return;
                        }
                        
                        Log.d("CartActivity", "Navigating to PaymentMethodActivity...");
                        // Chuyển sang màn hình chọn phương thức thanh toán
                        Intent intent = new Intent(CartActivity.this, PaymentMethodActivity.class);
                        intent.putExtra("isFromCart", true);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("CartActivity", "Error in checkout button click", e);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e("CartActivity", "❌ btnCheckout is null!");
            }
        } catch (Exception e) {
            Log.e("CartActivity", "Error in setupRecyclerView", e);
            Toast.makeText(this, "Lỗi khi thiết lập giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        try {
            int itemCount = cartManager.getCartItems().size();
            Log.d("CartActivity", "=== updateUI() ===");
            Log.d("CartActivity", "Cart items count: " + itemCount);
            Log.d("CartActivity", "CartAdapter is null: " + (cartAdapter == null));
            if (cartAdapter != null) {
                Log.d("CartActivity", "CartAdapter item count: " + cartAdapter.getItemCount());
            }
            
            if (itemCount == 0) {
                // Hiển thị giỏ hàng trống
                Log.d("CartActivity", "Cart is empty, showing empty state");
                if (layoutEmptyCart != null) {
                    layoutEmptyCart.setVisibility(View.VISIBLE);
                }
                if (layoutSelectAll != null) {
                    layoutSelectAll.setVisibility(View.GONE);
                }
                if (layoutBottom != null) {
                    layoutBottom.setVisibility(View.GONE);
                }
                if (rvCartItems != null) {
                    rvCartItems.setVisibility(View.GONE);
                }
            } else {
                // Hiển thị danh sách sản phẩm
                Log.d("CartActivity", "Cart has " + itemCount + " items, showing list");
                if (layoutEmptyCart != null) {
                    layoutEmptyCart.setVisibility(View.GONE);
                }
                if (layoutSelectAll != null) {
                    layoutSelectAll.setVisibility(View.VISIBLE);
                }
                if (layoutBottom != null) {
                    layoutBottom.setVisibility(View.VISIBLE);
                    // Đảm bảo nút thanh toán luôn enabled và clickable
                    if (btnCheckout != null) {
                        btnCheckout.setEnabled(true);
                        btnCheckout.setClickable(true);
                        btnCheckout.setFocusable(true);
                        btnCheckout.setAlpha(1.0f);
                        Log.d("CartActivity", "✅ Checkout button enabled and visible");
                    }
                }
                if (rvCartItems != null) {
                    rvCartItems.setVisibility(View.VISIBLE);
                }
                
                // Đảm bảo adapter được update với dữ liệu mới nhất từ cartManager
                if (cartAdapter != null) {
                    Log.d("CartActivity", "Adapter item count before update: " + cartAdapter.getItemCount());
                    // Luôn update adapter với danh sách mới nhất từ cartManager
                    List<CartItem> currentItems = new ArrayList<>(cartManager.getCartItems());
                    cartAdapter.updateCartItems(currentItems);
                    Log.d("CartActivity", "✅ Adapter updated. Final item count: " + cartAdapter.getItemCount());
                } else {
                    Log.e("CartActivity", "❌ CartAdapter is null! Cannot update UI!");
                }
                
                updateTotalPrice();
                updateSelectAllCheckbox();
            }
        } catch (Exception e) {
            Log.e("CartActivity", "Error in updateUI", e);
        }
    }

    private void updateTotalPrice() {
        try {
            if (txtTotalPrice != null) {
                long total = cartManager.getTotalPrice();
                txtTotalPrice.setText(formatPrice(total));
            }
        } catch (Exception e) {
            Log.e("CartActivity", "Error in updateTotalPrice", e);
        }
    }

    private void updateSelectAllCheckbox() {
        try {
            if (checkBoxSelectAll != null) {
                checkBoxSelectAll.setChecked(cartManager.areAllSelected());
            }
        } catch (Exception e) {
            Log.e("CartActivity", "Error in updateSelectAllCheckbox", e);
        }
    }

    private String formatPrice(long price) {
        // Format giống như MainActivity: "199.000₫"
        return String.format("%,d₫", price).replace(",", ".");
    }

    private void loadCartFromServer() {
        try {
            Log.d("CartActivity", "=== loadCartFromServer() ===");
            
            if (sessionManager == null) {
                Log.e("CartActivity", "sessionManager is null!");
                return;
            }
            
            if (!sessionManager.isLoggedIn()) {
                // Nếu chưa đăng nhập, hiển thị giỏ hàng trống
                Log.d("CartActivity", "Not logged in, showing empty cart");
                if (cartManager != null) {
                    cartManager.getCartItems().clear();
                }
                if (cartAdapter != null) {
                    cartAdapter.updateCartItems(new ArrayList<>());
                }
                updateUI();
                return;
            }

            if (!NetworkUtils.isConnected(this)) {
                // Nếu không có mạng, giữ nguyên cart local và hiển thị thông báo
                Log.d("CartActivity", "No network, keeping local cart");
                Toast.makeText(this, "Không có kết nối mạng. Đang hiển thị giỏ hàng cục bộ.", Toast.LENGTH_SHORT).show();
                // KHÔNG clear cart, chỉ update UI với dữ liệu local
                if (cartAdapter != null && cartManager != null) {
                    cartAdapter.updateCartItems(cartManager.getCartItems());
                }
                updateUI();
                return;
            }

            String userId = sessionManager.getUserId();
            if (userId == null || userId.isEmpty()) {
                Log.w("CartActivity", "User ID is null or empty");
                updateUI();
                return;
            }
            
            if (apiService == null) {
                Log.e("CartActivity", "apiService is null!");
                Toast.makeText(this, "Lỗi khởi tạo dịch vụ. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                return;
            }
        
            Log.d("CartActivity", "Fetching cart for user: " + userId);

            apiService.getCart(userId).enqueue(new Callback<BaseResponse<CartResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<CartResponse>> call, Response<BaseResponse<CartResponse>> response) {
                runOnUiThread(() -> {
                    try {
                        Log.d("CartActivity", "=== API RESPONSE RECEIVED ===");
                        Log.d("CartActivity", "Response code: " + response.code());
                        Log.d("CartActivity", "Response isSuccessful: " + response.isSuccessful());
                        Log.d("CartActivity", "Response body is null: " + (response.body() == null));
                        
                        if (response.body() != null) {
                            Log.d("CartActivity", "Response success: " + response.body().getSuccess());
                            Log.d("CartActivity", "Response message: " + response.body().getMessage());
                            Log.d("CartActivity", "Response data is null: " + (response.body().getData() == null));
                        }
                        
                        if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                            CartResponse cartResponse = response.body().getData();
                            Log.d("CartActivity", "CartResponse is null: " + (cartResponse == null));
                            if (cartResponse != null) {
                                Log.d("CartActivity", "CartResponse items is null: " + (cartResponse.getItems() == null));
                                if (cartResponse.getItems() != null) {
                                    Log.d("CartActivity", "CartResponse items count: " + cartResponse.getItems().size());
                                } else {
                                    Log.w("CartActivity", "⚠️ CartResponse items is NULL!");
                                }
                            } else {
                                Log.w("CartActivity", "⚠️ CartResponse is NULL!");
                            }
                            
                            if (cartResponse != null && cartResponse.getItems() != null && !cartResponse.getItems().isEmpty()) {
                            // Tạo danh sách mới thay vì clear và add vào list cũ
                            List<CartItem> newCartItems = new ArrayList<>();
                            
                            // Convert CartItemResponse to CartItem và merge các items giống nhau
                            int addedCount = 0;
                            int mergedCount = 0;
                            int skippedCount = 0;
                            Log.d("CartActivity", "Processing " + cartResponse.getItems().size() + " items from server");
                            
                            for (CartResponse.CartItemResponse itemResponse : cartResponse.getItems()) {
                                Log.d("CartActivity", "Processing item - Size: " + itemResponse.getKichThuoc() + ", Quantity: " + itemResponse.getSoLuong());
                                
                                try {
                                    ProductResponse productResponse = itemResponse.getProduct();
                                    Log.d("CartActivity", "getProduct() returned: " + (productResponse != null ? "not null" : "null"));
                                    if (productResponse != null) {
                                        Log.d("CartActivity", "✅ ProductResponse is not null");
                                        Log.d("CartActivity", "Product ID: " + productResponse.getId());
                                        Log.d("CartActivity", "Product Name: " + productResponse.getName());
                                    // Convert ProductResponse to Product
                                    Product product = convertToProduct(productResponse);
                                    
                                    if (product != null) {
                                        String itemSize = itemResponse.getKichThuoc() != null ? itemResponse.getKichThuoc() : "";
                                        int itemQuantity = itemResponse.getSoLuong() != null ? itemResponse.getSoLuong() : 1;
                                        long itemGia = itemResponse.getGia() != null ? itemResponse.getGia() : 0;
                                        
                                        // Kiểm tra xem đã có item với cùng product ID và size chưa
                                        boolean found = false;
                                        for (CartItem existingItem : newCartItems) {
                                            if (existingItem.product != null && 
                                                existingItem.product.id != null && 
                                                product.id != null &&
                                                existingItem.product.id.equals(product.id) && 
                                                existingItem.size != null && 
                                                itemSize != null &&
                                                existingItem.size.equals(itemSize)) {
                                                // Tìm thấy item cùng sản phẩm và size -> merge (tăng quantity)
                                                existingItem.quantity += itemQuantity;
                                                found = true;
                                                mergedCount++;
                                                Log.d("CartActivity", "✅ Merged item: " + product.name + " (Size: " + itemSize + "). New quantity: " + existingItem.quantity);
                                                break;
                                            }
                                        }
                                        
                                        // Nếu không tìm thấy item cùng sản phẩm và size, tạo item mới
                                        if (!found) {
                                            CartItem cartItem = new CartItem(
                                                product,
                                                itemSize,
                                                itemQuantity,
                                                itemGia,
                                                itemResponse.getId()
                                            );
                                            Log.d("CartActivity", "Created CartItem with gia: " + itemGia + ", quantity: " + cartItem.quantity + ", total: " + cartItem.getTotalPrice());
                                            
                                            newCartItems.add(cartItem);
                                            addedCount++;
                                            Log.d("CartActivity", "✅ Added item: " + product.name + " x" + cartItem.quantity + " (Size: " + itemSize + ")");
                                        }
                                    } else {
                                        skippedCount++;
                                        Log.e("CartActivity", "❌ Failed to convert ProductResponse to Product");
                                    }
                                    } else {
                                        skippedCount++;
                                        Log.e("CartActivity", "❌ ProductResponse is NULL for item - Size: " + itemResponse.getKichThuoc() + ", Quantity: " + itemResponse.getSoLuong());
                                        Log.e("CartActivity", "   sanPhamIdRaw: " + itemResponse.getSanPhamId());
                                    }
                                } catch (Exception e) {
                                    skippedCount++;
                                    Log.e("CartActivity", "❌ Exception getting product for item: " + e.getMessage(), e);
                                }
                            }
                            
                            Log.d("CartActivity", "Processed items - Added: " + addedCount + ", Merged: " + mergedCount + ", Skipped: " + skippedCount);
                            
                            // Cập nhật cart manager với danh sách mới
                            if (cartManager != null) {
                                cartManager.getCartItems().clear();
                                cartManager.getCartItems().addAll(newCartItems);
                                // Tự động select tất cả items khi load từ server để hiển thị tổng tiền
                                cartManager.selectAll(true);
                                Log.d("CartActivity", "✅ Auto-selected all items after loading from server");
                                Log.d("CartActivity", "✅ Cart now has " + cartManager.getCartItems().size() + " items visible");
                            }
                            
                            Log.d("CartActivity", "✅ Loaded " + addedCount + " items from server. Total in cart: " + (cartManager != null ? cartManager.getCartItems().size() : 0));
                            
                            // Log từng item để debug
                            for (int i = 0; i < newCartItems.size(); i++) {
                                CartItem item = newCartItems.get(i);
                                Log.d("CartActivity", "  Item " + i + ": " + item.product.name + " x" + item.quantity + " (Size: " + item.size + ")");
                            }
                            
                            // Update adapter với danh sách mới - ĐẢM BẢO SỬ DỤNG CÙNG REFERENCE
                            if (cartAdapter != null) {
                                // Đảm bảo adapter sử dụng cùng list với cartManager
                                cartAdapter.updateCartItems(cartManager.getCartItems());
                                Log.d("CartActivity", "✅ Adapter updated with " + cartManager.getCartItems().size() + " items");
                                Log.d("CartActivity", "✅ Adapter getItemCount: " + cartAdapter.getItemCount());
                                
                                // Force refresh RecyclerView ngay lập tức
                                rvCartItems.post(() -> {
                                    cartAdapter.notifyDataSetChanged();
                                    Log.d("CartActivity", "✅ RecyclerView forced refresh on UI thread");
                                });
                            } else {
                                Log.e("CartActivity", "❌ CartAdapter is null, cannot update!");
                            }
                            
                            // Update UI - Đảm bảo RecyclerView được refresh
                            updateUI();
                        } else {
                            // Nếu cart rỗng từ server, chỉ clear nếu chắc chắn server có cart
                            Log.d("CartActivity", "Cart is empty on server or items is null");
                            // Kiểm tra xem có phải server trả về cart rỗng thật không
                            if (cartResponse != null && cartResponse.getItems() != null && cartResponse.getItems().isEmpty()) {
                                // Server có cart nhưng rỗng - clear local cart
                                Log.d("CartActivity", "Server cart exists but is empty - clearing local cart");
                                if (cartManager != null) {
                                    cartManager.getCartItems().clear();
                                }
                                if (cartAdapter != null) {
                                    cartAdapter.updateCartItems(new ArrayList<>());
                                }
                            } else {
                                // Có thể là lỗi parse hoặc server không trả về đúng format
                                Log.w("CartActivity", "Cart response format may be incorrect - keeping local cart");
                                // Giữ nguyên cart local
                                if (cartAdapter != null && cartManager != null) {
                                    cartAdapter.updateCartItems(cartManager.getCartItems());
                                }
                            }
                            updateUI();
                        }
                        } else {
                            // Nếu response không thành công, giữ nguyên cart local
                            Log.w("CartActivity", "Failed to load cart from server. Code: " + response.code());
                            
                            // Log chi tiết response body nếu có
                            if (response.body() != null) {
                                Log.w("CartActivity", "Response body success: " + response.body().getSuccess());
                                Log.w("CartActivity", "Response body message: " + response.body().getMessage());
                            } else {
                                // Thử đọc raw response body
                                try {
                                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                                    Log.w("CartActivity", "Error body: " + errorBody);
                                } catch (Exception e) {
                                    Log.e("CartActivity", "Error reading error body", e);
                                }
                            }
                            
                            String errorMsg = NetworkUtils.extractErrorMessage(response);
                            Log.w("CartActivity", "Error: " + errorMsg);
                            Toast.makeText(CartActivity.this, "Không thể tải giỏ hàng từ server. Đang hiển thị giỏ hàng cục bộ.", Toast.LENGTH_SHORT).show();
                            // KHÔNG clear cart, giữ nguyên dữ liệu local
                            if (cartAdapter != null && cartManager != null) {
                                cartAdapter.updateCartItems(cartManager.getCartItems());
                            }
                            updateUI();
                        }
                    } catch (Exception e) {
                        Log.e("CartActivity", "❌ Exception in onResponse callback", e);
                        Toast.makeText(CartActivity.this, "Lỗi khi xử lý dữ liệu giỏ hàng. Đang hiển thị giỏ hàng cục bộ.", Toast.LENGTH_SHORT).show();
                        if (cartAdapter != null && cartManager != null) {
                            cartAdapter.updateCartItems(cartManager.getCartItems());
                        }
                        updateUI();
                    }
                });
            }

            @Override
            public void onFailure(Call<BaseResponse<CartResponse>> call, Throwable t) {
                runOnUiThread(() -> {
                    try {
                        // Nếu lỗi network, giữ nguyên cart local
                        Log.e("CartActivity", "Network error loading cart: " + t.getMessage(), t);
                        Toast.makeText(CartActivity.this, "Lỗi kết nối. Đang hiển thị giỏ hàng cục bộ.", Toast.LENGTH_SHORT).show();
                        // KHÔNG clear cart, giữ nguyên dữ liệu local
                        if (cartAdapter != null && cartManager != null) {
                            cartAdapter.updateCartItems(cartManager.getCartItems());
                        }
                        updateUI();
                    } catch (Exception e) {
                        Log.e("CartActivity", "Error in onFailure callback", e);
                    }
                });
            }
        });
        } catch (Exception e) {
            Log.e("CartActivity", "Error in loadCartFromServer", e);
            Toast.makeText(this, "Lỗi khi tải giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Product convertToProduct(ProductResponse productResponse) {
        try {
            if (productResponse == null) {
                Log.e("CartActivity", "ProductResponse is null!");
                return null;
            }
            
            Log.d("CartActivity", "=== convertToProduct() ===");
            Log.d("CartActivity", "ProductResponse ID: " + productResponse.getId());
            Log.d("CartActivity", "ProductResponse Name: " + productResponse.getName());
            
            Product product = new Product();
            product.id = productResponse.getId() != null ? productResponse.getId() : "";
            product.name = productResponse.getName() != null ? productResponse.getName() : "";
            product.brand = productResponse.getBrand() != null ? productResponse.getBrand() : "";
            
            // Format price - handle null values
            Integer giaGoc = productResponse.getGiaGoc();
            Integer giaKhuyenMai = productResponse.getGiaKhuyenMai();
            product.priceOld = giaGoc != null ? formatPrice((long) giaGoc) : "";
            product.priceNew = giaKhuyenMai != null ? formatPrice((long) giaKhuyenMai) : "";
            
            Log.d("CartActivity", "Product priceOld: " + product.priceOld);
            Log.d("CartActivity", "Product priceNew: " + product.priceNew);
            
            // Get rating - handle null
            Double danhGia = productResponse.getDanhGia();
            product.rating = danhGia != null ? danhGia : 0.0;
            
            product.imageUrl = productResponse.getImageUrl();
            product.description = productResponse.getDescription();
            product.category = productResponse.getCategory();
            
            Log.d("CartActivity", "Product imageUrl: " + product.imageUrl);
            
            // Map image name to drawable resource if needed
            // Chỉ map nếu imageUrl không phải là URL (http/https)
            if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
                if (product.imageUrl.startsWith("http://") || product.imageUrl.startsWith("https://")) {
                    // Là URL từ server, giữ nguyên
                    Log.d("CartActivity", "Image is URL, keeping as is: " + product.imageUrl);
                } else {
                    // Là tên file, thử map với drawable
                    try {
                        String imageName = product.imageUrl;
                        // Loại bỏ extension và path
                        if (imageName.contains("/")) {
                            imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
                        }
                        imageName = imageName.replace(".img", "")
                                             .replace(".jpg", "")
                                             .replace(".png", "")
                                             .replace(".jpeg", "")
                                             .replace(".webp", "");
                        
                        Log.d("CartActivity", "Trying to map image name: " + imageName);
                        int imageRes = getResources().getIdentifier(imageName, "drawable", getPackageName());
                        if (imageRes != 0) {
                            product.imageRes = imageRes;
                            Log.d("CartActivity", "✅ Mapped image to drawable: " + imageName + " -> " + imageRes);
                        } else {
                            // Thử map với tên sản phẩm
                            String mappedName = mapImageNameFromProduct(product.name);
                            if (mappedName != null) {
                                imageRes = getResources().getIdentifier(mappedName, "drawable", getPackageName());
                                if (imageRes != 0) {
                                    product.imageRes = imageRes;
                                    Log.d("CartActivity", "✅ Mapped by product name: " + mappedName + " -> " + imageRes);
                                }
                            }
                            if (imageRes == 0) {
                                Log.w("CartActivity", "⚠️ Could not find drawable for: " + imageName + ", will use imageUrl");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CartActivity", "Error mapping image resource", e);
                    }
                }
            } else {
                Log.w("CartActivity", "⚠️ Product has no imageUrl");
            }
            
            Log.d("CartActivity", "✅ Converted product: " + product.name + " (ID: " + product.id + ")");
            Log.d("CartActivity", "   Final imageUrl: " + product.imageUrl);
            Log.d("CartActivity", "   Final imageRes: " + product.imageRes);
            return product;
        } catch (Exception e) {
            Log.e("CartActivity", "Error in convertToProduct", e);
            return null;
        }
    }
    
    /**
     * Map tên sản phẩm với tên drawable
     */
    private String mapImageNameFromProduct(String productName) {
        if (productName == null || productName.isEmpty()) {
            return null;
        }
        
        String lowerName = productName.toLowerCase();
        
        // Nike products
        if (lowerName.contains("nike") || lowerName.contains("air force") || lowerName.contains("af1")) {
            return "giay10";
        }
        
        // Vans products
        if (lowerName.contains("vans") || lowerName.contains("old skool") || lowerName.contains("authentic")) {
            return "giay11";
        }
        
        // Adidas products
        if (lowerName.contains("adidas") || lowerName.contains("ultraboost") || lowerName.contains("stan smith") || lowerName.contains("superstar")) {
            return "giay12";
        }
        
        // Puma products
        if (lowerName.contains("puma")) {
            return "giay13";
        }
        
        // Converse products
        if (lowerName.contains("converse") || lowerName.contains("chuck taylor")) {
            return "giay14";
        }
        
        // Default fallback
        return "giaymau";
    }

    private void showCheckoutDialog() {
        try {
            // Tạo dialog
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_checkout, null);
            builder.setView(dialogView);

            EditText edtDiaChi = dialogView.findViewById(R.id.edtDiaChi);
            EditText edtSoDienThoai = dialogView.findViewById(R.id.edtSoDienThoai);
            EditText edtGhiChu = dialogView.findViewById(R.id.edtGhiChu);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

            android.app.AlertDialog dialog = builder.create();
            dialog.setCancelable(true);

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnConfirm.setOnClickListener(v -> {
                String diaChi = edtDiaChi.getText().toString().trim();
                String soDienThoai = edtSoDienThoai.getText().toString().trim();
                String ghiChu = edtGhiChu.getText().toString().trim();

                // Validation
                if (diaChi.isEmpty()) {
                    Toast.makeText(CartActivity.this, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
                    edtDiaChi.requestFocus();
                    return;
                }

                if (soDienThoai.isEmpty()) {
                    Toast.makeText(CartActivity.this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                    edtSoDienThoai.requestFocus();
                    return;
                }

                // Validate số điện thoại (ít nhất 10 số)
                if (soDienThoai.length() < 10) {
                    Toast.makeText(CartActivity.this, "Số điện thoại phải có ít nhất 10 số", Toast.LENGTH_SHORT).show();
                    edtSoDienThoai.requestFocus();
                    return;
                }

                dialog.dismiss();
                createOrder(diaChi, soDienThoai, ghiChu);
            });

            dialog.show();
        } catch (Exception e) {
            Log.e("CartActivity", "Error showing checkout dialog", e);
            Toast.makeText(this, "Lỗi khi hiển thị form thanh toán", Toast.LENGTH_SHORT).show();
        }
    }

    private void createOrder(String diaChi, String soDienThoai, String ghiChu) {
        try {
            Log.d("CartActivity", "=== createOrder() ===");
            Log.d("CartActivity", "Dia chi: " + diaChi);
            Log.d("CartActivity", "So dien thoai: " + soDienThoai);
            
            if (!NetworkUtils.isConnected(this)) {
                Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ No network connection");
                return;
            }

            if (sessionManager == null) {
                Toast.makeText(this, "Lỗi: Phiên đăng nhập không hợp lệ", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ sessionManager is null");
                return;
            }

            String userId = sessionManager.getUserId();
            if (userId == null || userId.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ User ID is null or empty");
                return;
            }

            if (cartManager == null) {
                Toast.makeText(this, "Lỗi: Giỏ hàng chưa được khởi tạo", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ cartManager is null");
                return;
            }

            List<CartItem> selectedItems = cartManager.getSelectedItems();
            if (selectedItems == null || selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ No items selected");
                return;
            }

            Log.d("CartActivity", "Selected items count: " + selectedItems.size());

            if (apiService == null) {
                Toast.makeText(this, "Lỗi: Không thể kết nối đến server", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ apiService is null");
                return;
            }

            // Tạo OrderRequest
            OrderRequest request = new OrderRequest();
            request.setUserId(userId);
            
            List<OrderRequest.OrderItemRequest> orderItems = new ArrayList<>();
            long totalPrice = 0;
            
            for (CartItem cartItem : selectedItems) {
                if (cartItem == null || cartItem.product == null) {
                    Log.e("CartActivity", "❌ CartItem or product is null");
                    continue;
                }
                
                if (cartItem.product.id == null || cartItem.product.id.isEmpty()) {
                    Toast.makeText(this, "Sản phẩm " + cartItem.product.name + " không có ID, không thể tạo đơn hàng", Toast.LENGTH_LONG).show();
                    Log.e("CartActivity", "❌ Product ID is null for: " + cartItem.product.name);
                    return;
                }
                
                try {
                    // Ưu tiên dùng giá từ server (gia field), nếu không có thì parse từ priceNew
                    long itemPrice = 0;
                    if (cartItem.gia > 0) {
                        itemPrice = cartItem.gia;
                    } else if (cartItem.product.priceNew != null && !cartItem.product.priceNew.isEmpty()) {
                        String priceStr = cartItem.product.priceNew.replaceAll("[^0-9]", "");
                        if (!priceStr.isEmpty()) {
                            itemPrice = Long.parseLong(priceStr);
                        }
                    }
                    
                    if (itemPrice <= 0) {
                        Log.e("CartActivity", "❌ Invalid price for product: " + cartItem.product.name);
                        Toast.makeText(this, "Lỗi giá sản phẩm: " + cartItem.product.name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    long itemTotal = itemPrice * cartItem.quantity;
                    totalPrice += itemTotal;
                    
                    Log.d("CartActivity", "Adding item: " + cartItem.product.name + 
                          ", ID: " + cartItem.product.id + 
                          ", Quantity: " + cartItem.quantity + 
                          ", Size: " + cartItem.size + 
                          ", Price: " + itemPrice + 
                          ", Total: " + itemTotal);
                    
                    OrderRequest.OrderItemRequest orderItem = new OrderRequest.OrderItemRequest(
                        cartItem.product.id,
                        cartItem.product.name,
                        cartItem.quantity,
                        cartItem.size,
                        itemPrice
                    );
                    orderItems.add(orderItem);
                } catch (NumberFormatException e) {
                    Log.e("CartActivity", "Error parsing price for product: " + cartItem.product.name, e);
                    Toast.makeText(this, "Lỗi giá sản phẩm: " + cartItem.product.name, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            if (orderItems.isEmpty()) {
                Toast.makeText(this, "Không có sản phẩm hợp lệ để thanh toán", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "❌ No valid order items");
                return;
            }
            
            request.setItems(orderItems);
            request.setTongTien(totalPrice);
            request.setDiaChiGiaoHang(diaChi != null ? diaChi : "");
            request.setSoDienThoai(soDienThoai != null ? soDienThoai : "");
            request.setGhiChu(ghiChu != null ? ghiChu : "");

            // Log request để debug
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String requestJson = gson.toJson(request);
                Log.d("CartActivity", "Order request JSON: " + requestJson);
            } catch (Exception e) {
                Log.e("CartActivity", "Error serializing request", e);
            }

            Log.d("CartActivity", "Total price: " + totalPrice);
            Log.d("CartActivity", "Order items count: " + orderItems.size());

            if (btnCheckout != null) {
                btnCheckout.setEnabled(false);
                btnCheckout.setText("Đang xử lý...");
            }

            Log.d("CartActivity", "Calling API createOrder...");
            apiService.createOrder(request).enqueue(new Callback<BaseResponse<OrderResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<OrderResponse>> call, Response<BaseResponse<OrderResponse>> response) {
                try {
                    if (btnCheckout != null) {
                        btnCheckout.setEnabled(true);
                        btnCheckout.setText("Thanh toán");
                    }
                    
                    Log.d("CartActivity", "=== Order API Response ===");
                    Log.d("CartActivity", "Response code: " + response.code());
                    Log.d("CartActivity", "Response isSuccessful: " + response.isSuccessful());
                    Log.d("CartActivity", "Response body: " + (response.body() != null ? "not null" : "null"));
                    
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            BaseResponse<OrderResponse> body = response.body();
                            Log.d("CartActivity", "Response success: " + body.getSuccess());
                            Log.d("CartActivity", "Response message: " + body.getMessage());
                            
                            if (body.getSuccess()) {
                                Log.d("CartActivity", "✅ Order created successfully!");
                                Toast.makeText(CartActivity.this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                                
                                // Xóa các sản phẩm đã chọn khỏi local cart
                                if (cartManager != null) {
                                    cartManager.removeSelectedItems();
                                }
                                
                                // Reload cart từ server để đồng bộ (server đã xóa items đã thanh toán)
                                loadCartFromServer();
                                
                                // Chuyển đến màn hình đơn hàng sau delay để đảm bảo server đã lưu xong
                                // Tăng delay lên 1.5 giây để đảm bảo server đã lưu đơn hàng vào database
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    Log.d("CartActivity", "Navigating to OrderActivity after order creation");
                                    Intent intent = new Intent(CartActivity.this, OrderActivity.class);
                                    // Thêm flag để OrderActivity biết cần reload ngay
                                    intent.putExtra("shouldReload", true);
                                    startActivity(intent);
                                }, 1500);
                            } else {
                                String errorMsg = body.getMessage() != null ? body.getMessage() : "Không thể tạo đơn hàng";
                                Log.e("CartActivity", "❌ Order creation failed: " + errorMsg);
                                Toast.makeText(CartActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w("CartActivity", "⚠️ Response body is null");
                            Toast.makeText(CartActivity.this, "Không nhận được phản hồi từ server", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMsg = NetworkUtils.extractErrorMessage(response);
                        Log.e("CartActivity", "❌ Order API error. Code: " + response.code() + ", Message: " + errorMsg);
                        
                        // Đọc error body nếu có
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e("CartActivity", "Error body: " + errorBody);
                                
                                // Parse JSON để lấy message
                                try {
                                    org.json.JSONObject json = new org.json.JSONObject(errorBody);
                                    if (json.has("message")) {
                                        errorMsg = json.getString("message");
                                    }
                                } catch (Exception e) {
                                    // Ignore
                                }
                            } catch (Exception e) {
                                Log.e("CartActivity", "Error reading error body", e);
                            }
                        }
                        
                        Toast.makeText(CartActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e("CartActivity", "❌ Exception in onResponse", e);
                    if (btnCheckout != null) {
                        btnCheckout.setEnabled(true);
                        btnCheckout.setText("Thanh toán");
                    }
                    Toast.makeText(CartActivity.this, "Lỗi khi xử lý phản hồi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<OrderResponse>> call, Throwable t) {
                try {
                    Log.e("CartActivity", "❌ Order API failure", t);
                    if (btnCheckout != null) {
                        btnCheckout.setEnabled(true);
                        btnCheckout.setText("Thanh toán");
                    }
                    
                    String errorMsg = t.getMessage() != null ? t.getMessage() : "Không thể kết nối đến server";
                    
                    // Xử lý các loại lỗi khác nhau
                    if (t instanceof java.net.UnknownHostException || 
                        t instanceof java.net.ConnectException) {
                        errorMsg = "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
                    } else if (t instanceof java.net.SocketTimeoutException) {
                        errorMsg = "Kết nối quá thời gian. Vui lòng thử lại.";
                    } else if (t instanceof java.io.IOException) {
                        errorMsg = "Lỗi kết nối mạng. Vui lòng thử lại.";
                    }
                    
                    Toast.makeText(CartActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("CartActivity", "❌ Exception in onFailure", e);
                }
            }
        });
        } catch (Exception e) {
            Log.e("CartActivity", "❌ Exception in createOrder", e);
            if (btnCheckout != null) {
                btnCheckout.setEnabled(true);
                btnCheckout.setText("Thanh toán");
            }
            Toast.makeText(this, "Lỗi khi tạo đơn hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadNotificationCount() {
        if (!sessionManager.isLoggedIn()) {
            updateNotificationBadge(0);
            return;
        }
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            updateNotificationBadge(0);
            return;
        }

        if (!NetworkUtils.isConnected(this)) {
            return;
        }

        apiService.getNotifications(userId, false).enqueue(new Callback<BaseResponse<NotificationListResponse>>() {
            @Override
            public void onResponse(Call<BaseResponse<NotificationListResponse>> call, Response<BaseResponse<NotificationListResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    NotificationListResponse notificationData = response.body().getData();
                    if (notificationData != null) {
                        updateNotificationBadge(notificationData.getUnreadCount());
                    } else {
                        updateNotificationBadge(0);
                    }
                } else {
                    updateNotificationBadge(0);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<NotificationListResponse>> call, Throwable t) {
                Log.e("CartActivity", "Error loading notification count: " + t.getMessage());
                updateNotificationBadge(0);
            }
        });
    }

    private void updateNotificationBadge(int count) {
        if (txtNotificationBadge != null) {
            if (count > 0) {
                // Hiển thị dấu đỏ nhỏ (không cần số)
                txtNotificationBadge.setText(""); // Để trống để chỉ hiển thị dấu đỏ
                txtNotificationBadge.setVisibility(View.VISIBLE);
                Log.d("CartActivity", "✅ Badge hiển thị - Có " + count + " thông báo chưa đọc");
            } else {
                txtNotificationBadge.setVisibility(View.GONE);
                Log.d("CartActivity", "Badge ẩn - Không có thông báo chưa đọc");
            }
        }
    }
}

