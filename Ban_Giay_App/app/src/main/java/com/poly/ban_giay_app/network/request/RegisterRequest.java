package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("ten_dang_nhap")
    private final String username;

    @SerializedName("mat_khau")
    private final String password;

    @SerializedName("ho_ten")
    private final String fullName;

    @SerializedName("email")
    private final String email;

    public RegisterRequest(String username, String password, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }
}
