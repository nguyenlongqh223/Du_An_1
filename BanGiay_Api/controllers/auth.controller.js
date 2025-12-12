const User = require("../models/User");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
const nodemailer = require("nodemailer");

// SECRET KEY cho JWT
const JWT_SECRET = process.env.JWT_SECRET || "ban_giay_secret_key";

// ---------------- ĐĂNG KÝ ----------------
exports.register = async (req, res) => {
  try {
    console.log("\n========== REGISTER USER ==========");
    console.log("Request body:", JSON.stringify(req.body, null, 2));
    
    const { ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, dia_chi } = req.body;
    
    // Validation
    if (!ten_dang_nhap || !mat_khau || !email) {
      return res.status(400).json({ 
        success: false,
        message: "Vui lòng điền đầy đủ thông tin: ten_dang_nhap, mat_khau, email" 
      });
    }
    
    // Kiểm tra email đã tồn tại chưa
    const userExists = await User.findOne({ email });
    if (userExists) {
      console.log(`❌ Email already exists: ${email}`);
      return res.status(400).json({ 
        success: false,
        message: "Email đã tồn tại" 
      });
    }
    
    // Kiểm tra ten_dang_nhap đã tồn tại chưa
    const usernameExists = await User.findOne({ ten_dang_nhap });
    if (usernameExists) {
      console.log(`❌ Username already exists: ${ten_dang_nhap}`);
      return res.status(400).json({ 
        success: false,
        message: "Tên đăng nhập đã tồn tại" 
      });
    }

    // Tạo user mới với đầy đủ thông tin
    const user = new User({ 
      ten_dang_nhap, 
      mat_khau, 
      ho_ten: ho_ten || "", 
      email,
      so_dien_thoai: so_dien_thoai || "",
      dia_chi: dia_chi || ""
    });
    
    await user.save();
    
    console.log(`✅ User registered successfully: ${user.email}`);
    console.log(`   - Ho ten: ${user.ho_ten}`);
    console.log(`   - So dien thoai: ${user.so_dien_thoai || "N/A"}`);
    console.log(`   - Dia chi: ${user.dia_chi || "N/A"}`);
    console.log("==========================================\n");
    
    // Trả về user nhưng không có password
    const userResponse = {
      _id: user._id,
      ten_dang_nhap: user.ten_dang_nhap,
      ho_ten: user.ho_ten || "",
      email: user.email,
      so_dien_thoai: user.so_dien_thoai || "",
      dia_chi: user.dia_chi || "",
      createdAt: user.createdAt,
      updatedAt: user.updatedAt
    };
    
    res.json({ 
      success: true,
      message: "Đăng ký thành công", 
      user: userResponse 
    });
  } catch (err) {
    console.error("❌ Error registering user:", err);
    if (err.code === 11000) {
      // Duplicate key error
      const field = Object.keys(err.keyPattern)[0];
      return res.status(400).json({ 
        success: false,
        message: `${field === 'email' ? 'Email' : 'Tên đăng nhập'} đã tồn tại` 
      });
    }
    res.status(500).json({ 
      success: false,
      error: err.message || "Lỗi server khi đăng ký" 
    });
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

// ---------------- QUÊN MẬT KHẨU - GỬI OTP ---------------- 
exports.forgotPassword = async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ 
        success: false,
        message: "Email là bắt buộc" 
      });
    }

    const user = await User.findOne({ email });
    if (!user) {
      return res.status(400).json({ 
        success: false,
        message: "Email không tồn tại" 
      });
    }

    // Tạo OTP 6 số
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpExpires = new Date();
    otpExpires.setMinutes(otpExpires.getMinutes() + 10); // OTP hết hạn sau 10 phút

    // Lưu OTP vào database
    user.otp = otp;
    user.otpExpires = otpExpires;
    await user.save();

    // Gửi email với OTP
    try {
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
          user: process.env.EMAIL_USER,
          pass: process.env.EMAIL_PASS,
        },
      });

      const mailOptions = {
        from: process.env.EMAIL_USER || "noreply@bangiay.com",
        to: user.email,
        subject: "Mã xác nhận đặt lại mật khẩu",
        html: `
          <div style="font-family: Arial, sans-serif; padding: 20px;">
            <h2>Mã xác nhận đặt lại mật khẩu</h2>
            <p>Xin chào ${user.ho_ten || user.ten_dang_nhap},</p>
            <p>Bạn đã yêu cầu đặt lại mật khẩu. Mã xác nhận của bạn là:</p>
            <div style="background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
              ${otp}
            </div>
            <p>Mã này sẽ hết hạn sau 10 phút.</p>
            <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
          </div>
        `,
        text: `Mã xác nhận đặt lại mật khẩu của bạn là: ${otp}. Mã này sẽ hết hạn sau 10 phút.`,
      };

      await transporter.sendMail(mailOptions);
      console.log("OTP email sent to:", user.email);
    } catch (emailError) {
      console.error("Error sending email:", emailError);
      // Vẫn trả về success nếu không gửi được email (để test)
      // Trong production nên return error
    }

    res.json({ 
      success: true,
      message: "Mã xác nhận đã được gửi vào email của bạn" 
    });
  } catch (err) {
    console.error("Error in forgotPassword:", err);
    res.status(500).json({ 
      success: false,
      error: err.message || "Lỗi server khi gửi mã xác nhận" 
    });
  }
};

// ---------------- XÁC THỰC OTP VÀ ĐẶT LẠI MẬT KHẨU ---------------- 
exports.verifyOtpAndResetPassword = async (req, res) => {
  try {
    const { email, otp, newPassword, confirmPassword } = req.body;

    if (!email || !otp || !newPassword || !confirmPassword) {
      return res.status(400).json({ 
        success: false,
        message: "Vui lòng điền đầy đủ thông tin" 
      });
    }

    if (newPassword !== confirmPassword) {
      return res.status(400).json({ 
        success: false,
        message: "Mật khẩu mới và xác nhận mật khẩu không khớp" 
      });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ 
        success: false,
        message: "Mật khẩu phải có ít nhất 6 ký tự" 
      });
    }

    const user = await User.findOne({ email });
    if (!user) {
      return res.status(400).json({ 
        success: false,
        message: "Email không tồn tại" 
      });
    }

    // Kiểm tra OTP
    if (!user.otp || user.otp !== otp) {
      return res.status(400).json({ 
        success: false,
        message: "Mã xác nhận không đúng" 
      });
    }

    // Kiểm tra OTP hết hạn
    if (!user.otpExpires || new Date() > user.otpExpires) {
      return res.status(400).json({ 
        success: false,
        message: "Mã xác nhận đã hết hạn. Vui lòng yêu cầu mã mới" 
      });
    }

    // Đặt lại mật khẩu
    user.mat_khau = newPassword;
    user.otp = undefined;
    user.otpExpires = undefined;
    await user.save();

    res.json({ 
      success: true,
      message: "Đặt lại mật khẩu thành công" 
    });
  } catch (err) {
    console.error("Error in verifyOtpAndResetPassword:", err);
    res.status(500).json({ 
      success: false,
      error: err.message || "Lỗi server khi đặt lại mật khẩu" 
    });
  }
};
