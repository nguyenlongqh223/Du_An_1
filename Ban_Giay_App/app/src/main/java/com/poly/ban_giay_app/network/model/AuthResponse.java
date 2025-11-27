package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("token")
    private String token;

    @SerializedName("user")
    private UserResponse user;

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public UserResponse getUser() {
        return user;
    }
}
