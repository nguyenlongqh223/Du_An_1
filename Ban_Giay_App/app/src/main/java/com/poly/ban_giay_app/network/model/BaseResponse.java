package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {
    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private T data;

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
