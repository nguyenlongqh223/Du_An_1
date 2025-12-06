const Payment = require("../models/Payment");

// Tạo thanh toán mới
exports.createPayment = async (req, res) => {
  try {
    console.log("POST /api/payment - Body:", req.body);

    const {
      phuong_thuc_thanh_toan,
      ten_chu_the,
      so_the_day_du,
      ngay_het_han,
      email,
      ten_san_pham,
      gia_thanh_toan,
      so_tai_khoan_chuyen_tien,
    } = req.body;

    // Validation - khác nhau tùy phương thức thanh toán
    if (!phuong_thuc_thanh_toan || !email || !ten_san_pham || !gia_thanh_toan) {
      return res.status(400).json({
        success: false,
        message: "Vui lòng điền đầy đủ thông tin",
      });
    }

    // Đối với credit_card và atm_card, cần ten_chu_the
    if ((phuong_thuc_thanh_toan === "credit_card" || phuong_thuc_thanh_toan === "atm_card") && !ten_chu_the) {
      return res.status(400).json({
        success: false,
        message: "Tên chủ thẻ không được để trống",
      });
    }

    // Validate payment method
    if (!["credit_card", "atm_card", "bank_payment"].includes(phuong_thuc_thanh_toan)) {
      return res.status(400).json({
        success: false,
        message: "Phương thức thanh toán không hợp lệ",
      });
    }

    // Validate email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        success: false,
        message: "Email không hợp lệ",
      });
    }

    // Lấy 4 số cuối của thẻ/tài khoản
    let so_the_cuoi = "";
    if (phuong_thuc_thanh_toan === "credit_card" || phuong_thuc_thanh_toan === "atm_card") {
      if (!so_the_day_du) {
        return res.status(400).json({
          success: false,
          message: "Số thẻ không được để trống",
        });
      }
      // Lấy 4 số cuối
      const cardNumber = so_the_day_du.replace(/\s/g, ""); // Xóa khoảng trắng
      if (cardNumber.length < 4) {
        return res.status(400).json({
          success: false,
          message: "Số thẻ không hợp lệ",
        });
      }
      so_the_cuoi = cardNumber.substring(cardNumber.length - 4);
    } else if (phuong_thuc_thanh_toan === "bank_payment") {
      // Đối với thanh toán ngân hàng, lấy số tài khoản chuyển tiền
      if (!so_tai_khoan_chuyen_tien) {
        return res.status(400).json({
          success: false,
          message: "Số tài khoản chuyển tiền không được để trống",
        });
      }
      // Lấy 4 số cuối của tài khoản
      const accountNumber = so_tai_khoan_chuyen_tien.replace(/\s/g, "");
      if (accountNumber.length < 4) {
        return res.status(400).json({
          success: false,
          message: "Số tài khoản không hợp lệ",
        });
      }
      so_the_cuoi = accountNumber.substring(accountNumber.length - 4);
    }

    // Tạo payment mới
    const paymentData = {
      phuong_thuc_thanh_toan,
      ten_chu_the: phuong_thuc_thanh_toan === "bank_payment" ? "N/A" : ten_chu_the, // Bank payment không cần tên chủ thẻ
      so_the_cuoi,
      email,
      ten_san_pham,
      gia_thanh_toan,
      trang_thai: "completed", // Mặc định là completed vì đã nhập đầy đủ thông tin
    };

    // Thêm thông tin chuyển khoản nếu là bank_payment
    if (phuong_thuc_thanh_toan === "bank_payment" && so_tai_khoan_chuyen_tien) {
      paymentData.so_tai_khoan_chuyen_tien = so_tai_khoan_chuyen_tien;
    }

    const payment = new Payment(paymentData);

    await payment.save();

    // Convert _id to id for response

    res.status(201).json({
      success: true,
      message: "Thanh toán thành công",
    });
  } catch (error) {
    console.error("Error creating payment:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server khi tạo thanh toán",
      error: error.message,
    });
  }
};

// Lấy tất cả thanh toán (có thể dùng cho admin)
exports.getAllPayments = async (req, res) => {
  try {
    const { page = 1, limit = 10, email, trang_thai } = req.query;

    const query = {};
    if (email) {
      query.email = email.toLowerCase();
    }
    if (trang_thai) {
      query.trang_thai = trang_thai;
    }

    const payments = await Payment.find(query)
      .sort({ createdAt: -1 })
      .limit(limit * 1)
      .skip((page - 1) * limit);

    const total = await Payment.countDocuments(query);

    res.json({
      success: true,
      data: payments,
      pagination: {
        page: Number(page),
        limit: Number(limit),
        total,
        pages: Math.ceil(total / limit),
      },
    });
  } catch (error) {
    console.error("Error getting payments:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server khi lấy danh sách thanh toán",
      error: error.message,
    });
  }
};

// Lấy thanh toán theo ID
exports.getPaymentById = async (req, res) => {
  try {
    const payment = await Payment.findById(req.params.id);

    if (!payment) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy thanh toán",
      });
    }

    res.json({
      success: true,
      data: payment,
    });
  } catch (error) {
    console.error("Error getting payment:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi server khi lấy thông tin thanh toán",
      error: error.message,
    });
  }
};

