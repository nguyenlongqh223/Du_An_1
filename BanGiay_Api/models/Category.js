const mongoose = require("mongoose");

const CategorySchema = new mongoose.Schema(
  {
    ten_danh_muc: {
      type: String,
      required: true,
      unique: true,
      trim: true,
    },
    danh_muc: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      lowercase: true,
    },
    mo_ta: {
      type: String,
      default: "",
    },
    trang_thai: {
      type: String,
      enum: ["active", "inactive"],
      default: "active",
    },
    // Soft delete fields
    is_deleted: { type: Boolean, default: false },
    deleted_at: { type: Date, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Category", CategorySchema);

