const User = require("../models/User");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
const nodemailer = require("nodemailer");

// SECRET KEY cho JWT
const JWT_SECRET = "ban_giay_secret_key";
const OTP_EXPIRES_MINUTES = 5;
const EMAIL_USER = process.env.EMAIL_USER || "hahsjdbfbf@gmail.com";
const EMAIL_PASS = (process.env.EMAIL_PASS || "pehigz dylepeqpzi").replace(/\s+/g, "");

const transporter = nodemailer.createTransport({
  host: "smtp.gmail.com",
  port: 465,
  secure: true,
  auth: {
    user: EMAIL_USER,
    pass: EMAIL_PASS,
  },
  tls: {
    rejectUnauthorized: false,
  },
});

const generateOtp = () => Math.floor(100000 + Math.random() * 900000).toString();

// ---------------- ĐĂNG KÝ ----------------
exports.register = async (req, res) => {
  try {
    const { ten_dang_nhap, mat_khau, ho_ten, email } = req.body;
    const userExists = await User.findOne({ email });
    if (userExists)
      return res.status(400).json({ message: "Email đã tồn tại" });

    const user = new User({ ten_dang_nhap, mat_khau, ho_ten, email });
    await user.save();
    res.json({ message: "Đăng ký thành công", user });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// ---------------- ĐĂNG NHẬP ----------------
exports.login = async (req, res) => {
  try {
    const { email, mat_khau } = req.body;
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ message: "Email không tồn tại" });

    const isMatch = await user.comparePassword(mat_khau);
    if (!isMatch)
      return res.status(400).json({ message: "Mật khẩu không đúng" });

    const token = jwt.sign({ id: user._id }, JWT_SECRET, { expiresIn: "1d" });
    res.json({ message: "Đăng nhập thành công", token, user });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// ---------------- QUÊN MẬT KHẨU ----------------
exports.forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ message: "Email không tồn tại" });

    const otp = generateOtp();
    user.otp_code = otp;
    user.otp_expires = new Date(Date.now() + OTP_EXPIRES_MINUTES * 60 * 1000);
    user.otp_verified = false;
    await user.save();

    const mailOptions = {
      from: EMAIL_USER,
      to: user.email,
      subject: "Mã OTP đặt lại mật khẩu",
      text: `Mã OTP của bạn là: ${otp}. Mã sẽ hết hạn sau ${OTP_EXPIRES_MINUTES} phút.`,
    };

    try {
      await transporter.sendMail(mailOptions);
    } catch (error) {
      console.error("Send OTP failed:", error);
      return res
        .status(500)
        .json({ message: "Không thể gửi OTP, vui lòng kiểm tra cấu hình email." });
    }

    res.json({ message: "Mã OTP đã được gửi vào email của bạn" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// ---------------- XÁC THỰC OTP ----------------
exports.verifyOtp = async (req, res) => {
  try {
    const { email, otp } = req.body;
    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ message: "Email không tồn tại" });

    if (
      !user.otp_code ||
      !user.otp_expires ||
      user.otp_code !== otp ||
      user.otp_expires < new Date()
    ) {
      return res.status(400).json({ message: "Mã OTP không hợp lệ hoặc đã hết hạn" });
    }

    user.otp_verified = true;
    await user.save();
    res.json({ message: "Xác minh OTP thành công" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// ---------------- ĐẶT LẠI MẬT KHẨU ----------------
exports.resetPassword = async (req, res) => {
  try {
    const { email, otp, new_password } = req.body;
    if (!new_password) {
      return res.status(400).json({ message: "Mật khẩu mới không được để trống" });
    }

    const user = await User.findOne({ email });
    if (!user) return res.status(400).json({ message: "Email không tồn tại" });

    if (
      !user.otp_code ||
      !user.otp_expires ||
      user.otp_code !== otp ||
      user.otp_expires < new Date()
    ) {
      return res.status(400).json({ message: "Mã OTP không hợp lệ hoặc đã hết hạn" });
    }

    user.mat_khau = new_password;
    user.otp_code = undefined;
    user.otp_expires = undefined;
    user.otp_verified = false;
    await user.save();

    res.json({ message: "Đặt lại mật khẩu thành công" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
