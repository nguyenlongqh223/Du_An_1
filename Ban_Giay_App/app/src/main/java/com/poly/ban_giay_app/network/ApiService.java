package com.poly.ban_giay_app.network;

import com.poly.ban_giay_app.network.model.AuthResponse;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.UserResponse;
import com.poly.ban_giay_app.network.request.ForgotPasswordRequest;
import com.poly.ban_giay_app.network.request.LoginRequest;
import com.poly.ban_giay_app.network.request.RegisterRequest;
import com.poly.ban_giay_app.network.request.ResetPasswordRequest;
import com.poly.ban_giay_app.network.request.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<BaseResponse<UserResponse>> register(@Body RegisterRequest request);

    @POST("api/auth/forgot-password")
    Call<BaseResponse<Void>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/verify-otp")
    Call<BaseResponse<Void>> verifyOtp(@Body VerifyOtpRequest request);

    @POST("api/auth/reset-password")
    Call<BaseResponse<Void>> resetPassword(@Body ResetPasswordRequest request);
}
