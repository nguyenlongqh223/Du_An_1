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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ExecutorService searchExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        // Init account navigation
        initAccountNav();
        updateAccountNavUi();

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
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor to prevent memory leaks
        if (searchExecutor != null && !searchExecutor.isShutdown()) {
            searchExecutor.shutdown();
        }
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
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "No network connection");
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
                                    Log.d("MainActivity", "Processing product: " + productResponse.getName() + 
                                          " - Price: " + productResponse.getPriceNew() + 
                                          " - Image: " + productResponse.getImageUrl());
                                    Product product = convertToProduct(productResponse);
                                    if (product != null && product.name != null && !product.name.isEmpty()) {
                                        topProductList.add(product);
                                        Log.d("MainActivity", "Added product: " + product.name + " - " + product.priceNew);
                                    } else {
                                        Log.w("MainActivity", "Failed to convert product: " + productResponse.getName());
                                    }
                                }
                            }
                            // Process product conversion on background thread if list is large
                            if (topProductList.size() > 50) {
                                new Thread(() -> {
                                    updateAllProductList();
                                    runOnUiThread(() -> {
                                        topProductAdapter.notifyDataSetChanged();
                                        Log.d("MainActivity", "Top products updated: " + topProductList.size());
                                    });
                                }).start();
                            } else {
                                runOnUiThread(() -> {
                                    updateAllProductList();
                                    topProductAdapter.notifyDataSetChanged();
                                    Log.d("MainActivity", "Top products updated: " + topProductList.size());
                                });
                            }
                        } else {
                            Log.w("MainActivity", "Top products list is empty or null");
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Không có sản phẩm bán chạy", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> {
                    String errorMsg = t.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Không thể kết nối đến server. Kiểm tra lại IP và server đang chạy.";
                    }
                    Toast.makeText(MainActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                });
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
                            // Process product conversion on background thread if list is large
                            if (menProductList.size() > 50) {
                                new Thread(() -> {
                                    updateAllProductList();
                                    runOnUiThread(() -> {
                                        menProductAdapter.notifyDataSetChanged();
                                        Log.d("MainActivity", "Men products updated: " + menProductList.size());
                                    });
                                }).start();
                            } else {
                                runOnUiThread(() -> {
                                    updateAllProductList();
                                    menProductAdapter.notifyDataSetChanged();
                                    Log.d("MainActivity", "Men products updated: " + menProductList.size());
                                });
                            }
                        } else {
                            Log.w("MainActivity", "Men products list is empty or null");
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Không có sản phẩm nam", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> {
                    String errorMsg = t.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Không thể kết nối đến server. Kiểm tra lại IP và server đang chạy.";
                    }
                    Toast.makeText(MainActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateAllProductList() {
        synchronized (allProductList) {
            allProductList.clear();
            allProductList.addAll(topProductList);
            allProductList.addAll(menProductList);
        }
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
        String imageUrl = productResponse.getImageUrl();

        Log.d("MainActivity", "Converting product - Name: " + name + 
              ", ImageUrl: " + imageUrl);

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
            Product product = new Product(
                name, 
                priceOld != null ? priceOld : "", 
                priceNew, 
                imageUrl
            );
            // Đảm bảo imageUrl được set
            product.imageUrl = imageUrl;
            Log.d("MainActivity", "Created product with imageUrl: " + imageUrl);
            return product;
        } else {
            // Nếu không có URL ảnh, dùng ảnh mặc định
            Product product = new Product(
                name, 
                priceOld != null ? priceOld : "", 
                priceNew, 
                R.drawable.giaymau
            );
            product.imageUrl = null; // Không có URL, sẽ dùng imageRes
            Log.d("MainActivity", "Created product with default image");
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
        // Perform search on background thread to avoid blocking UI
        searchExecutor.execute(() -> {
            List<Product> results = new ArrayList<>();
            
            // Normalize query for Vietnamese search (bỏ dấu để tìm kiếm linh hoạt hơn)
            String normalizedQuery = normalizeVietnamese(query.toLowerCase());
            String queryLower = query.toLowerCase();
            
            // Create a thread-safe copy of allProductList to avoid concurrent modification
            List<Product> productsToSearch;
            synchronized (allProductList) {
                productsToSearch = new ArrayList<>(allProductList);
            }
            
            // Filter products by name - tìm kiếm cả với dấu và không dấu
            for (Product product : productsToSearch) {
                if (product == null || product.name == null) continue;
                
                String productName = product.name.toLowerCase();
                String normalizedProductName = normalizeVietnamese(productName);
                
                // Tìm kiếm cả text gốc và text đã normalize
                if (productName.contains(queryLower) || 
                    normalizedProductName.contains(normalizedQuery) ||
                    productName.contains(normalizedQuery) ||
                    normalizedProductName.contains(queryLower)) {
                    results.add(product);
                }
            }
            
            // Update UI on main thread
            runOnUiThread(() -> {
                searchResultList.clear();
                searchResultList.addAll(results);
                
                // Update UI
                if (searchResultList.isEmpty()) {
                    txtSearchTitle.setText("Không tìm thấy sản phẩm");
                } else {
                    txtSearchTitle.setText("Kết quả tìm kiếm (" + searchResultList.size() + ")");
                }
                
                searchAdapter.notifyDataSetChanged();
                showSearchLayout();
            });
        });
    }
    
    // Normalize Vietnamese text for better search (remove accents)
    // Optimized version using StringBuilder for better performance
    private String normalizeVietnamese(String text) {
        if (text == null || text.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                // Lowercase
                case 'à': case 'á': case 'ạ': case 'ả': case 'ã': case 'ă': case 'ằ': case 'ắ': case 'ặ': case 'ẳ': case 'ẵ':
                case 'â': case 'ầ': case 'ấ': case 'ậ': case 'ẩ': case 'ẫ':
                    sb.append('a'); break;
                case 'è': case 'é': case 'ẹ': case 'ẻ': case 'ẽ': case 'ê': case 'ề': case 'ế': case 'ệ': case 'ể': case 'ễ':
                    sb.append('e'); break;
                case 'ì': case 'í': case 'ị': case 'ỉ': case 'ĩ':
                    sb.append('i'); break;
                case 'ò': case 'ó': case 'ọ': case 'ỏ': case 'õ': case 'ô': case 'ồ': case 'ố': case 'ộ': case 'ổ': case 'ỗ':
                case 'ơ': case 'ờ': case 'ớ': case 'ợ': case 'ở': case 'ỡ':
                    sb.append('o'); break;
                case 'ù': case 'ú': case 'ụ': case 'ủ': case 'ũ': case 'ư': case 'ừ': case 'ứ': case 'ự': case 'ử': case 'ữ':
                    sb.append('u'); break;
                case 'ỳ': case 'ý': case 'ỵ': case 'ỷ': case 'ỹ':
                    sb.append('y'); break;
                case 'đ':
                    sb.append('d'); break;
                // Uppercase
                case 'À': case 'Á': case 'Ạ': case 'Ả': case 'Ã': case 'Ă': case 'Ằ': case 'Ắ': case 'Ặ': case 'Ẳ': case 'Ẵ':
                case 'Â': case 'Ầ': case 'Ấ': case 'Ậ': case 'Ẩ': case 'Ẫ':
                    sb.append('A'); break;
                case 'È': case 'É': case 'Ẹ': case 'Ẻ': case 'Ẽ': case 'Ê': case 'Ề': case 'Ế': case 'Ệ': case 'Ể': case 'Ễ':
                    sb.append('E'); break;
                case 'Ì': case 'Í': case 'Ị': case 'Ỉ': case 'Ĩ':
                    sb.append('I'); break;
                case 'Ò': case 'Ó': case 'Ọ': case 'Ỏ': case 'Õ': case 'Ô': case 'Ồ': case 'Ố': case 'Ộ': case 'Ổ': case 'Ỗ':
                case 'Ơ': case 'Ờ': case 'Ớ': case 'Ợ': case 'Ở': case 'Ỡ':
                    sb.append('O'); break;
                case 'Ù': case 'Ú': case 'Ụ': case 'Ủ': case 'Ũ': case 'Ư': case 'Ừ': case 'Ứ': case 'Ự': case 'Ử': case 'Ữ':
                    sb.append('U'); break;
                case 'Ỳ': case 'Ý': case 'Ỵ': case 'Ỷ': case 'Ỹ':
                    sb.append('Y'); break;
                case 'Đ':
                    sb.append('D'); break;
                default:
                    sb.append(c); break;
            }
        }
        return sb.toString();
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
                // TODO: Navigate to cart screen when available
                Toast.makeText(this, "Tính năng giỏ hàng đang phát triển", Toast.LENGTH_SHORT).show();
            });
        }

        // Trợ giúp
        View navHelp = findViewById(R.id.navHelp);
        if (navHelp != null) {
            navHelp.setOnClickListener(v -> {
                // TODO: Navigate to help screen when available
                Toast.makeText(this, "Tính năng trợ giúp đang phát triển", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
