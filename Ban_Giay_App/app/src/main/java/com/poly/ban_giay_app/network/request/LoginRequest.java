package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("email")
    private final String email;

    @SerializedName("mat_khau")
    private final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
