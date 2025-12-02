package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {
    @SerializedName("email")
    private final String email;
    
    @SerializedName("otp")
    private final String otp;
    
    @SerializedName("newPassword")
    private final String newPassword;
    
    @SerializedName("confirmPassword")
    private final String confirmPassword;

    public VerifyOtpRequest(String email, String otp, String newPassword, String confirmPassword) {
        this.email = email;
        this.otp = otp;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}




