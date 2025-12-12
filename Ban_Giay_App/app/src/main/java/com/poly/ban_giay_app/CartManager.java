package com.poly.ban_giay_app;

import android.content.Context;
import android.util.Log;

import com.poly.ban_giay_app.models.CartItem;
import com.poly.ban_giay_app.models.Product;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.NetworkUtils;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.request.CartRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;
    private Context context;

    // Callback interface để thông báo kết quả
    public interface CartCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    private CartManager() {
        cartItems = new ArrayList<>();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
        // Khởi tạo ApiClient nếu chưa được khởi tạo
        if (context != null) {
            ApiClient.init(context);
        }
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void addToCart(Product product, String size, int quantity) {
        addToCart(product, size, quantity, null);
    }

    public void addToCart(Product product, String size, int quantity, CartCallback callback) {
        try {
            // Debug: Log thông tin sản phẩm khi add vào cart
            Log.d("CartManager", "=== Adding product to cart ===");
            Log.d("CartManager", "Product name: " + product.name);
            Log.d("CartManager", "Product ID: " + product.id);
            Log.d("CartManager", "Product imageUrl: " + product.imageUrl);
            Log.d("CartManager", "Product imageRes: " + product.imageRes);
            Log.d("CartManager", "Size: " + size + ", Quantity: " + quantity);
            
            // Validate input
            if (product == null) {
                Log.e("CartManager", "❌ Product is null!");
                if (callback != null) {
                    callback.onError("Sản phẩm không hợp lệ");
                }
                return;
            }
            
            if (size == null || size.isEmpty()) {
                Log.e("CartManager", "❌ Size is null or empty!");
                if (callback != null) {
                    callback.onError("Vui lòng chọn kích thước");
                }
                return;
            }
            
            if (quantity <= 0) {
                Log.e("CartManager", "❌ Quantity is invalid: " + quantity);
                if (callback != null) {
                    callback.onError("Số lượng không hợp lệ");
                }
                return;
            }
            
            // Lưu lên server VÀ thêm vào local cart để hiển thị ngay
            // Server sẽ tự động xử lý việc tăng số lượng nếu sản phẩm đã có
            // Thêm vào local cart trước để UI hiển thị ngay
            addToLocalCart(product, size, quantity);
            // Sau đó lưu lên server
            saveCartToServer(product, size, quantity, callback);
        } catch (Exception e) {
            Log.e("CartManager", "❌ Exception in addToCart", e);
            if (callback != null) {
                callback.onError("Lỗi khi thêm vào giỏ hàng: " + e.getMessage());
            }
        }
    }

    private void saveCartToServer(Product product, String size, int quantity, CartCallback callback) {
        if (context == null) {
            String error = "Không thể kết nối đến server. Context chưa được khởi tạo.";
            Log.e("CartManager", "❌ " + error);
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }

        SessionManager sessionManager = new SessionManager(context);
        if (!sessionManager.isLoggedIn()) {
            String error = "Vui lòng đăng nhập để đồng bộ với server";
            Log.w("CartManager", error);
            if (callback != null) {
                callback.onSuccess("Đã thêm vào giỏ hàng thành công! (chỉ lưu cục bộ)");
            }
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            String error = "Không tìm thấy thông tin người dùng";
            Log.w("CartManager", error);
            if (callback != null) {
                callback.onSuccess("Đã thêm vào giỏ hàng thành công! (chỉ lưu cục bộ)");
            }
            return;
        }

        if (product.id == null || product.id.isEmpty()) {
            String warning = "Sản phẩm này không có ID từ server, chỉ lưu cục bộ. Vui lòng chọn sản phẩm từ danh sách chính thức để đồng bộ với server.";
            Log.w("CartManager", warning);
            Log.w("CartManager", "Product name: " + product.name + ", Product ID: " + product.id);
            // Vẫn thông báo thành công cho user vì đã lưu vào local cart
            // Nhưng cảnh báo rằng không lưu được lên server
            if (callback != null) {
                callback.onSuccess("Đã thêm vào giỏ hàng thành công! (chỉ lưu cục bộ)");
            }
            return;
        }

        if (!NetworkUtils.isConnected(context)) {
            String error = "Không có kết nối mạng";
            Log.w("CartManager", error);
            if (callback != null) {
                callback.onSuccess("Đã thêm vào giỏ hàng thành công! (chỉ lưu cục bộ, chưa đồng bộ với server)");
            }
            return;
        }

        // Đảm bảo ApiClient đã được init với context
        try {
            ApiClient.init(context);
            ApiService apiService = ApiClient.getApiService();
            
            if (apiService == null) {
                String error = "Không thể khởi tạo dịch vụ API";
                Log.e("CartManager", "❌ " + error);
                if (callback != null) {
                    callback.onError(error);
                }
                return;
            }
            
            CartRequest request = new CartRequest(userId, product.id, size, quantity);

            Log.d("CartManager", "=== SAVING CART TO SERVER ===");
            Log.d("CartManager", "UserId: " + userId);
            Log.d("CartManager", "ProductId: " + product.id);
            Log.d("CartManager", "ProductName: " + product.name);
            Log.d("CartManager", "Size: " + size);
            Log.d("CartManager", "Quantity: " + quantity);
            Log.d("CartManager", "Request object: " + request.getUserId() + ", " + request.getProductId() + ", " + request.getSize() + ", " + request.getQuantity());
            
            // Log request body dưới dạng JSON để debug
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String requestJson = gson.toJson(request);
                Log.d("CartManager", "Request JSON: " + requestJson);
            } catch (Exception e) {
                Log.e("CartManager", "Error serializing request to JSON", e);
            }

            apiService.addToCart(request).enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                try {
                    Log.d("CartManager", "=== API RESPONSE ===");
                    Log.d("CartManager", "Response code: " + response.code());
                    Log.d("CartManager", "Response isSuccessful: " + response.isSuccessful());
                    Log.d("CartManager", "Response body: " + (response.body() != null ? "not null" : "null"));
                    
                    // Xử lý response thành công (200-299)
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            BaseResponse<Void> body = response.body();
                            Log.d("CartManager", "Response success: " + body.getSuccess());
                            Log.d("CartManager", "Response message: " + body.getMessage());
                            
                            if (body.getSuccess()) {
                                // Luôn hiển thị thông báo thành công rõ ràng
                                String message = body.getMessage() != null && !body.getMessage().isEmpty() 
                                    ? body.getMessage() 
                                    : "Đã thêm vào giỏ hàng thành công!";
                                Log.d("CartManager", "✅ Cart saved to server successfully: " + message);
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                            } else {
                                String error = body.getMessage() != null && !body.getMessage().isEmpty() 
                                    ? body.getMessage() 
                                    : "Không thể thêm vào giỏ hàng";
                                Log.e("CartManager", "❌ Failed to save cart: " + error);
                                if (callback != null) {
                                    callback.onError(error);
                                }
                            }
                        } else {
                            // Response thành công nhưng body null - có thể server trả về 200 với body rỗng
                            // Trong trường hợp này, coi như thành công
                            Log.w("CartManager", "⚠️ Response successful but body is null. Assuming success.");
                            String message = "Đã thêm vào giỏ hàng thành công!";
                            if (callback != null) {
                                callback.onSuccess(message);
                            }
                        }
                    } else {
                        // Xử lý lỗi chi tiết hơn
                        String error = NetworkUtils.extractErrorMessage(response);
                        
                        Log.e("CartManager", "❌ Failed to save cart to server. Code: " + response.code());
                        Log.e("CartManager", "Error message: " + error);
                        
                        // Nếu là lỗi 404, có thể là sản phẩm không tồn tại
                        if (response.code() == 404) {
                            error = "Sản phẩm không tồn tại trong hệ thống. Vui lòng thử lại hoặc chọn sản phẩm khác.";
                            Log.e("CartManager", "❌ Product not found (404). Product ID: " + product.id);
                        }
                        
                        // Đọc error body để lấy thông tin chi tiết
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e("CartManager", "Error body: " + errorBody);
                                
                                // Cố gắng parse JSON để lấy message từ backend
                                try {
                                    org.json.JSONObject json = new org.json.JSONObject(errorBody);
                                    if (json.has("message")) {
                                        String serverMessage = json.getString("message");
                                        if (serverMessage != null && !serverMessage.isEmpty()) {
                                            error = serverMessage;
                                            Log.e("CartManager", "Server error message: " + serverMessage);
                                        }
                                    } else if (json.has("error")) {
                                        String serverError = json.getString("error");
                                        if (serverError != null && !serverError.isEmpty()) {
                                            error = serverError;
                                            Log.e("CartManager", "Server error: " + serverError);
                                        }
                                    }
                                } catch (Exception e) {
                                    // Ignore JSON parse error, sử dụng error mặc định
                                    Log.w("CartManager", "Cannot parse error JSON", e);
                                }
                            } catch (Exception e) {
                                Log.e("CartManager", "Cannot read error body", e);
                            }
                        }
                        
                        // Kiểm tra nếu lỗi liên quan đến size không có sẵn
                        if (error != null && (error.toLowerCase().contains("kích thước") || 
                            error.toLowerCase().contains("size") || 
                            error.toLowerCase().contains("không có sẵn") ||
                            error.toLowerCase().contains("not available"))) {
                            // Giữ nguyên thông báo từ server vì nó đã rõ ràng
                            Log.e("CartManager", "❌ Size not available error: " + error);
                        }
                        
                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                } catch (Exception e) {
                    Log.e("CartManager", "❌ Exception processing response", e);
                    String error = "Lỗi khi xử lý phản hồi từ server: " + e.getMessage();
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                try {
                    String error = t.getMessage() != null ? t.getMessage() : "Không thể kết nối đến server";
                    Log.e("CartManager", "❌ Error saving cart to server: " + error);
                    if (t.getCause() != null) {
                        Log.e("CartManager", "Cause: " + t.getCause().getMessage());
                    }
                    t.printStackTrace();
                    
                    // Xử lý các loại lỗi khác nhau
                    if (t instanceof java.net.UnknownHostException || 
                        t instanceof java.net.ConnectException) {
                        error = "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
                    } else if (t instanceof java.net.SocketTimeoutException) {
                        error = "Kết nối quá thời gian. Vui lòng thử lại.";
                    } else if (t instanceof java.io.IOException) {
                        error = "Lỗi kết nối mạng. Vui lòng thử lại.";
                    }
                    
                    if (callback != null) {
                        callback.onError(error);
                    }
                } catch (Exception e) {
                    Log.e("CartManager", "❌ Exception in onFailure", e);
                    if (callback != null) {
                        callback.onError("Lỗi không xác định khi thêm vào giỏ hàng");
                    }
                }
            }
        });
        } catch (Exception e) {
            Log.e("CartManager", "❌ Exception in saveCartToServer", e);
            if (callback != null) {
                callback.onError("Lỗi khi lưu giỏ hàng: " + e.getMessage());
            }
        }
    }

    private void updateCartOnServer(Product product, String size, int quantity, CartCallback callback) {
        // TODO: Implement update cart item on server
        // For now, just save again (backend should handle update if item exists)
        saveCartToServer(product, size, quantity, callback);
    }

    /**
     * Thêm sản phẩm vào local cart (để hiển thị ngay trong UI)
     * Nếu đã có sản phẩm cùng ID và size, sẽ tăng số lượng thay vì tạo item mới
     */
    private void addToLocalCart(Product product, String size, int quantity) {
        try {
            // Kiểm tra xem đã có item nào với cùng product ID và size chưa
            if (product != null && product.id != null && !product.id.isEmpty()) {
                for (CartItem existingItem : cartItems) {
                    if (existingItem.product != null && 
                        existingItem.product.id != null && 
                        existingItem.product.id.equals(product.id) &&
                        existingItem.size != null &&
                        existingItem.size.equals(size)) {
                        // Tìm thấy item cùng sản phẩm và size -> tăng số lượng
                        existingItem.quantity += quantity;
                        Log.d("CartManager", "✅ Merged with existing item. New quantity: " + existingItem.quantity + 
                              " (Product: " + product.name + ", Size: " + size + ")");
                        return;
                    }
                }
            }
            
            // Không tìm thấy item cùng sản phẩm và size -> tạo item mới
            CartItem newItem = new CartItem(product, size, quantity);
            cartItems.add(newItem);
            Log.d("CartManager", "✅ Added NEW item to local cart. Total items: " + cartItems.size() + 
                  " (Product: " + product.name + ", Size: " + size + ", Quantity: " + quantity + ")");
        } catch (Exception e) {
            Log.e("CartManager", "❌ Error adding to local cart", e);
        }
    }

    public void removeFromCart(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.remove(position);
        }
    }

    /**
     * Xóa item khỏi giỏ hàng trên server (nếu đã đăng nhập)
     */
    public void removeItemFromServer(CartItem item, CartCallback callback) {
        try {
            if (item == null || item.product == null) {
                if (callback != null) {
                    callback.onError("Sản phẩm không hợp lệ");
                }
                return;
            }

            if (context == null) {
                if (callback != null) {
                    callback.onError("Không thể xóa trên server: context chưa được khởi tạo");
                }
                return;
            }

            SessionManager sessionManager = new SessionManager(context);
            if (!sessionManager.isLoggedIn()) {
                // Người dùng chưa đăng nhập, chỉ xóa local
                if (callback != null) {
                    callback.onSuccess("Đã xóa khỏi giỏ hàng (cục bộ)");
                }
                return;
            }

            String userId = sessionManager.getUserId();
            if (userId == null || userId.isEmpty()) {
                if (callback != null) {
                    callback.onError("Không tìm thấy thông tin người dùng");
                }
                return;
            }

            if (!NetworkUtils.isConnected(context)) {
                if (callback != null) {
                    callback.onError("Không có kết nối mạng. Sản phẩm đã được xóa cục bộ, vui lòng thử lại khi có mạng để đồng bộ server.");
                }
                return;
            }

            ApiClient.init(context);
            ApiService apiService = ApiClient.getApiService();
            if (apiService == null) {
                if (callback != null) {
                    callback.onError("Không thể khởi tạo dịch vụ API");
                }
                return;
            }

            // Backend yêu cầu user_id + item_id khi xóa
            CartRequest request = new CartRequest(userId, item.itemId);
            apiService.removeFromCart(request).enqueue(new Callback<BaseResponse<Void>>() {
                @Override
                public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().getSuccess())) {
                            if (callback != null) {
                                callback.onSuccess(response.body().getMessage() != null ? response.body().getMessage() : "Đã xóa sản phẩm khỏi giỏ hàng");
                            }
                        } else {
                            String error = NetworkUtils.extractErrorMessage(response);
                            if (callback != null) {
                                callback.onError(error != null ? error : "Không thể xóa sản phẩm khỏi giỏ hàng");
                            }
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onError("Lỗi khi xử lý phản hồi: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                    if (callback != null) {
                        callback.onError(t.getMessage() != null ? t.getMessage() : "Không thể kết nối đến server");
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Lỗi khi xóa khỏi giỏ hàng: " + e.getMessage());
            }
        }
    }

    public void updateQuantity(int position, int quantity) {
        if (position >= 0 && position < cartItems.size()) {
            if (quantity > 0) {
                cartItems.get(position).quantity = quantity;
            } else {
                cartItems.remove(position);
            }
        }
    }

    public void setItemSelected(int position, boolean selected) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.get(position).isSelected = selected;
        }
    }

    public void selectAll(boolean selectAll) {
        for (CartItem item : cartItems) {
            item.isSelected = selectAll;
        }
    }

    public boolean areAllSelected() {
        if (cartItems.isEmpty()) return false;
        for (CartItem item : cartItems) {
            if (!item.isSelected) return false;
        }
        return true;
    }

    public long getTotalPrice() {
        long total = 0;
        // Nếu không có item nào được selected, tính tổng tất cả items
        boolean hasSelected = false;
        for (CartItem item : cartItems) {
            if (item.isSelected) {
                hasSelected = true;
                break;
            }
        }
        
        for (CartItem item : cartItems) {
            // Nếu có item được selected, chỉ tính các item được selected
            // Nếu không có item nào được selected, tính tất cả
            if (!hasSelected || item.isSelected) {
                total += item.getTotalPrice();
            }
        }
        return total;
    }

    public int getSelectedCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            if (item.isSelected) {
                count++;
            }
        }
        return count;
    }

    public void clearCart() {
        cartItems.clear();
    }

    public List<CartItem> getSelectedItems() {
        List<CartItem> selected = new ArrayList<>();
        for (CartItem item : cartItems) {
            if (item.isSelected) {
                selected.add(item);
            }
        }
        return selected;
    }

    public void removeSelectedItems() {
        cartItems.removeIf(item -> item.isSelected);
    }
}

