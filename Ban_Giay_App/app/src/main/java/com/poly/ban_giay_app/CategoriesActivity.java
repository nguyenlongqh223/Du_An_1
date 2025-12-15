package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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

import com.poly.ban_giay_app.adapter.ProductAdapter;
import com.poly.ban_giay_app.models.Product;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.CategoryResponse;
import com.poly.ban_giay_app.network.model.ProductResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private View navAccount;
    private ImageView imgAccountIcon;
    private TextView tvAccountLabel;

    // RecyclerViews and Adapters
    private RecyclerView rvTopSelling, rvHotTrend, rvMen, rvWomen;
    private ProductAdapter topSellingAdapter, hotTrendAdapter, menAdapter, womenAdapter;
    private List<Product> topSellingList = new ArrayList<>();
    private List<Product> hotTrendList = new ArrayList<>();
    private List<Product> menList = new ArrayList<>();
    private List<Product> womenList = new ArrayList<>();
    private ApiService apiService;
    
    // Dynamic category sections
    private android.widget.LinearLayout layoutCategoriesContainer;
    private Map<String, RecyclerView> categoryRecyclerViews = new HashMap<>();
    private Map<String, ProductAdapter> categoryAdapters = new HashMap<>();
    private Map<String, List<Product>> categoryProductLists = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_categories);

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

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Load categories from API first, then load products
        loadCategoriesFromApi();
        
        // Load top selling and hot trend products
        loadProductsFromApi();
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
                    Intent intent = new Intent(CategoriesActivity.this, AccountActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(CategoriesActivity.this, LoginActivity.class);
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

    private void initProductLists() {
        try {
            // Top selling products
            rvTopSelling = findViewById(R.id.rvTopSelling);
            if (rvTopSelling != null) {
                rvTopSelling.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                topSellingAdapter = new ProductAdapter(topSellingList);
                rvTopSelling.setAdapter(topSellingAdapter);
            }

            // Hot trend products
            rvHotTrend = findViewById(R.id.rvHotTrend);
            if (rvHotTrend != null) {
                rvHotTrend.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                hotTrendAdapter = new ProductAdapter(hotTrendList);
                rvHotTrend.setAdapter(hotTrendAdapter);
            }

            // Men's shoes
            rvMen = findViewById(R.id.rvMen);
            if (rvMen != null) {
                rvMen.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                menAdapter = new ProductAdapter(menList);
                rvMen.setAdapter(menAdapter);
            }

            // Women's shoes
            rvWomen = findViewById(R.id.rvWomen);
            if (rvWomen != null) {
                rvWomen.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                womenAdapter = new ProductAdapter(womenList);
                rvWomen.setAdapter(womenAdapter);
            }
            
            // Khởi tạo container cho các danh mục động
            // Tìm LinearLayout chứa các section (parent của layoutWomen)
            View layoutWomen = findViewById(R.id.layoutWomen);
            if (layoutWomen != null && layoutWomen.getParent() instanceof android.widget.LinearLayout) {
                layoutCategoriesContainer = (android.widget.LinearLayout) layoutWomen.getParent();
            }
        } catch (Exception e) {
            Log.e("CategoriesActivity", "Error initializing product lists", e);
            Toast.makeText(this, "Lỗi khởi tạo danh sách sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadCategoriesFromApi() {
        if (!NetworkUtils.isConnected(this)) {
            Log.e("CategoriesActivity", "No network connection - cannot load categories");
            return;
        }

        Log.d("CategoriesActivity", "Loading categories from API...");
        
        // Load TẤT CẢ danh mục active
        apiService.getCategories().enqueue(new Callback<BaseResponse<List<CategoryResponse>>>() {
            @Override
            public void onResponse(Call<BaseResponse<List<CategoryResponse>>> call, 
                                 Response<BaseResponse<List<CategoryResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    List<CategoryResponse> categories = response.body().getData();
                    Log.d("CategoriesActivity", "All categories loaded: " + (categories != null ? categories.size() : "null"));
                    
                    if (categories != null && !categories.isEmpty()) {
                        // Lọc danh mục active và sắp xếp theo thu_tu
                        List<CategoryResponse> activeCategories = new ArrayList<>();
                        for (CategoryResponse category : categories) {
                            if ("active".equals(category.getTrangThai())) {
                                activeCategories.add(category);
                                Log.d("CategoriesActivity", "Found active category: " + category.getTenDanhMuc());
                            }
                        }
                        
                        // Sắp xếp theo thu_tu (tăng dần)
                        activeCategories.sort((a, b) -> {
                            int orderA = a.getThuTu() != null ? a.getThuTu() : 0;
                            int orderB = b.getThuTu() != null ? b.getThuTu() : 0;
                            return Integer.compare(orderA, orderB);
                        });
                        
                        runOnUiThread(() -> {
                            // Xóa các section cũ (trừ các section có sẵn)
                            clearDynamicCategorySections();
                            
                            // Ẩn section "Giày nữ" mặc định (sẽ hiện lại nếu có danh mục "nu")
                            View layoutWomen = findViewById(R.id.layoutWomen);
                            if (layoutWomen != null) {
                                layoutWomen.setVisibility(View.GONE);
                            }
                            
                            // Tạo section cho từng danh mục
                            for (CategoryResponse category : activeCategories) {
                                String categoryName = category.getTenDanhMuc();
                                Log.d("CategoriesActivity", "Creating section for category: " + categoryName);
                                
                                // Kiểm tra xem có phải là danh mục "nam" hoặc "nu" không
                                boolean isMen = "nam".equalsIgnoreCase(categoryName) || categoryName.toLowerCase().contains("nam");
                                boolean isWomen = "nu".equalsIgnoreCase(categoryName) || "nữ".equalsIgnoreCase(categoryName) || categoryName.toLowerCase().contains("nu") || categoryName.toLowerCase().contains("nữ");
                                
                                if (isMen && rvMen != null) {
                                    // Cập nhật title của section "Giày nam"
                                    View layoutMen = findViewById(R.id.layoutMen);
                                    if (layoutMen != null) {
                                        TextView tvMenTitle = null;
                                        // Tìm TextView trong layoutMen
                                        for (int i = 0; i < ((android.widget.LinearLayout) layoutMen).getChildCount(); i++) {
                                            View child = ((android.widget.LinearLayout) layoutMen).getChildAt(i);
                                            if (child instanceof TextView) {
                                                tvMenTitle = (TextView) child;
                                                break;
                                            }
                                        }
                                        if (tvMenTitle != null) {
                                            tvMenTitle.setText(categoryName);
                                        }
                                    }
                                    loadProductsByCategory(categoryName, categoryName, rvMen, menList, menAdapter);
                                } else if (isWomen && rvWomen != null) {
                                    // Hiển thị section "Giày nữ" và cập nhật title
                                    if (layoutWomen != null) {
                                        layoutWomen.setVisibility(View.VISIBLE);
                                        TextView tvWomenTitle = null;
                                        for (int i = 0; i < ((android.widget.LinearLayout) layoutWomen).getChildCount(); i++) {
                                            View child = ((android.widget.LinearLayout) layoutWomen).getChildAt(i);
                                            if (child instanceof TextView) {
                                                tvWomenTitle = (TextView) child;
                                                break;
                                            }
                                        }
                                        if (tvWomenTitle != null) {
                                            tvWomenTitle.setText(categoryName);
                                        }
                                    }
                                    loadProductsByCategory(categoryName, categoryName, rvWomen, womenList, womenAdapter);
                                } else {
                                    // Các danh mục khác tạo section mới
                                    createCategorySection(categoryName, categoryName);
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<List<CategoryResponse>>> call, Throwable t) {
                Log.e("CategoriesActivity", "Error loading categories: " + t.getMessage());
            }
        });
    }
    
    private void loadProductsByCategory(String categoryName, String displayTitle, 
                                       RecyclerView recyclerView, List<Product> productList, ProductAdapter adapter) {
        if (!NetworkUtils.isConnected(this)) {
            Log.e("CategoriesActivity", "No network connection - cannot load products by category");
            return;
        }

        Log.d("CategoriesActivity", "Loading products for category: " + categoryName);
        
        apiService.getProductsByCategory(categoryName).enqueue(new Callback<List<ProductResponse>>() {
            @Override
            public void onResponse(Call<List<ProductResponse>> call, Response<List<ProductResponse>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        productList.clear();
                        for (ProductResponse productResponse : response.body()) {
                            Product product = convertToProduct(productResponse);
                            if (product != null && product.name != null && !product.name.isEmpty()) {
                                productList.add(product);
                            }
                        }
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                            Log.d("CategoriesActivity", "Products for category '" + categoryName + "' updated: " + productList.size());
                        });
                    }
                } catch (Exception e) {
                    Log.e("CategoriesActivity", "Error loading products for category: " + categoryName, e);
                }
            }

            @Override
            public void onFailure(Call<List<ProductResponse>> call, Throwable t) {
                Log.e("CategoriesActivity", "Failed to load products for category: " + categoryName, t);
            }
        });
    }
    
    private void createCategorySection(String categoryName, String displayTitle) {
        if (layoutCategoriesContainer == null) {
            Log.e("CategoriesActivity", "layoutCategoriesContainer is null, cannot create category section");
            return;
        }
        
        // Tạo LinearLayout cho section mới
        android.widget.LinearLayout sectionLayout = new android.widget.LinearLayout(this);
        sectionLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        sectionLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        sectionLayout.setPadding(
            (int) (12 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (12 * getResources().getDisplayMetrics().density),
            0
        );
        
        // Tạo TextView cho title
        TextView titleTextView = new TextView(this);
        titleTextView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleTextView.setText(displayTitle);
        titleTextView.setTextSize(18);
        titleTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTextView.setTextColor(0xFF000000);
        titleTextView.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        
        // Tạo RecyclerView cho sản phẩm
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (320 * getResources().getDisplayMetrics().density)
        ));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setPadding(0, 0, 0, 0);
        recyclerView.setHorizontalScrollBarEnabled(true);
        recyclerView.setNestedScrollingEnabled(false);
        
        // Tạo adapter và list cho danh mục này
        List<Product> productList = new ArrayList<>();
        ProductAdapter adapter = new ProductAdapter(productList);
        recyclerView.setAdapter(adapter);
        
        // Lưu vào map để quản lý
        categoryRecyclerViews.put(categoryName, recyclerView);
        categoryAdapters.put(categoryName, adapter);
        categoryProductLists.put(categoryName, productList);
        
        // Thêm vào layout
        sectionLayout.addView(titleTextView);
        sectionLayout.addView(recyclerView);
        
        // Thêm section vào container (sau layoutWomen)
        View layoutWomen = findViewById(R.id.layoutWomen);
        if (layoutWomen != null) {
            int insertIndex = layoutCategoriesContainer.indexOfChild(layoutWomen) + 1;
            layoutCategoriesContainer.addView(sectionLayout, insertIndex);
        } else {
            layoutCategoriesContainer.addView(sectionLayout);
        }
        
        // Load sản phẩm cho danh mục này
        loadProductsByCategory(categoryName, displayTitle, recyclerView, productList, adapter);
        
        Log.d("CategoriesActivity", "Created category section for: " + categoryName);
    }
    
    private void clearDynamicCategorySections() {
        // Xóa tất cả các section động (trừ các section có sẵn)
        if (layoutCategoriesContainer != null) {
            List<View> viewsToRemove = new ArrayList<>();
            for (int i = 0; i < layoutCategoriesContainer.getChildCount(); i++) {
                View child = layoutCategoriesContainer.getChildAt(i);
                if (child.getId() != R.id.layoutTopSelling && 
                    child.getId() != R.id.layoutHotTrend && 
                    child.getId() != R.id.layoutMen && 
                    child.getId() != R.id.layoutWomen) {
                    // Kiểm tra xem có phải là section danh mục động không
                    if (child instanceof android.widget.LinearLayout) {
                        android.widget.LinearLayout layout = (android.widget.LinearLayout) child;
                        if (layout.getChildCount() >= 2 && layout.getChildAt(1) instanceof RecyclerView) {
                            RecyclerView rv = (RecyclerView) layout.getChildAt(1);
                            if (categoryRecyclerViews.containsValue(rv)) {
                                viewsToRemove.add(child);
                            }
                        }
                    }
                }
            }
            for (View view : viewsToRemove) {
                layoutCategoriesContainer.removeView(view);
            }
        }
        
        // Clear maps
        categoryRecyclerViews.clear();
        categoryAdapters.clear();
        categoryProductLists.clear();
        
        Log.d("CategoriesActivity", "Cleared dynamic category sections");
    }

    private void loadProductsFromApi() {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load top selling products
        apiService.getBestSellingProducts(10).enqueue(new Callback<List<ProductResponse>>() {
            @Override
            public void onResponse(Call<List<ProductResponse>> call, Response<List<ProductResponse>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && topSellingList != null && topSellingAdapter != null) {
                        topSellingList.clear();
                        for (ProductResponse productResponse : response.body()) {
                            Product product = convertToProduct(productResponse);
                            if (product != null && product.name != null && !product.name.isEmpty()) {
                                topSellingList.add(product);
                            }
                        }
                        runOnUiThread(() -> {
                            if (topSellingAdapter != null) {
                                topSellingAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("CategoriesActivity", "Error loading top selling products", e);
                }
            }

            @Override
            public void onFailure(Call<List<ProductResponse>> call, Throwable t) {
                Log.e("CategoriesActivity", "Failed to load top selling products", t);
            }
        });

        // Load hot trend products (newest products)
        apiService.getNewestProducts(10).enqueue(new Callback<List<ProductResponse>>() {
            @Override
            public void onResponse(Call<List<ProductResponse>> call, Response<List<ProductResponse>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && hotTrendList != null && hotTrendAdapter != null) {
                        hotTrendList.clear();
                        for (ProductResponse productResponse : response.body()) {
                            Product product = convertToProduct(productResponse);
                            if (product != null && product.name != null && !product.name.isEmpty()) {
                                hotTrendList.add(product);
                            }
                        }
                        runOnUiThread(() -> {
                            if (hotTrendAdapter != null) {
                                hotTrendAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("CategoriesActivity", "Error loading hot trend products", e);
                }
            }

            @Override
            public void onFailure(Call<List<ProductResponse>> call, Throwable t) {
                Log.e("CategoriesActivity", "Failed to load hot trend products", t);
            }
        });

        // Không load "nam" và "nu" ở đây nữa - sẽ được load trong loadCategoriesFromApi()
        // Các danh mục sẽ được load động từ API
    }

    private Product convertToProduct(ProductResponse productResponse) {
        if (productResponse == null) {
            return null;
        }

        String name = productResponse.getName();
        String imageUrl = productResponse.getImageUrl();

        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        Integer giaGoc = productResponse.getGiaGoc();
        Integer giaKhuyenMai = productResponse.getGiaKhuyenMai();

        String priceOld = null;
        String priceNew = null;

        if (giaGoc != null && giaGoc > 0) {
            priceOld = formatPrice(giaGoc);
        }

        if (giaKhuyenMai != null && giaKhuyenMai > 0) {
            priceNew = formatPrice(giaKhuyenMai);
        } else if (giaGoc != null && giaGoc > 0) {
            priceNew = formatPrice(giaGoc);
            priceOld = null;
        }

        if (priceNew == null || priceNew.trim().isEmpty()) {
            priceNew = "0₫";
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Product product = new Product(
                name,
                priceOld != null ? priceOld : "",
                priceNew,
                imageUrl
            );
            product.imageUrl = imageUrl;
            return product;
        } else {
            Product product = new Product(
                name,
                priceOld != null ? priceOld : "",
                priceNew,
                R.drawable.giaymau
            );
            product.imageUrl = null;
            return product;
        }
    }

    private String formatPrice(int price) {
        return String.format("%,d₫", price).replace(",", ".");
    }

    private void setupBottomNavigation() {
        try {
            // Trang chủ
            View navHome = findViewById(R.id.navHome);
            if (navHome != null) {
                navHome.setOnClickListener(v -> {
                    Intent intent = new Intent(CategoriesActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            }

            // Danh mục - already on this screen, highlight it
            View navCategories = findViewById(R.id.navCategories);
            if (navCategories != null) {
                // Highlight current screen
                ImageView imgCategories = navCategories.findViewById(R.id.imgCategoriesIcon);
                TextView tvCategories = navCategories.findViewById(R.id.tvCategoriesLabel);
                if (imgCategories != null) {
                    imgCategories.setColorFilter(ContextCompat.getColor(this, R.color.teal_700));
                }
                if (tvCategories != null) {
                    tvCategories.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
                }
            }

            // Giỏ hàng
            View navCart = findViewById(R.id.navCart);
            if (navCart != null) {
                navCart.setOnClickListener(v -> {
                    Intent intent = new Intent(CategoriesActivity.this, CartActivity.class);
                    startActivity(intent);
                });
            }

            // Trợ giúp
            View navHelp = findViewById(R.id.navHelp);
            if (navHelp != null) {
                navHelp.setOnClickListener(v -> {
                    Intent intent = new Intent(CategoriesActivity.this, HelpActivity.class);
                    startActivity(intent);
                });
            }
        } catch (Exception e) {
            Log.e("CategoriesActivity", "Error setting up bottom navigation", e);
        }
    }
}

