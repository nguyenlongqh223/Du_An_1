package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("_id")
    private String id;

    @SerializedName("ten_dang_nhap")
    private String username;

    @SerializedName("ho_ten")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("so_dien_thoai")
    private String phone;

    @SerializedName("dia_chi")
    private String address;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
