const mongoose = require("mongoose");

const ProductSchema = new mongoose.Schema(
  {
    ten_san_pham: { type: String, required: true },
    gia_goc: { type: Number, required: true },
    gia_khuyen_mai: { type: Number, required: true },
    hinh_anh: { type: String, required: true },
    mo_ta: { type: String },
    thuong_hieu: { type: String },
    danh_muc: { 
      type: String, 
      default: "unisex"
      // Không có enum, chấp nhận bất kỳ giá trị nào
    },
    kich_thuoc: [{ type: String }], // ["37", "38", "39", "40", "41", "42"]
    so_luong_ton: { type: Number, default: 0 },
    danh_gia: { type: Number, default: 5.0 }, // 1-5 sao
    so_luong_da_ban: { type: Number, default: 0 },
    trang_thai: { type: String, enum: ["active", "inactive"], default: "active" },
  },
  { timestamps: true }
);

// Index cho tìm kiếm
ProductSchema.index({ ten_san_pham: "text", mo_ta: "text" });

module.exports = mongoose.model("Product", ProductSchema);




