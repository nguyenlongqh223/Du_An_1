package com.poly.ban_giay_app.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    public Product product;
    public String size;
    public int quantity;
    public boolean isSelected;
    public long gia; // Giá tại thời điểm thêm vào giỏ (từ server)
    // ID của item trong giỏ hàng trên server (dùng cho thao tác delete/update)
    public String itemId;

    public CartItem(Product product, String size, int quantity) {
        this.product = product;
        this.size = size;
        this.quantity = quantity;
        this.isSelected = false;
        this.gia = 0; // Mặc định 0, sẽ được set từ server
        this.itemId = null;
    }

    public CartItem(Product product, String size, int quantity, long gia) {
        this.product = product;
        this.size = size;
        this.quantity = quantity;
        this.isSelected = false;
        this.gia = gia;
        this.itemId = null;
    }

    public CartItem(Product product, String size, int quantity, long gia, String itemId) {
        this.product = product;
        this.size = size;
        this.quantity = quantity;
        this.isSelected = false;
        this.gia = gia;
        this.itemId = itemId;
    }

    /**
     * Tính tổng giá của item này (giá * số lượng)
     */
    public long getTotalPrice() {
        try {
            long price = 0;
            
            // Ưu tiên dùng giá từ server (gia field)
            if (gia > 0) {
                price = gia;
            } else if (product != null && product.priceNew != null && !product.priceNew.isEmpty()) {
                // Nếu không có gia, lấy từ product.priceNew (bỏ ký tự ₫ và tất cả dấu chấm)
                String priceStr = product.priceNew.replace("₫", "").replaceAll("\\.", "");
                price = Long.parseLong(priceStr);
            }
            
            return price * quantity;
        } catch (Exception e) {
            return 0;
        }
    }
}

