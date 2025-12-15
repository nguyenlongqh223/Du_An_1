package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {
    @SerializedName("_id")
    private String id;

    // MongoDB field: tieu_de (ưu tiên)
    @SerializedName("tieu_de")
    private String tieuDe;
    
    // Fallback: title (nếu có)
    @SerializedName("title")
    private String title;

    // MongoDB field: noi_dung (ưu tiên)
    @SerializedName("noi_dung")
    private String noiDung;
    
    // Fallback: message (nếu có)
    @SerializedName("message")
    private String message;

    // MongoDB field: da_doc (ưu tiên)
    @SerializedName("da_doc")
    private Boolean daDoc;
    
    // Fallback: is_read (nếu có)
    @SerializedName("is_read")
    private Boolean isRead;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("loai")
    private String loai;

    @SerializedName("duong_dan")
    private String duongDan;

    @SerializedName("metadata")
    private Object metadata;

    // Tên sản phẩm (khi admin thay đổi trạng thái đơn hàng)
    @SerializedName("ten_san_pham")
    private String tenSanPham;
    
    // Fallback: product_name (nếu có)
    @SerializedName("product_name")
    private String productName;

    // Lý do hủy đơn (khi admin hủy đơn)
    @SerializedName("ly_do_huy")
    private String lyDoHuy;
    
    // Fallback: cancellation_reason (nếu có)
    @SerializedName("cancellation_reason")
    private String cancellationReason;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getTitle() {
        // MongoDB có field tieu_de, ưu tiên dùng nó
        if (tieuDe != null && !tieuDe.trim().isEmpty()) {
            return tieuDe;
        }
        // Fallback: nếu không có tieu_de thì dùng title
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return "Thông báo";
    }

    public String getMessage() {
        // MongoDB có field noi_dung, ưu tiên dùng nó
        if (noiDung != null && !noiDung.trim().isEmpty()) {
            return noiDung;
        }
        // Fallback: nếu không có noi_dung thì dùng message
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        return "";
    }

    public boolean isRead() {
        // MongoDB có field da_doc, ưu tiên dùng nó
        // da_doc = true nghĩa là đã đọc, da_doc = false nghĩa là chưa đọc
        if (daDoc != null) {
            return daDoc;
        }
        // Fallback: nếu không có da_doc thì dùng is_read
        if (isRead != null) {
            return isRead;
        }
        return false;
    }

    public String getUserId() {
        return userId;
    }

    public String getLoai() {
        return loai;
    }

    public String getDuongDan() {
        return duongDan;
    }

    public Object getMetadata() {
        return metadata;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getTenSanPham() {
        // Ưu tiên dùng ten_san_pham, fallback về product_name
        if (tenSanPham != null && !tenSanPham.trim().isEmpty()) {
            return tenSanPham;
        }
        if (productName != null && !productName.trim().isEmpty()) {
            return productName;
        }
        return null;
    }

    public String getLyDoHuy() {
        // Ưu tiên dùng ly_do_huy, fallback về cancellation_reason
        if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
            return lyDoHuy;
        }
        if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
            return cancellationReason;
        }
        return null;
    }
}
