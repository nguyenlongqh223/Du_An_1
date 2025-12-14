package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Model để map với dữ liệu đơn hàng từ MongoDB
 */
public class OrderResponse {
    @SerializedName("_id")
    private String id;

    // user_id có thể là string ID hoặc object (ObjectId từ MongoDB)
    @SerializedName("user_id")
    private Object userIdRaw;

    @SerializedName("items")
    private List<OrderItemResponse> items;

    @SerializedName("tong_tien")
    private Integer tongTien;

    @SerializedName("trang_thai")
    private String trangThai;

    @SerializedName("dia_chi_giao_hang")
    private String diaChiGiaoHang;

    @SerializedName("so_dien_thoai")
    private String soDienThoai;

    @SerializedName("ghi_chu")
    private String ghiChu;

    @SerializedName("ten_khach_hang")
    private String tenKhachHang;

    @SerializedName("ho_ten")
    private String hoTen;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public static class OrderItemResponse {
        @SerializedName("san_pham_id")
        private String sanPhamId;

        @SerializedName("ten_san_pham")
        private String tenSanPham;

        @SerializedName("so_luong")
        private Integer soLuong;

        @SerializedName("kich_thuoc")
        private String kichThuoc;

        @SerializedName("gia")
        private Integer gia;

        public String getSanPhamId() {
            return sanPhamId;
        }

        public void setSanPhamId(String sanPhamId) {
            this.sanPhamId = sanPhamId;
        }

        public String getTenSanPham() {
            return tenSanPham;
        }

        public void setTenSanPham(String tenSanPham) {
            this.tenSanPham = tenSanPham;
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
                android.util.Log.e("OrderResponse", "Error extracting user_id from Map: " + e.getMessage(), e);
                return userIdRaw.toString();
            }
        }
        
        // Các trường hợp khác, convert sang string
        return userIdRaw.toString();
    }

    public void setUserId(String userId) {
        this.userIdRaw = userId;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public Integer getTongTien() {
        return tongTien;
    }

    public void setTongTien(Integer tongTien) {
        this.tongTien = tongTien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getDiaChiGiaoHang() {
        return diaChiGiaoHang;
    }

    public void setDiaChiGiaoHang(String diaChiGiaoHang) {
        this.diaChiGiaoHang = diaChiGiaoHang;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public String getTenKhachHang() {
        // Ưu tiên ten_khach_hang, sau đó ho_ten
        if (tenKhachHang != null && !tenKhachHang.isEmpty()) {
            return tenKhachHang;
        }
        return hoTen != null ? hoTen : null;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
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

    /**
     * Lấy tên trạng thái hiển thị
     */
    public String getTrangThaiDisplay() {
        if (trangThai == null) return "Không xác định";
        switch (trangThai) {
            case "pending":
                return "Chờ xác nhận";
            case "confirmed":
                return "Đã xác nhận";
            case "shipping":
                return "Đang giao hàng";
            case "delivered":
                return "Đã giao hàng";
            case "cancelled":
                return "Đã hủy";
            default:
                return trangThai;
        }
    }
}

