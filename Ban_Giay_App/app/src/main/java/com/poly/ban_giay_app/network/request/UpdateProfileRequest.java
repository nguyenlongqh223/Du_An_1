package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {
    @SerializedName("ho_ten")
    private String hoTen;

    @SerializedName("so_dien_thoai")
    private String soDienThoai;

    @SerializedName("dia_chi")
    private String diaChi;

    public UpdateProfileRequest(String hoTen, String soDienThoai, String diaChi) {
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.diaChi = diaChi;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }
}

