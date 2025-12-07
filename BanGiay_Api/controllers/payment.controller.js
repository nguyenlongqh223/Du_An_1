const Payment = require("../models/Payment");
const User = require("../models/User");

// Tạo payment mới
exports.createPayment = async (req, res) => {
  try {
    console.log("POST /api/payment - Body:", req.body);

    const {
      user_id,
      email,
      ten_chu_the,
      so_the,
      loai_the,
      ten_san_pham,
      gia_san_pham,
      so_luong,
      kich_thuoc,
      ngay_het_han,
    } = req.body;

    // Validate required fields
    if (!ten_chu_the || !so_the || !loai_the || !ten_san_pham || !gia_san_pham) {
      return res.status(400).json({
        success: false,
        message: "Vui lòng điền đầy đủ thông tin thanh toán",
      });
    }

    // Validate user_id or email - cho phép null nếu không có
    let finalUserId = (user_id && user_id.trim() !== "") ? user_id : null;
    let finalEmail = (email && email.trim() !== "") ? email : null;

    // Nếu có email nhưng không có user_id, tìm user theo email
    if (email && !user_id) {
      try {
        const user = await User.findOne({ email: email });
        if (user) {
          finalUserId = user._id.toString();
          finalEmail = user.email;
        }
      } catch (err) {
        console.log("Could not find user by email:", err.message);
      }
    } else if (user_id && !email) {
      try {
        const user = await User.findById(user_id);
        if (user) {
          finalEmail = user.email;
        }
      } catch (err) {
        console.log("Could not find user by id:", err.message);
      }
    }

    // Nếu vẫn không có email và user_id, vẫn cho phép tạo payment (guest checkout)
    if (!finalEmail && !finalUserId) {
      console.log("Warning: Creating payment without user_id or email (guest checkout)");
    }

    // Tạo payment mới
    const paymentData = {
      user_id: finalUserId,
      email: finalEmail,
      ten_chu_the,
      so_the: so_the.replace(/\s/g, ""), // Xóa khoảng trắng trong số thẻ
      loai_the,
      ten_san_pham,
      gia_san_pham,
      so_luong: so_luong || 1,
      kich_thuoc: kich_thuoc || "",
      ngay_het_han: ngay_het_han || "",
      trang_thai: "pending",
    };

    console.log("Creating payment with data:", JSON.stringify(paymentData, null, 2));

    const payment = new Payment(paymentData);

    try {
      const savedPayment = await payment.save();
      console.log("Payment saved successfully:", savedPayment._id);

      res.status(201).json({
        success: true,
        message: "Thanh toán đã được tạo thành công",
        data: savedPayment,
      });
    } catch (saveError) {
      console.error("Error saving payment to database:", saveError);
      if (saveError.name === "ValidationError") {
        return res.status(400).json({
          success: false,
          message: "Dữ liệu không hợp lệ",
          error: saveError.message,
        });
      }
      throw saveError; // Re-throw to be caught by outer catch
    }
  } catch (error) {
    console.error("Error creating payment:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi tạo thanh toán",
      error: error.message,
    });
  }
};

// Lấy tất cả payments
exports.getAllPayments = async (req, res) => {
  try {
    const payments = await Payment.find()
      .populate("user_id", "email name")
      .sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      data: payments,
    });
  } catch (error) {
    console.error("Error getting payments:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy danh sách thanh toán",
      error: error.message,
    });
  }
};

// Lấy payment theo user_id
exports.getPaymentsByUserId = async (req, res) => {
  try {
    const { user_id } = req.params;

    const payments = await Payment.find({ user_id })
      .sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      data: payments,
    });
  } catch (error) {
    console.error("Error getting user payments:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy danh sách thanh toán",
      error: error.message,
    });
  }
};

// Lấy payment theo email
exports.getPaymentsByEmail = async (req, res) => {
  try {
    const { email } = req.params;

    const payments = await Payment.find({ email })
      .sort({ createdAt: -1 });

    res.status(200).json({
      success: true,
      data: payments,
    });
  } catch (error) {
    console.error("Error getting payments by email:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy danh sách thanh toán",
      error: error.message,
    });
  }
};

// Cập nhật trạng thái payment
exports.updatePaymentStatus = async (req, res) => {
  try {
    const { id } = req.params;
    const { trang_thai } = req.body;

    if (!trang_thai || !["pending", "completed", "failed"].includes(trang_thai)) {
      return res.status(400).json({
        success: false,
        message: "Trạng thái không hợp lệ",
      });
    }

    const payment = await Payment.findByIdAndUpdate(
      id,
      { trang_thai },
      { new: true }
    );

    if (!payment) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy thanh toán",
      });
    }

    res.status(200).json({
      success: true,
      message: "Cập nhật trạng thái thành công",
      data: payment,
    });
  } catch (error) {
    console.error("Error updating payment status:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi cập nhật trạng thái",
      error: error.message,
    });
  }
};

