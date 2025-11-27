const User = require("../models/User");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
const nodemailer = require("nodemailer");

// SECRET KEY cho JWT
const JWT_SECRET = "ban_giay_secret_key";

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

    // Tạo mật khẩu tạm thời
    const tempPassword = Math.random().toString(36).slice(-8);
    user.mat_khau = tempPassword;
    await user.save();

    // Gửi email
    const transporter = nodemailer.createTransport({
      service: "gmail",
      auth: {
        user: "youremail@gmail.com", // đổi thành email của bạn
        pass: "yourpassword", // mật khẩu ứng dụng Gmail
      },
    });

    const mailOptions = {
      from: "youremail@gmail.com",
      to: user.email,
      subject: "Quên mật khẩu",
      text: `Mật khẩu mới của bạn là: ${tempPassword}`,
    };

    transporter.sendMail(mailOptions, (err, info) => {
      if (err) console.log(err);
      else console.log("Email sent: " + info.response);
    });

    res.json({ message: "Mật khẩu mới đã được gửi vào email của bạn" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
