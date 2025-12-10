package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Model để map với dữ liệu giỏ hàng từ MongoDB
 * Backend trả về Cart có user_id và items (array)
 */
public class CartResponse {
    @SerializedName("_id")
    private String id;

    // user_id có thể là string ID hoặc object (ObjectId từ MongoDB)
    @SerializedName("user_id")
    private Object userIdRaw;

    @SerializedName("items")
    private List<CartItemResponse> items;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    /**
     * Model cho từng item trong giỏ hàng
     */
    public static class CartItemResponse {
        @SerializedName("_id")
        private String id;

        // Khi chưa populate: san_pham_id là string ID
        // Khi đã populate: san_pham_id là object Product (sẽ được deserialize thành Map)
        // Gson sẽ deserialize object thành Map<String, Object>
        @SerializedName("san_pham_id")
        private Object sanPhamIdRaw;
        
        // Log để debug
        private void logSanPhamIdType() {
            if (sanPhamIdRaw != null) {
                android.util.Log.d("CartItemResponse", "sanPhamIdRaw type: " + sanPhamIdRaw.getClass().getName());
                if (sanPhamIdRaw instanceof Map) {
                    android.util.Log.d("CartItemResponse", "sanPhamIdRaw is Map with keys: " + ((Map<?, ?>) sanPhamIdRaw).keySet());
                }
            } else {
                android.util.Log.d("CartItemResponse", "sanPhamIdRaw is null");
            }
        }

        @SerializedName("so_luong")
        private Integer soLuong;

        @SerializedName("kich_thuoc")
        private String kichThuoc;

        @SerializedName("gia")
        private Integer gia;

        // Cache để lưu ProductResponse khi đã deserialize
        private ProductResponse productCache;

        public String getSanPhamId() {
            if (sanPhamIdRaw instanceof String) {
                return (String) sanPhamIdRaw;
            } else if (sanPhamIdRaw instanceof ProductResponse) {
                ProductResponse pr = (ProductResponse) sanPhamIdRaw;
                return pr.getId();
            }
            return null;
        }

        public void setSanPhamId(String sanPhamId) {
            this.sanPhamIdRaw = sanPhamId;
        }

        public Integer getSoLuong() {
            return soLuong;
        }

        public void setSoLuong(Integer soLuong) {
            this.soLuong = soLuong;
        }

        public String getKichThuoc() {
            return kichThuoc;
        }

        public void setKichThuoc(String kichThuoc) {
            this.kichThuoc = kichThuoc;
        }

        public Integer getGia() {
            return gia;
        }

