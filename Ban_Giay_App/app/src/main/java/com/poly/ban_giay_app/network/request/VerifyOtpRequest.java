package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {
    @SerializedName("email")
    private final String email;

    @SerializedName("otp")
    private final String otp;

    public VerifyOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}


