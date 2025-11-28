package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    @SerializedName("email")
    private final String email;

    @SerializedName("otp")
    private final String otp;

    @SerializedName("new_password")
    private final String newPassword;

    public ResetPasswordRequest(String email, String otp, String newPassword) {
        this.email = email;
        this.otp = otp;
        this.newPassword = newPassword;
    }
}


