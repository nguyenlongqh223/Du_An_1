package com.poly.ban_giay_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

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
        
        // Init search functionality
        initSearch();
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
        // Dòng sản phẩm hot trên banner
        topProductList.add(new Product("Nike Air Max Red", "3,200,000₫", "2,690,000₫", R.drawable.giaymau));
        topProductList.add(new Product("Air Jordan 1 Low Gold", "3,500,000₫", "2,990,000₫", R.drawable.giaymau_1));
        topProductList.add(new Product("Air Jordan 1 Mid Triple Black", "3,000,000₫", "2,590,000₫", R.drawable.giay2));
        topProductList.add(new Product("Air Jordan 1 Low Orange", "3,200,000₫", "2,750,000₫", R.drawable.giay3));
        topProductList.add(new Product("Air Jordan 1 High Orange", "3,800,000₫", "3,150,000₫", R.drawable.giay4));
        topProductList.add(new Product("Air Jordan 1 Low UNC Blue", "3,200,000₫", "2,790,000₫", R.drawable.giay5));
        topProductList.add(new Product("Air Jordan 1 Mid Panda", "3,400,000₫", "2,890,000₫", R.drawable.giy6));
        topProductList.add(new Product("Nike Dunk Low Black/White", "2,900,000₫", "2,450,000₫", R.drawable.giy6));
        topProductList.add(new Product("Nike Dunk Low Grey/White", "2,900,000₫", "2,450,000₫", R.drawable.giay7));
        topProductList.add(new Product("Air Force 1 Green Swoosh", "3,000,000₫", "2,590,000₫", R.drawable.giay8));

        topProductAdapter = new ProductAdapter(topProductList);
        rvTop.setAdapter(topProductAdapter);

        // Men's shoes (nếu muốn bỏ thì xoá phần này trong layout)
        rvMen = findViewById(R.id.rvMen);
        rvMen.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        menProductList = new ArrayList<>();
        menProductList.add(new Product("Air Force 1 Beige", "3,000,000₫", "2,590,000₫", R.drawable.giay8));
        menProductList.add(new Product("Air Force 1 Pastel", "3,200,000₫", "2,790,000₫", R.drawable.giay9));
        menProductList.add(new Product("Nike Dunk Low Black/White", "2,900,000₫", "2,450,000₫", R.drawable.giay10));
        menProductList.add(new Product("Nike Dunk Low Grey/White", "2,900,000₫", "2,450,000₫", R.drawable.giay11));
        menProductList.add(new Product("Air Jordan 1 Mid Green", "3,200,000₫", "2,750,000₫", R.drawable.giay12));
        menProductList.add(new Product("Air Force 1 Shadow Pastel", "3,200,000₫", "2,790,000₫", R.drawable.giay13));
        menProductList.add(new Product("Air Jordan 1 Mid Grey/Blue", "3,200,000₫", "2,790,000₫", R.drawable.giay14));
        menProductList.add(new Product("Nike Dunk Low Panda", "2,900,000₫", "2,450,000₫", R.drawable.giay15));

        menProductAdapter = new ProductAdapter(menProductList);
        rvMen.setAdapter(menProductAdapter);
        
        // Combine all products for search
        allProductList = new ArrayList<>();
        allProductList.addAll(topProductList);
        allProductList.addAll(menProductList);
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
}
