package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class CategoryResponse {
    @SerializedName("_id")
    private String id;

    @SerializedName("ten_danh_muc")
    private String tenDanhMuc;

    @SerializedName("hinh_anh")
    private String imageUrl;

    @SerializedName("trang_thai")
    private String trangThai;

    @SerializedName("thu_tu")
    private Integer thuTu;

    @SerializedName("hien_thi_trang_chu")
    private Boolean hienThiTrangChu;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenDanhMuc() {
        return tenDanhMuc;
    }

    public void setTenDanhMuc(String tenDanhMuc) {
        this.tenDanhMuc = tenDanhMuc;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public Integer getThuTu() {
        return thuTu;
    }

    public void setThuTu(Integer thuTu) {
        this.thuTu = thuTu;
    }

    public Boolean getHienThiTrangChu() {
        return hienThiTrangChu;
    }

    public void setHienThiTrangChu(Boolean hienThiTrangChu) {
        this.hienThiTrangChu = hienThiTrangChu;
    }
}
