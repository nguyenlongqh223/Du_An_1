package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.poly.ban_giay_app.adapter.ProductAdapter;
import com.poly.ban_giay_app.models.Product;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.NotificationListResponse;
import com.poly.ban_giay_app.network.model.ProductListResponse;
import com.poly.ban_giay_app.network.model.ProductResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private View navAccount;
    private ImageView imgAccountIcon;
    private TextView tvAccountLabel;
    private ImageView imgBell;
    private TextView txtNotificationBadge;

    // RecyclerViews and Adapters for products
    private RecyclerView rvTop, rvMen, rvSearchResults;
    private ProductAdapter topProductAdapter, menProductAdapter, searchAdapter;
    private List<Product> topProductList, menProductList, allProductList, searchResultList;
    private EditText edtSearch;
    private TextView txtSearchTitle;
    private ImageView imgBanner;
    private View layoutTopSection, layoutMenSection;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private ApiService apiService;
    private Handler notificationCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable notificationCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        // Khởi tạo ApiClient với context để có thể thêm token vào header
        ApiClient.init(this);
        apiService = ApiClient.getApiService();

        // Init account navigation
        initAccountNav();
        updateAccountNavUi();
        
        // Init notification icon
        initNotificationIcon();

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Init product lists
        initProductLists();
        
        // Load products from API
        loadProductsFromApi();
        
        // Init search functionality
        initSearch();

        // Setup "Xem tất cả" buttons
        setupViewAllButtons();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccountNavUi();
        loadNotificationCount();
        startNotificationCheck();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopNotificationCheck();
    }
    
    private void startNotificationCheck() {
        // Dừng check cũ nếu có
        stopNotificationCheck();
        
        // Check thông báo mới mỗi 30 giây
        notificationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                loadNotificationCount();
                // Lên lịch check lại sau 30 giây
                notificationCheckHandler.postDelayed(this, 30000);
            }
        };
        notificationCheckHandler.postDelayed(notificationCheckRunnable, 30000);
    }
    
    private void stopNotificationCheck() {
        if (notificationCheckRunnable != null) {
            notificationCheckHandler.removeCallbacks(notificationCheckRunnable);
            notificationCheckRunnable = null;
        }
    }
    
    private void initNotificationIcon() {
        imgBell = findViewById(R.id.imgBell);
        txtNotificationBadge = findViewById(R.id.txtNotificationBadge);
        
        if (imgBell != null) {
            imgBell.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
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

        // Dùng API endpoint chuyên dụng để lấy số thông báo chưa đọc (nhanh hơn)
        apiService.getUnreadNotificationCount(userId).enqueue(new Callback<BaseResponse<Integer>>() {
            @Override
            public void onResponse(Call<BaseResponse<Integer>> call, 
                                 Response<BaseResponse<Integer>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    Integer count = response.body().getData();
                    if (count != null) {
                        updateNotificationBadge(count);
                        Log.d("MainActivity", "Unread notifications count: " + count);
                    } else {
                        updateNotificationBadge(0);
                    }
                } else {
                    // Fallback: thử dùng API cũ nếu API mới không hoạt động
                    Log.w("MainActivity", "Unread count API failed, trying fallback...");
                    loadNotificationCountFallback(userId);
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Integer>> call, Throwable t) {
                Log.e("MainActivity", "Error loading notification count: " + t.getMessage());
                // Fallback: thử dùng API cũ
                loadNotificationCountFallback(userId);
            }
        });
    }
    
    private void loadNotificationCountFallback(String userId) {
        // Fallback: dùng API cũ để lấy số thông báo chưa đọc
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
                Log.e("MainActivity", "Fallback API also failed: " + t.getMessage());
                updateNotificationBadge(0);
            }
        });
    }
    
    private void updateNotificationBadge(int count) {
        if (txtNotificationBadge != null) {
            if (count > 0) {
                txtNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                txtNotificationBadge.setVisibility(View.VISIBLE);
            } else {
                txtNotificationBadge.setVisibility(View.GONE);
            }
        }
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

        // Cart navigation
        View navCart = findViewById(R.id.navCart);
        if (navCart != null) {
            navCart.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CartActivity.class);
                startActivity(intent);
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

    // Initialize product lists and set them to RecyclerViews
    private void initProductLists() {
        // Top selling products (hiển thị dạng ngang, có thể scroll)
        rvTop = findViewById(R.id.rvTop);
        rvTop.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        topProductList = new ArrayList<>();

        topProductAdapter = new ProductAdapter(topProductList);
        rvTop.setAdapter(topProductAdapter);

        // Men's shoes (nếu muốn bỏ thì xoá phần này trong layout)
        rvMen = findViewById(R.id.rvMen);
        rvMen.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        menProductList = new ArrayList<>();

        menProductAdapter = new ProductAdapter(menProductList);
        rvMen.setAdapter(menProductAdapter);
        
        // Combine all products for search
        allProductList = new ArrayList<>();
        allProductList.addAll(topProductList);
        allProductList.addAll(menProductList);
    }

    private void loadProductsFromApi() {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng. Đang tải dữ liệu mẫu...", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "No network connection - loading sample data");
            loadSampleProducts();
            return;
        }

        Log.d("MainActivity", "Starting to load products from API...");
        Log.d("MainActivity", "API Base URL: " + com.poly.ban_giay_app.BuildConfig.API_BASE_URL);

        // Load top selling products - dùng API mới
        apiService.getBestSellingProducts(10).enqueue(new Callback<List<ProductResponse>>() {
            @Override
            public void onResponse(Call<List<ProductResponse>> call, Response<List<ProductResponse>> response) {
                try {
                    Log.d("MainActivity", "Top products response code: " + response.code());
                    if (response.isSuccessful()) {
                        List<ProductResponse> products = response.body();
                        Log.d("MainActivity", "Top products data: " + (products != null ? products.size() : "null"));
                        if (products != null && !products.isEmpty()) {
                            topProductList.clear();
                            for (ProductResponse productResponse : products) {
                                if (productResponse != null) {
                                    String imageUrl = productResponse.getImageUrl();
                                    Log.d("MainActivity", "Processing product: " + productResponse.getName() + 
                                          " - Price: " + productResponse.getPriceNew() + 
                                          " - Image: " + imageUrl);
                                    Product product = convertToProduct(productResponse);
                                    if (product != null && product.name != null && !product.name.isEmpty()) {
                                        topProductList.add(product);
                                        Log.d("MainActivity", "Added product: " + product.name + 
                                                " - Price: " + product.priceNew + 
                                                " - ImageUrl: " + product.imageUrl + 
                                                " - ImageRes: " + product.imageRes);
                                    } else {
                                        Log.w("MainActivity", "Failed to convert product: " + productResponse.getName());
                                    }
                                }
                            }
                            runOnUiThread(() -> {
                                topProductAdapter.notifyDataSetChanged();
                                updateAllProductList();
                                Log.d("MainActivity", "Top products updated: " + topProductList.size());
                            });
                        } else {
                            Log.w("MainActivity", "Top products list is empty or null - API returned empty array");
                            // Nếu API trả về mảng rỗng, dùng sample data
                            runOnUiThread(() -> {
                                if (topProductList.isEmpty()) {
                                    Log.w("MainActivity", "API returned empty, loading sample data for top products");
                                    loadSampleTopProducts();
                                    Toast.makeText(MainActivity.this, "Không có sản phẩm từ server. Đang hiển thị dữ liệu mẫu.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Không có sản phẩm bán chạy", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        String errorMsg = NetworkUtils.extractErrorMessage(response);
                        Log.e("MainActivity", "Error loading top products: " + errorMsg + ", Code: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Lỗi tải sản phẩm: " + errorMsg, Toast.LENGTH_SHORT).show();
                        });
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e("MainActivity", "Error body: " + errorBody);
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error reading error body", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Exception loading top products", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<List<ProductResponse>> call, Throwable t) {
                Log.e("MainActivity", "Failed to load top products", t);
                Log.e("MainActivity", "Error type: " + t.getClass().getName());
                Log.e("MainActivity", "Error message: " + t.getMessage());
                if (t.getCause() != null) {
                    Log.e("MainActivity", "Cause: " + t.getCause().getMessage());
                }
                t.printStackTrace();
                
                // Thử fallback API: getAllProducts hoặc legacy API
                if (topProductList.isEmpty()) {
                    Log.w("MainActivity", "Trying fallback API: getAllProducts for top products");
                    tryFallbackTopProducts();
                } else {
                    runOnUiThread(() -> {
                        String errorMsg = t.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = "Không thể kết nối đến server. Đang dùng dữ liệu mẫu.";
                        }
                        Toast.makeText(MainActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

        // Load men's products - dùng API mới
        apiService.getProductsByCategory("nam").enqueue(new Callback<List<ProductResponse>>() {
            @Override
            public void onResponse(Call<List<ProductResponse>> call, Response<List<ProductResponse>> response) {
                try {
                    Log.d("MainActivity", "Men products response code: " + response.code());
                    if (response.isSuccessful()) {
                        List<ProductResponse> products = response.body();
                        Log.d("MainActivity", "Men products data: " + (products != null ? products.size() : "null"));
                        if (products != null && !products.isEmpty()) {
                            menProductList.clear();
                            for (ProductResponse productResponse : products) {
                                if (productResponse != null) {
                                    Log.d("MainActivity", "Processing men product: " + productResponse.getName() + 
                                          " - Price: " + productResponse.getPriceNew() + 
                                          " - Image: " + productResponse.getImageUrl());
                                    Product product = convertToProduct(productResponse);
                                    if (product != null && product.name != null && !product.name.isEmpty()) {
                                        menProductList.add(product);
                                        Log.d("MainActivity", "Added men product: " + product.name + " - " + product.priceNew);
                                    } else {
                                        Log.w("MainActivity", "Failed to convert men product: " + productResponse.getName());
                                    }
                                }
                            }
                            runOnUiThread(() -> {
                                menProductAdapter.notifyDataSetChanged();
                                updateAllProductList();
                                Log.d("MainActivity", "Men products updated: " + menProductList.size());
                            });
                        } else {
                            Log.w("MainActivity", "Men products list is empty or null - API returned empty array");
                            // Nếu API trả về mảng rỗng, dùng sample data
                            runOnUiThread(() -> {
                                if (menProductList.isEmpty()) {
                                    Log.w("MainActivity", "API returned empty, loading sample data for men products");
                                    loadSampleMenProducts();
                                    Toast.makeText(MainActivity.this, "Không có sản phẩm từ server. Đang hiển thị dữ liệu mẫu.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Không có sản phẩm nam", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        String errorMsg = NetworkUtils.extractErrorMessage(response);
                        Log.e("MainActivity", "Error loading men products: " + errorMsg + ", Code: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Lỗi tải sản phẩm: " + errorMsg, Toast.LENGTH_SHORT).show();
                        });
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e("MainActivity", "Error body: " + errorBody);
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error reading error body", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Exception loading men products", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<List<ProductResponse>> call, Throwable t) {
                Log.e("MainActivity", "Failed to load men products", t);
                Log.e("MainActivity", "Error type: " + t.getClass().getName());
                Log.e("MainActivity", "Error message: " + t.getMessage());
                if (t.getCause() != null) {
                    Log.e("MainActivity", "Cause: " + t.getCause().getMessage());
                }
                t.printStackTrace();
                
                // Thử fallback API: getAllProducts hoặc legacy API
                if (menProductList.isEmpty()) {
                    Log.w("MainActivity", "Trying fallback API: getAllProducts for men products");
                    tryFallbackMenProducts();
                } else {
                    runOnUiThread(() -> {
                        String errorMsg = t.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = "Không thể kết nối đến server. Đang dùng dữ liệu mẫu.";
                        }
                        Toast.makeText(MainActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateAllProductList() {
        allProductList.clear();
        allProductList.addAll(topProductList);
        allProductList.addAll(menProductList);
    }

    /**
     * Load dữ liệu mẫu khi không có kết nối API
     */
    private void loadSampleProducts() {
        loadSampleTopProducts();
        loadSampleMenProducts();
    }

    /**
     * Load dữ liệu mẫu cho top products
     */
    private void loadSampleTopProducts() {
        topProductList.clear();
        
        // Tạo các sản phẩm mẫu với ảnh từ drawable
        topProductList.add(new Product(
            "Giày Sneaker Nike Air Max",
            "1.200.000₫",
            "899.000₫",
            R.drawable.giay2
        ));
        
        topProductList.add(new Product(
            "Giày Thể Thao Adidas Ultraboost",
            "1.500.000₫",
            "1.199.000₫",
            R.drawable.giay3
        ));
        
        topProductList.add(new Product(
            "Giày Chạy Bộ Puma Speed",
            "800.000₫",
            "599.000₫",
            R.drawable.giay4
        ));
        
        topProductList.add(new Product(
            "Giày Sneaker Converse Classic",
            "900.000₫",
            "699.000₫",
            R.drawable.giay5
        ));
        
        topProductList.add(new Product(
            "Giày Thể Thao New Balance",
            "1.100.000₫",
            "849.000₫",
            R.drawable.giay7
        ));
        
        topProductAdapter.notifyDataSetChanged();
        updateAllProductList();
        Log.d("MainActivity", "✅ Loaded " + topProductList.size() + " sample top products");
    }

    /**
     * Load dữ liệu mẫu cho men products
     */
    private void loadSampleMenProducts() {
        menProductList.clear();
        
        // Tạo các sản phẩm mẫu cho giày nam
        menProductList.add(new Product(
            "Giày Nam Sneaker Vans Old Skool",
            "1.000.000₫",
            "749.000₫",
            R.drawable.giay8
        ));
        
        menProductList.add(new Product(
            "Giày Nam Thể Thao Reebok",
            "950.000₫",
            "699.000₫",
            R.drawable.giay9
        ));
        
        menProductList.add(new Product(
            "Giày Nam Chạy Bộ Asics",
            "1.300.000₫",
            "999.000₫",
            R.drawable.giay10
        ));
        
        menProductList.add(new Product(
            "Giày Nam Sneaker Jordan",
            "2.000.000₫",
            "1.599.000₫",
            R.drawable.giay11
        ));
        
        menProductList.add(new Product(
            "Giày Nam Thể Thao Under Armour",
            "1.400.000₫",
            "1.099.000₫",
            R.drawable.giay12
        ));
        
        menProductList.add(new Product(
            "Giày Nam Sneaker Fila",
            "850.000₫",
            "649.000₫",
            R.drawable.giay13
        ));
        
        menProductList.add(new Product(
            "Giày Nam Thể Thao Skechers",
            "900.000₫",
            "699.000₫",
            R.drawable.giay14
        ));
        
        menProductList.add(new Product(
            "Giày Nam Chạy Bộ Mizuno",
            "1.100.000₫",
            "849.000₫",
            R.drawable.giay15
        ));
        
        menProductAdapter.notifyDataSetChanged();
        updateAllProductList();
        Log.d("MainActivity", "✅ Loaded " + menProductList.size() + " sample men products");
    }

    /**
     * Thử fallback API cho top products khi API chính fail
     */
    private void tryFallbackTopProducts() {
        Log.d("MainActivity", "Trying fallback API: getAllProducts for top products");
        
        // Thử dùng getAllProducts với limit 10
        apiService.getAllProducts(1, 10, null, null, null, null, null, null).enqueue(new Callback<ProductListResponse>() {
            @Override
            public void onResponse(Call<ProductListResponse> call, Response<ProductListResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ProductListResponse productListResponse = response.body();
                        List<ProductResponse> products = productListResponse.getProducts();
                        
                        if (products != null && !products.isEmpty()) {
                            topProductList.clear();
                            // Lấy 10 sản phẩm đầu tiên làm top products
                            int count = Math.min(10, products.size());
                            for (int i = 0; i < count; i++) {
                                ProductResponse productResponse = products.get(i);
                                if (productResponse != null) {
                                    Product product = convertToProduct(productResponse);
                                    if (product != null && product.name != null && !product.name.isEmpty()) {
                                        topProductList.add(product);
                                    }
                                }
                            }
                            runOnUiThread(() -> {
                                topProductAdapter.notifyDataSetChanged();
                                updateAllProductList();
                                Log.d("MainActivity", "✅ Fallback API success: Loaded " + topProductList.size() + " top products");
                            });
                            return;
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in fallback getAllProducts", e);
                }
                
                // Nếu fallback API cũng fail, dùng sample data
                runOnUiThread(() -> {
                    if (topProductList.isEmpty()) {
                        Log.w("MainActivity", "Fallback API failed, loading sample data");
                        loadSampleTopProducts();
                    }
                });
            }

            @Override
            public void onFailure(Call<ProductListResponse> call, Throwable t) {
                Log.e("MainActivity", "Fallback API getAllProducts also failed", t);
                runOnUiThread(() -> {
                    if (topProductList.isEmpty()) {
                        Log.w("MainActivity", "All APIs failed, loading sample data");
                        loadSampleTopProducts();
                    }
                });
            }
        });
    }

    /**
     * Thử fallback API cho men products khi API chính fail
     */
    private void tryFallbackMenProducts() {
        Log.d("MainActivity", "Trying fallback API: getAllProducts for men products");
        
        // Thử dùng getAllProducts với filter danh_muc = "nam"
        apiService.getAllProducts(1, 20, "nam", null, null, null, null, null).enqueue(new Callback<ProductListResponse>() {
            @Override
            public void onResponse(Call<ProductListResponse> call, Response<ProductListResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ProductListResponse productListResponse = response.body();
                        List<ProductResponse> products = productListResponse.getProducts();
                        
                        if (products != null && !products.isEmpty()) {
                            menProductList.clear();
                            for (ProductResponse productResponse : products) {
                                if (productResponse != null) {
                                    Product product = convertToProduct(productResponse);
                                    if (product != null && product.name != null && !product.name.isEmpty()) {
                                        menProductList.add(product);
                                    }
                                }
                            }
                            runOnUiThread(() -> {
                                menProductAdapter.notifyDataSetChanged();
                                updateAllProductList();
                                Log.d("MainActivity", "✅ Fallback API success: Loaded " + menProductList.size() + " men products");
                            });
                            return;
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in fallback getAllProducts for men", e);
                }
                
                // Nếu fallback API cũng fail, dùng sample data
                runOnUiThread(() -> {
                    if (menProductList.isEmpty()) {
                        Log.w("MainActivity", "Fallback API failed, loading sample data");
                        loadSampleMenProducts();
                    }
                });
            }

            @Override
            public void onFailure(Call<ProductListResponse> call, Throwable t) {
                Log.e("MainActivity", "Fallback API getAllProducts for men also failed", t);
                runOnUiThread(() -> {
                    if (menProductList.isEmpty()) {
                        Log.w("MainActivity", "All APIs failed, loading sample data");
                        loadSampleMenProducts();
                    }
                });
            }
        });
    }

    /**
     * Chuyển đổi ProductResponse từ API sang Product model để hiển thị
     */
    private Product convertToProduct(ProductResponse productResponse) {
        if (productResponse == null) {
            Log.w("MainActivity", "ProductResponse is null");
            return null;
        }

        String name = productResponse.getName();
        
        // Debug: Log tất cả các field ảnh từ API
        productResponse.logImageFields(name);
        
        String imageUrl = productResponse.getImageUrl();

        Log.d("MainActivity", "Converting product - Name: " + name + 
              ", ImageUrl from getImageUrl(): " + imageUrl);
        
        // Log để kiểm tra xem API có trả về ảnh không
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.w("MainActivity", "⚠️ API không trả về dữ liệu ảnh cho sản phẩm: " + name);
        } else {
            Log.d("MainActivity", "✅ API có trả về ảnh: " + imageUrl);
        }

        // Đảm bảo có ít nhất tên sản phẩm
        if (name == null || name.trim().isEmpty()) {
            Log.w("MainActivity", "Product has no name, skipping");
            return null;
        }

        // Lấy giá trực tiếp từ Integer (từ MongoDB: gia_goc và gia_khuyen_mai)
        Integer giaGoc = productResponse.getGiaGoc();
        Integer giaKhuyenMai = productResponse.getGiaKhuyenMai();
        
        String priceOld = null;
        String priceNew = null;

        // Format giá gốc (gia_goc) - hiển thị khi có giá khuyến mãi
        if (giaGoc != null && giaGoc > 0) {
            priceOld = formatPrice(giaGoc);
        }
        
        // Format giá khuyến mãi (gia_khuyen_mai) - giá hiển thị chính
        if (giaKhuyenMai != null && giaKhuyenMai > 0) {
            priceNew = formatPrice(giaKhuyenMai);
        } else if (giaGoc != null && giaGoc > 0) {
            // Nếu không có giá khuyến mãi, dùng giá gốc làm giá mới
            priceNew = formatPrice(giaGoc);
            priceOld = null; // Không hiển thị giá cũ nếu không có khuyến mãi
        }

        // Nếu không có giá nào, set giá mặc định
        if (priceNew == null || priceNew.trim().isEmpty()) {
            priceNew = "0₫";
        }

        Log.d("MainActivity", "Formatted prices - Old: " + priceOld + ", New: " + priceNew);

        // Tạo Product với URL ảnh (nếu có) hoặc resource mặc định
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            // Đảm bảo imageUrl là duy nhất cho từng sản phẩm
            String finalImageUrl = imageUrl.trim();
            
            // Nếu là base64 image, giữ nguyên
            if (finalImageUrl.startsWith("data:image")) {
                // Base64 image - giữ nguyên
            } else if (finalImageUrl.startsWith("http://") || finalImageUrl.startsWith("https://")) {
                // URL từ server - giữ nguyên
            } else {
                // Tên file ảnh local - có thể cần xử lý thêm
                Log.d("MainActivity", "Image is local file name: " + finalImageUrl);
            }
            
            Product product = new Product(
                name, 
                priceOld != null ? priceOld : "", 
                priceNew, 
                finalImageUrl
            );
            // Lưu ID sản phẩm từ MongoDB
            product.id = productResponse.getId();
            // Đảm bảo imageUrl được set với giá trị cuối cùng
            product.imageUrl = finalImageUrl;
            // Đảm bảo imageRes = 0 khi dùng imageUrl
            product.imageRes = 0;
            Log.d("MainActivity", "✅ Created product: " + name + 
                    " | imageUrl: " + product.imageUrl + 
                    " | ID: " + product.id);
            return product;
        } else {
            // Nếu không có URL ảnh, dùng ảnh mặc định
            Product product = new Product(
                name, 
                priceOld != null ? priceOld : "", 
                priceNew, 
                R.drawable.giaymau
            );
            // Lưu ID sản phẩm từ MongoDB
            product.id = productResponse.getId();
            // Đảm bảo imageUrl = null và imageRes được set
            product.imageUrl = null;
            product.imageRes = R.drawable.giaymau;
            Log.d("MainActivity", "✅ Created product with default image: " + name + 
                    " | imageRes: " + product.imageRes + 
                    " | ID: " + product.id);
            return product;
        }
    }
    
    /**
     * Format giá thành định dạng VNĐ
     */
    private String formatPrice(int price) {
        return String.format("%,d₫", price).replace(",", ".");
    }
    
    private void initSearch() {
        edtSearch = findViewById(R.id.edtSearch);
        txtSearchTitle = findViewById(R.id.txtSearchTitle);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        imgBanner = findViewById(R.id.imgBanner);
        layoutTopSection = findViewById(R.id.layoutTopSection);
        layoutMenSection = findViewById(R.id.layoutMenSection);
        
        // Setup RecyclerView for search results
        rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
        searchResultList = new ArrayList<>();
        searchAdapter = new ProductAdapter(searchResultList);
        rvSearchResults.setAdapter(searchAdapter);
        
        // Add text change listener - sử dụng Handler với delay để tránh mất text khi gõ tiếng Việt
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // Hủy runnable cũ nếu có
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                
                // Lấy text hiện tại
                final String query = s.toString();
                
                // Tạo runnable mới với delay 300ms để đợi người dùng gõ xong
                searchRunnable = () -> {
                    String trimmedQuery = query.trim();
                    if (trimmedQuery.isEmpty()) {
                        // Show normal layout
                        showNormalLayout();
                    } else {
                        // Show search results - giữ nguyên text gốc, chỉ normalize khi so sánh
                        performSearch(trimmedQuery);
                    }
                };
                
                // Delay 300ms trước khi thực hiện tìm kiếm
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }
    
    private void performSearch(String query) {
        searchResultList.clear();
        
        // Normalize query for Vietnamese search (bỏ dấu để tìm kiếm linh hoạt hơn)
        String normalizedQuery = normalizeVietnamese(query.toLowerCase());
        
        // Filter products by name - tìm kiếm cả với dấu và không dấu
        for (Product product : allProductList) {
            String productName = product.name.toLowerCase();
            String normalizedProductName = normalizeVietnamese(productName);
            
            // Tìm kiếm cả text gốc và text đã normalize
            if (productName.contains(query.toLowerCase()) || 
                normalizedProductName.contains(normalizedQuery) ||
                productName.contains(normalizedQuery) ||
                normalizedProductName.contains(query.toLowerCase())) {
                searchResultList.add(product);
            }
        }
        
        // Update UI
        if (searchResultList.isEmpty()) {
            txtSearchTitle.setText("Không tìm thấy sản phẩm");
        } else {
            txtSearchTitle.setText("Kết quả tìm kiếm (" + searchResultList.size() + ")");
        }
        
        searchAdapter.notifyDataSetChanged();
        showSearchLayout();
    }
    
    // Normalize Vietnamese text for better search (remove accents)
    private String normalizeVietnamese(String text) {
        if (text == null) return "";
        return text
            .replace("à", "a").replace("á", "a").replace("ạ", "a").replace("ả", "a").replace("ã", "a")
            .replace("ă", "a").replace("ằ", "a").replace("ắ", "a").replace("ặ", "a").replace("ẳ", "a").replace("ẵ", "a")
            .replace("â", "a").replace("ầ", "a").replace("ấ", "a").replace("ậ", "a").replace("ẩ", "a").replace("ẫ", "a")
            .replace("è", "e").replace("é", "e").replace("ẹ", "e").replace("ẻ", "e").replace("ẽ", "e")
            .replace("ê", "e").replace("ề", "e").replace("ế", "e").replace("ệ", "e").replace("ể", "e").replace("ễ", "e")
            .replace("ì", "i").replace("í", "i").replace("ị", "i").replace("ỉ", "i").replace("ĩ", "i")
            .replace("ò", "o").replace("ó", "o").replace("ọ", "o").replace("ỏ", "o").replace("õ", "o")
            .replace("ô", "o").replace("ồ", "o").replace("ố", "o").replace("ộ", "o").replace("ổ", "o").replace("ỗ", "o")
            .replace("ơ", "o").replace("ờ", "o").replace("ớ", "o").replace("ợ", "o").replace("ở", "o").replace("ỡ", "o")
            .replace("ù", "u").replace("ú", "u").replace("ụ", "u").replace("ủ", "u").replace("ũ", "u")
            .replace("ư", "u").replace("ừ", "u").replace("ứ", "u").replace("ự", "u").replace("ử", "u").replace("ữ", "u")
            .replace("ỳ", "y").replace("ý", "y").replace("ỵ", "y").replace("ỷ", "y").replace("ỹ", "y")
            .replace("đ", "d")
            .replace("À", "A").replace("Á", "A").replace("Ạ", "A").replace("Ả", "A").replace("Ã", "A")
            .replace("Ă", "A").replace("Ằ", "A").replace("Ắ", "A").replace("Ặ", "A").replace("Ẳ", "A").replace("Ẵ", "A")
            .replace("Â", "A").replace("Ầ", "A").replace("Ấ", "A").replace("Ậ", "A").replace("Ẩ", "A").replace("Ẫ", "A")
            .replace("È", "E").replace("É", "E").replace("Ẹ", "E").replace("Ẻ", "E").replace("Ẽ", "E")
            .replace("Ê", "E").replace("Ề", "E").replace("Ế", "E").replace("Ệ", "E").replace("Ể", "E").replace("Ễ", "E")
            .replace("Ì", "I").replace("Í", "I").replace("Ị", "I").replace("Ỉ", "I").replace("Ĩ", "I")
            .replace("Ò", "O").replace("Ó", "O").replace("Ọ", "O").replace("Ỏ", "O").replace("Õ", "O")
            .replace("Ô", "O").replace("Ồ", "O").replace("Ố", "O").replace("Ộ", "O").replace("Ổ", "O").replace("Ỗ", "O")
            .replace("Ơ", "O").replace("Ờ", "O").replace("Ớ", "O").replace("Ợ", "O").replace("Ở", "O").replace("Ỡ", "O")
            .replace("Ù", "U").replace("Ú", "U").replace("Ụ", "U").replace("Ủ", "U").replace("Ũ", "U")
            .replace("Ư", "U").replace("Ừ", "U").replace("Ứ", "U").replace("Ự", "U").replace("Ử", "U").replace("Ữ", "U")
            .replace("Ỳ", "Y").replace("Ý", "Y").replace("Ỵ", "Y").replace("Ỷ", "Y").replace("Ỹ", "Y")
            .replace("Đ", "D");
    }
    
    private void showSearchLayout() {
        // Hide normal sections
        imgBanner.setVisibility(View.GONE);
        layoutTopSection.setVisibility(View.GONE);
        layoutMenSection.setVisibility(View.GONE);
        
        // Show search results
        txtSearchTitle.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.VISIBLE);
    }
    
    private void showNormalLayout() {
        // Show normal sections
        imgBanner.setVisibility(View.VISIBLE);
        layoutTopSection.setVisibility(View.VISIBLE);
        layoutMenSection.setVisibility(View.VISIBLE);
        
        // Hide search results
        txtSearchTitle.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
    }

    private void setupViewAllButtons() {
        Button btnViewAllTop = findViewById(R.id.btnViewAllTop);
        if (btnViewAllTop != null) {
            btnViewAllTop.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                startActivity(intent);
            });
        }

        Button btnViewAllMen = findViewById(R.id.btnViewAllMen);
        if (btnViewAllMen != null) {
            btnViewAllMen.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupBottomNavigation() {
        // Trang chủ - already on this screen, highlight it
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            ImageView imgHome = navHome.findViewById(R.id.imgHomeIcon);
            TextView tvHome = navHome.findViewById(R.id.tvHomeLabel);
            if (imgHome != null) {
                imgHome.setColorFilter(ContextCompat.getColor(this, R.color.teal_700));
            }
            if (tvHome != null) {
                tvHome.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
            }
        }

        // Danh mục
        View navCategories = findViewById(R.id.navCategories);
        if (navCategories != null) {
            navCategories.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                startActivity(intent);
            });
        }

        // Giỏ hàng
        View navCart = findViewById(R.id.navCart);
        if (navCart != null) {
            navCart.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CartActivity.class);
                startActivity(intent);
            });
        }

        // Trợ giúp
        View navHelp = findViewById(R.id.navHelp);
        if (navHelp != null) {
            navHelp.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            });
        }
    }
}
