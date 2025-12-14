package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {
    @SerializedName("success")
    private Boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private T user;

    @SerializedName("data")
    private T data;

    @SerializedName("products")
    private T products;

    @SerializedName("product")
    private T product;

    @SerializedName("notifications")
    private T notifications;

    public Boolean getSuccess() {
        return success != null ? success : true;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        // Ưu tiên data, sau đó notifications, product, products, cuối cùng là user
        if (data != null) {
            return data;
        }
        if (notifications != null) {
            return notifications;
        }
        if (product != null) {
            return product;
        }
        if (products != null) {
            return products;
        }
        return user;
    }

    public T getNotifications() {
        return notifications;
    }
}
