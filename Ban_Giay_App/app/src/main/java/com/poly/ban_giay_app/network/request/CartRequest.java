package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

/**
 * Request để thêm sản phẩm vào giỏ hàng
 */
public class CartRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("product_id")
    private String productId;

    @SerializedName("size")
    private String size;

    @SerializedName("quantity")
    private Integer quantity;

    // Dùng khi xóa item trong giỏ hàng (backend yêu cầu item_id)
    @SerializedName("item_id")
    private String itemId;

    public CartRequest() {
    }

    public CartRequest(String userId, String productId, String size, Integer quantity) {
        this.userId = userId;
        this.productId = productId;
        this.size = size;
        this.quantity = quantity;
    }

    public CartRequest(String userId, String itemId) {
        this.userId = userId;
        this.itemId = itemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}

