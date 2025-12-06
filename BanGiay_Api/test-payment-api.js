const mongoose = require("mongoose");
const Payment = require("./models/Payment");
require("dotenv").config();

// Kết nối MongoDB
const MONGODB_URI = process.env.MONGODB_URI || "mongodb://localhost:27017/BanGiay_App";

async function testPaymentAPI() {
  try {
    console.log("Đang kết nối MongoDB...");
    await mongoose.connect(MONGODB_URI);
    console.log("✅ Kết nối MongoDB thành công!");

    // Kiểm tra collection payments
    const paymentCount = await Payment.countDocuments();
    console.log(`\n📊 Số lượng payment trong database: ${paymentCount}`);

    if (paymentCount > 0) {
      console.log("\n📋 Danh sách payments:");
      const payments = await Payment.find().limit(5).sort({ createdAt: -1 });
      payments.forEach((payment, index) => {
        console.log(`\n${index + 1}. Payment ID: ${payment._id}`);
        console.log(`   - Phương thức: ${payment.phuong_thuc_thanh_toan}`);
        console.log(`   - Tên chủ thẻ: ${payment.ten_chu_the}`);
        console.log(`   - 4 số cuối: ${payment.so_the_cuoi}`);
        console.log(`   - Email: ${payment.email}`);
        console.log(`   - Sản phẩm: ${payment.ten_san_pham}`);
        console.log(`   - Giá: ${payment.gia_thanh_toan}`);
        console.log(`   - Trạng thái: ${payment.trang_thai}`);
        console.log(`   - Ngày tạo: ${payment.createdAt}`);
      });
    } else {
      console.log("\n⚠️  Collection payments đang trống!");
      console.log("   Hãy thử tạo payment từ app hoặc test API.");
    }

    // Test tạo payment mới
    console.log("\n🧪 Test tạo payment mới...");
    const testPayment = new Payment({
      phuong_thuc_thanh_toan: "credit_card",
      ten_chu_the: "Test User",
      so_the_cuoi: "1234",
      email: "test@example.com",
      ten_san_pham: "Giày Test",
      gia_thanh_toan: "1.000.000₫",
      trang_thai: "completed",
    });

    await testPayment.save();
    console.log("✅ Tạo test payment thành công!");
    console.log(`   ID: ${testPayment._id}`);

    // Xóa test payment
    await Payment.deleteOne({ _id: testPayment._id });
    console.log("🗑️  Đã xóa test payment");

    await mongoose.disconnect();
    console.log("\n✅ Test hoàn tất!");
  } catch (error) {
    console.error("❌ Lỗi:", error.message);
    console.error(error);
    process.exit(1);
  }
}

testPaymentAPI();

