const mongoose = require("mongoose");

const notificationSchema = new mongoose.Schema(
  {
    user_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    // Hỗ trợ cả 2 format: tiếng Việt và tiếng Anh
    // Format tiếng Việt (đang dùng trong database)
    tieu_de: {
      type: String,
    },
    noi_dung: {
      type: String,
    },
    loai: {
      type: String,
      enum: ["new_product", "order_update", "promotion", "system", "notification"],
    },
    da_doc: {
      type: Boolean,
      default: false,
    },
    duong_dan: {
      type: String,
    },
    metadata: {
      type: mongoose.Schema.Types.Mixed,
    },
    // Format tiếng Anh (để tương thích ngược)
    title: {
      type: String,
    },
    message: {
      type: String,
    },
    type: {
      type: String,
      enum: ["product", "order", "promotion", "system"],
    },
    is_read: {
      type: Boolean,
      default: false,
    },
    link: {
      type: String,
    },
    product_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Product",
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Notification", notificationSchema);
