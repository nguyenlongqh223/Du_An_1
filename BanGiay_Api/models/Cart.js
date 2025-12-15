const mongoose = require("mongoose");

const CartItemSchema = new mongoose.Schema({
  san_pham_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Product",
    required: true,
  },
  so_luong: { type: Number, required: true, min: 1 },
  kich_thuoc: { type: String, required: true },
  gia: { type: Number, required: true }, // Giá tại thời điểm thêm vào giỏ
});

const CartSchema = new mongoose.Schema(
  {
    user_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      unique: true,
    },
    items: [CartItemSchema],
    is_deleted: {
      type: Boolean,
      default: false,
    },
    deleted_at: {
      type: Date,
      default: null,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Cart", CartSchema);




