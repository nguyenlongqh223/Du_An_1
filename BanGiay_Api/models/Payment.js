const mongoose = require("mongoose");

const PaymentSchema = new mongoose.Schema(
  {
    user_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null, // Allow null for guest payments
      sparse: true, // Allow multiple null values
    },
    email: {
      type: String,
      default: null, // Allow null for guest payments
    },
    ten_chu_the: {
      type: String,
      required: true,
    },
    so_the: {
      type: String,
      required: true,
    },
    loai_the: {
      type: String,
      enum: ["credit_card", "atm_card", "bank_transfer", "cod"],
      required: true,
    },
    ten_san_pham: {
      type: String,
      required: true,
    },
    gia_san_pham: {
      type: String,
      required: true,
    },
    so_luong: {
      type: Number,
      default: 1,
    },
    kich_thuoc: {
      type: String,
    },
    ngay_het_han: {
      type: String,
    },
    trang_thai: {
      type: String,
      enum: ["pending", "completed", "failed", "refunded"],
      default: "pending",
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Payment", PaymentSchema);

