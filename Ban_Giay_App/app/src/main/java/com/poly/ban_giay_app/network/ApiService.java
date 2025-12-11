package com.poly.ban_giay_app.network;

import com.poly.ban_giay_app.network.model.AuthResponse;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.NotificationListResponse;
import com.poly.ban_giay_app.network.model.NotificationResponse;
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
import retrofit2.http.HTTP;
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
            @Query("sort_by") String sortBy
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

    // ==================== CART APIs ====================
    // POST http://YOUR_IP:3000/api/cart
    // Thêm sản phẩm vào giỏ hàng
    @POST("cart")
    Call<BaseResponse<Void>> addToCart(@Body com.poly.ban_giay_app.network.request.CartRequest request);

    // GET http://YOUR_IP:3000/api/cart?user_id=xxx
    // Lấy giỏ hàng của user
    @GET("cart")
    Call<BaseResponse<com.poly.ban_giay_app.network.model.CartResponse>> getCart(@Query("user_id") String userId);

    // PUT http://YOUR_IP:3000/api/cart/item
    // Cập nhật số lượng sản phẩm trong giỏ hàng
    @PUT("cart/item")
    Call<BaseResponse<Void>> updateCartItem(@Body com.poly.ban_giay_app.network.request.CartRequest request);

    // DELETE http://YOUR_IP:3000/api/cart/item
    // Xóa sản phẩm khỏi giỏ hàng (DELETE có body cần dùng @HTTP(hasBody = true))
    @HTTP(method = "DELETE", path = "cart/item", hasBody = true)
    Call<BaseResponse<Void>> removeFromCart(@Body com.poly.ban_giay_app.network.request.CartRequest request);

    // ==================== ORDER APIs ====================
    // POST http://YOUR_IP:3000/api/order
    // Tạo đơn hàng mới
    @POST("order")
    Call<BaseResponse<com.poly.ban_giay_app.network.model.OrderResponse>> createOrder(@Body com.poly.ban_giay_app.network.request.OrderRequest request);

    // GET http://YOUR_IP:3000/api/order?user_id=xxx&trang_thai=xxx
    // Lấy danh sách đơn hàng của user (có thể filter theo trạng thái)
    @GET("order")
    Call<BaseResponse<List<com.poly.ban_giay_app.network.model.OrderResponse>>> getOrders(
            @Query("user_id") String userId,
            @Query("trang_thai") String trangThai
    );

    // GET http://YOUR_IP:3000/api/order/{orderId}
    // Lấy chi tiết đơn hàng
    @GET("order/{orderId}")
    Call<BaseResponse<com.poly.ban_giay_app.network.model.OrderResponse>> getOrderById(@Path("orderId") String orderId);

    // PUT http://YOUR_IP:3000/api/order/{orderId}/status
    // Cập nhật trạng thái đơn hàng
    @PUT("order/{orderId}/status")
    Call<BaseResponse<com.poly.ban_giay_app.network.model.OrderResponse>> updateOrderStatus(
            @Path("orderId") String orderId,
            @Body com.poly.ban_giay_app.network.request.UpdateOrderStatusRequest request
    );

    // PUT http://YOUR_IP:3000/api/order/{orderId}/cancel
    // Hủy đơn hàng
    @PUT("order/{orderId}/cancel")
    Call<BaseResponse<com.poly.ban_giay_app.network.model.OrderResponse>> cancelOrder(@Path("orderId") String orderId);

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

    // ==================== NOTIFICATION APIs ====================
    // GET http://YOUR_IP:3000/api/notification?user_id=xxx&da_doc=false
    // Lấy danh sách thông báo của user
    @GET("notification")
    Call<BaseResponse<NotificationListResponse>> getNotifications(
            @Query("user_id") String userId,
            @Query("da_doc") Boolean daDoc
    );

    // GET http://YOUR_IP:3000/api/notification/{user_id}
    @GET("notification/{user_id}")
    Call<BaseResponse<NotificationListResponse>> getNotificationsByUser(
            @Path("user_id") String userId
    );

    // PUT http://YOUR_IP:3000/api/notification/{id}/read
    // Đánh dấu thông báo đã đọc
    @PUT("notification/{id}/read")
    Call<BaseResponse<NotificationResponse>> markNotificationAsRead(@Path("id") String notificationId);

    // PUT http://YOUR_IP:3000/api/notification/read-all?user_id=xxx
    // Đánh dấu tất cả thông báo của user đã đọc
    @PUT("notification/read-all")
    Call<BaseResponse<Void>> markAllNotificationsAsRead(@Query("user_id") String userId);

    // ==================== PAYMENT APIs ====================
    // POST http://YOUR_IP:3000/api/payment
    // Tạo payment mới
    @POST("payment")
    Call<BaseResponse<Object>> createPayment(@Body PaymentRequest request);
}
