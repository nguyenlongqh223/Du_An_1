package com.poly.ban_giay_app.network.request;

import com.google.gson.annotations.SerializedName;

public class PaymentRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("email")
    private String email;

    @SerializedName("ten_chu_the")
    private String tenChuThe;

    @SerializedName("so_the")
    private String soThe;

    @SerializedName("loai_the")
    private String loaiThe;

    @SerializedName("ten_san_pham")
    private String tenSanPham;

    @SerializedName("gia_san_pham")
    private String giaSanPham;

    @SerializedName("so_luong")
    private int soLuong;

    @SerializedName("kich_thuoc")
    private String kichThuoc;

    @SerializedName("ngay_het_han")
    private String ngayHetHan;

    public PaymentRequest(String userId, String email, String tenChuThe, String soThe,
                         String loaiThe, String tenSanPham, String giaSanPham, int soLuong,
                         String kichThuoc, String ngayHetHan) {
        this.userId = userId;
        this.email = email;
        this.tenChuThe = tenChuThe;
        this.soThe = soThe;
        this.loaiThe = loaiThe;
        this.tenSanPham = tenSanPham;
        this.giaSanPham = giaSanPham;
        this.soLuong = soLuong;
        this.kichThuoc = kichThuoc;
        this.ngayHetHan = ngayHetHan;
    }
}