        public void setGia(Integer gia) {
            this.gia = gia;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        // Getter cho product khi được populate
        public ProductResponse getProduct() {
            // Log để debug
            logSanPhamIdType();
            
            // Nếu đã cache, return cache
            if (productCache != null) {
                android.util.Log.d("CartItemResponse", "Returning cached product");
                return productCache;
            }
            
            // Nếu san_pham_id là ProductResponse (đã populate), return nó
            if (sanPhamIdRaw instanceof ProductResponse) {
                android.util.Log.d("CartItemResponse", "sanPhamIdRaw is already ProductResponse");
                productCache = (ProductResponse) sanPhamIdRaw;
                return productCache;
            }
            
            // Nếu san_pham_id là Map (từ JSON object), convert sang ProductResponse
            if (sanPhamIdRaw instanceof Map) {
                android.util.Log.d("CartItemResponse", "Converting Map to ProductResponse");
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> productMap = (Map<String, Object>) sanPhamIdRaw;
                    
                    // Tạo ProductResponse từ Map
                    ProductResponse product = new ProductResponse();
                    
                    // Set các field từ Map
                    // Xử lý _id - có thể là ObjectId object hoặc string
                    if (productMap.containsKey("_id")) {
                        Object idObj = productMap.get("_id");
                        if (idObj != null) {
                            // Nếu _id là Map (ObjectId từ MongoDB), lấy $oid hoặc toString
                            if (idObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> idMap = (Map<String, Object>) idObj;
                                if (idMap.containsKey("$oid")) {
                                    product.setId(idMap.get("$oid").toString());
                                } else {
                                    product.setId(idObj.toString());
                                }
                            } else {
                                product.setId(idObj.toString());
                            }
                        }
                    }
                    if (productMap.containsKey("ten_san_pham")) {
                        product.setName(productMap.get("ten_san_pham").toString());
                    }
                    if (productMap.containsKey("gia_goc")) {
                        Object giaGoc = productMap.get("gia_goc");
                        if (giaGoc instanceof Number) {
                            product.setGiaGoc(((Number) giaGoc).intValue());
                        }
                    }
                    if (productMap.containsKey("gia_khuyen_mai")) {
                        Object giaKhuyenMai = productMap.get("gia_khuyen_mai");
                        if (giaKhuyenMai instanceof Number) {
                            product.setGiaKhuyenMai(((Number) giaKhuyenMai).intValue());
                        }
                    }
                    if (productMap.containsKey("hinh_anh")) {
                        product.setHinhAnh(productMap.get("hinh_anh").toString());
                    }
                    if (productMap.containsKey("mo_ta")) {
                        product.setMoTa(productMap.get("mo_ta").toString());
                    }
                    if (productMap.containsKey("thuong_hieu")) {
                        product.setThuongHieu(productMap.get("thuong_hieu").toString());
                    }
                    if (productMap.containsKey("danh_muc")) {
                        product.setDanhMuc(productMap.get("danh_muc").toString());
                    }
                    if (productMap.containsKey("danh_gia")) {
                        Object danhGia = productMap.get("danh_gia");
                        if (danhGia instanceof Number) {
                            product.setDanhGia(((Number) danhGia).doubleValue());
                        }
                    }
                    
                    productCache = product;
                    android.util.Log.d("CartItemResponse", "✅ Successfully converted Map to ProductResponse: " + product.getName());
                    return productCache;
                } catch (Exception e) {
                    android.util.Log.e("CartItemResponse", "❌ Error converting Map to ProductResponse: " + e.getMessage(), e);
                    return null;
                }
            }
            
            // Nếu san_pham_id là string ID (chưa populate), return null
            if (sanPhamIdRaw instanceof String) {
                android.util.Log.w("CartItemResponse", "san_pham_id is String ID (not populated): " + sanPhamIdRaw);
            } else if (sanPhamIdRaw != null) {
                android.util.Log.w("CartItemResponse", "san_pham_id is unknown type: " + sanPhamIdRaw.getClass().getName());
            } else {
                android.util.Log.w("CartItemResponse", "san_pham_id is null");
            }
            return null;
        }

        public void setProduct(ProductResponse product) {
            this.productCache = product;
            this.sanPhamIdRaw = product;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get user_id - xử lý cả string và object (ObjectId từ MongoDB)
     */
    public String getUserId() {
        if (userIdRaw == null) {
            return null;
        }
        
        // Nếu là string, trả về trực tiếp
        if (userIdRaw instanceof String) {
            return (String) userIdRaw;
        }
        
        // Nếu là Map (ObjectId từ MongoDB), lấy $oid hoặc toString
        if (userIdRaw instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> idMap = (Map<String, Object>) userIdRaw;
                if (idMap.containsKey("$oid")) {
                    return idMap.get("$oid").toString();
                } else if (idMap.containsKey("_id")) {
                    Object idObj = idMap.get("_id");
                    if (idObj != null) {
                        return idObj.toString();
                    }
                }
                // Nếu không có $oid hoặc _id, thử toString
                return userIdRaw.toString();
            } catch (Exception e) {
                android.util.Log.e("CartResponse", "Error extracting user_id from Map: " + e.getMessage(), e);
                return userIdRaw.toString();
            }
        }
        
        // Các trường hợp khác, convert sang string
        return userIdRaw.toString();
    }

    public void setUserId(String userId) {
        this.userIdRaw = userId;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

