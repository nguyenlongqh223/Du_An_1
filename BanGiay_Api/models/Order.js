const mongoose = require("mongoose");

const OrderItemSchema = new mongoose.Schema({
  san_pham_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Product",
    required: true,
  },
  ten_san_pham: { type: String, required: true },
  so_luong: { type: Number, required: true },
  kich_thuoc: { type: String, required: true },
  gia: { type: Number, required: true },
});

const OrderSchema = new mongoose.Schema(
  {
    user_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    items: [OrderItemSchema],
    tong_tien: { type: Number, required: true },
    trang_thai: {
      type: String,
      enum: ["pending", "confirmed", "paid", "delivered", "cancelled"],
      default: "pending",
    },
    dia_chi_giao_hang: { type: String },
    so_dien_thoai: { type: String },
    ghi_chu: { type: String },
    da_thanh_toan: { type: Boolean, default: false },
    is_paid: { type: Boolean, default: false },
    ly_do_huy: { type: String },
    // Soft delete fields
    is_deleted: { type: Boolean, default: false },
    deleted_at: { type: Date, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Order", OrderSchema);




