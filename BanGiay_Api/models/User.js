const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");

const UserSchema = new mongoose.Schema(
  {
    ten_dang_nhap: { type: String, required: true, unique: true },
    mat_khau: { type: String, required: true },
    ho_ten: { type: String },
    email: { type: String, required: true, unique: true },
    so_dien_thoai: { type: String },
    dia_chi: { type: String },
    trang_thai: {
      type: String,
      enum: ["active", "inactive"],
      default: "active"
    },
    vatro_id: { type: mongoose.Schema.Types.ObjectId, ref: "VaiTro" },
    // OTP fields for forgot password
    otp: { type: String },
    otpExpires: { type: Date },
  },
  { timestamps: true }
);

// Hash password trước khi lưu
UserSchema.pre("save", async function () {
  if (!this.isModified("mat_khau")) return;
  const salt = await bcrypt.genSalt(10);
  this.mat_khau = await bcrypt.hash(this.mat_khau, salt);
});

// Method kiểm tra mật khẩu
UserSchema.methods.comparePassword = async function (password) {
  return await bcrypt.compare(password, this.mat_khau);
};

module.exports = mongoose.model("User", UserSchema);
