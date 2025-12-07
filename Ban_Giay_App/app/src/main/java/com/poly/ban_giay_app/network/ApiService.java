package com.poly.ban_giay_app.network;

import com.poly.ban_giay_app.network.model.AuthResponse;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.ProductListResponse;
import com.poly.ban_giay_app.network.model.ProductResponse;
import com.poly.ban_giay_app.network.model.UserResponse;
import com.poly.ban_giay_app.network.request.ForgotPasswordRequest;
import com.poly.ban_giay_app.network.request.LoginRequest;
import com.poly.ban_giay_app.network.request.PaymentRequest;
import com.poly.ban_giay_app.network.request.ProductRequest;
import com.poly.ban_giay_app.network.request.RegisterRequest;
import com.poly.ban_giay_app.network.request.StockUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Khai báo các API dùng với Retrofit.
 * Base URL: http://YOUR_IP:3000/api/
 */
public interface ApiService {

    // ==================== AUTH APIs ====================
    // POST http://YOUR_IP:3000/api/auth/login
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // POST http://YOUR_IP:3000/api/auth/register
    @POST("auth/register")
    Call<BaseResponse<UserResponse>> register(@Body RegisterRequest request);

    // POST http://YOUR_IP:3000/api/auth/forgot-password
    @POST("auth/forgot-password")
    Call<BaseResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

    // POST http://YOUR_IP:3000/api/auth/verify-otp-reset-password
    @POST("auth/verify-otp-reset-password")
    Call<BaseResponse<Void>> verifyOtpAndResetPassword(@Body com.poly.ban_giay_app.network.request.VerifyOtpRequest request);

    // ==================== PRODUCT APIs ====================
    
    // GET http://YOUR_IP:3000/api/product
    // Lấy tất cả sản phẩm (có phân trang và lọc)
    @GET("product")
    Call<ProductListResponse> getAllProducts(
            @Query("page") Integer page,
            @Query("limit") Integer limit,
            @Query("danh_muc") String danhMuc,
            @Query("thuong_hieu") String thuongHieu,
            @Query("min_price") Integer minPrice,
            @Query("max_price") Integer maxPrice,
            @Query("search") String search,
            @Query("sort_by") String sortBy,
            @Query("sort_order") String sortOrder
    );

    // GET http://YOUR_IP:3000/api/product/:id
    // Lấy sản phẩm theo ID
    @GET("product/{id}")
    Call<BaseResponse<ProductResponse>> getProductById(@Path("id") String id);

    // GET http://YOUR_IP:3000/api/product/best-selling
    // Lấy sản phẩm bán chạy (trả về array trực tiếp)
    @GET("product/best-selling")
    Call<List<ProductResponse>> getBestSellingProducts(@Query("limit") Integer limit);

    // GET http://YOUR_IP:3000/api/product/newest
    // Lấy sản phẩm mới nhất (trả về array trực tiếp)
    @GET("product/newest")
    Call<List<ProductResponse>> getNewestProducts(@Query("limit") Integer limit);

    // GET http://YOUR_IP:3000/api/product/category/:danh_muc
    // Lấy sản phẩm theo danh mục (trả về array trực tiếp)
    @GET("product/category/{danh_muc}")
    Call<List<ProductResponse>> getProductsByCategory(@Path("danh_muc") String danhMuc);

    // POST http://YOUR_IP:3000/api/product
    // Tạo sản phẩm mới (Admin)
    @POST("product")
    Call<BaseResponse<ProductResponse>> createProduct(@Body ProductRequest request);

    // PUT http://YOUR_IP:3000/api/product/:id
    // Cập nhật sản phẩm (Admin)
    @PUT("product/{id}")
    Call<BaseResponse<ProductResponse>> updateProduct(@Path("id") String id, @Body ProductRequest request);

    // PUT http://YOUR_IP:3000/api/product/:id/stock
    // Cập nhật số lượng tồn kho (Admin)
    @PUT("product/{id}/stock")
    Call<BaseResponse<ProductResponse>> updateStock(@Path("id") String id, @Body StockUpdateRequest request);

    // DELETE http://YOUR_IP:3000/api/product/:id
    // Xóa sản phẩm (Admin)
    @DELETE("product/{id}")
    Call<BaseResponse<Void>> deleteProduct(@Path("id") String id);

    // ==================== LEGACY APIs (Giữ lại để tương thích) ====================
    // Lấy danh sách sản phẩm bán chạy (legacy)
    @GET("products/top-selling")
    Call<BaseResponse<List<ProductResponse>>> getTopSellingProducts();

    // Lấy danh sách giày nam (legacy)
    @GET("products/men")
    Call<BaseResponse<List<ProductResponse>>> getMenProducts();

    // Lấy tất cả sản phẩm (legacy)
    @GET("products")
    Call<BaseResponse<List<ProductResponse>>> getAllProducts();

    // ==================== PAYMENT APIs ====================
    // POST http://YOUR_IP:3000/api/payment
    // Tạo payment mới
    @POST("payment")
    Call<BaseResponse<Object>> createPayment(@Body PaymentRequest request);
}
