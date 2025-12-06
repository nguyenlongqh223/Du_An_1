const http = require("http");

// Test data
const testPaymentData = {
  phuong_thuc_thanh_toan: "credit_card",
  ten_chu_the: "Nguyen Van A",
  so_the_day_du: "1234567890123456",
  ngay_het_han: "12/25",
  email: "test@example.com",
  ten_san_pham: "Giày Converse Chuck Taylor All Star",
  gia_thanh_toan: "1.200.000₫"
};

const postData = JSON.stringify(testPaymentData);

const options = {
  hostname: "localhost",
  port: 3000,
  path: "/api/payment",
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(postData),
  },
};

console.log("🧪 Test API Payment...");
console.log("📤 Gửi request:", testPaymentData);
console.log("");

const req = http.request(options, (res) => {
  console.log(`📥 Status Code: ${res.statusCode}`);
  console.log(`📋 Headers:`, res.headers);
  console.log("");

  let data = "";

  res.on("data", (chunk) => {
    data += chunk;
  });

  res.on("end", () => {
    console.log("📦 Response Body:");
    try {
      const jsonData = JSON.parse(data);
      console.log(JSON.stringify(jsonData, null, 2));
      
      if (res.statusCode === 201 && jsonData.success) {
        console.log("\n✅ Test thành công! Payment đã được tạo.");
        console.log(`   ID: ${jsonData.data.id}`);
        console.log(`   Tên chủ thẻ: ${jsonData.data.ten_chu_the}`);
        console.log(`   4 số cuối: ${jsonData.data.so_the_cuoi}`);
        console.log(`   Email: ${jsonData.data.email}`);
      } else {
        console.log("\n❌ Test thất bại!");
      }
    } catch (e) {
      console.log(data);
      console.log("\n❌ Không thể parse JSON response");
    }
  });
});

req.on("error", (error) => {
  console.error("❌ Lỗi:", error.message);
  console.error("\n💡 Đảm bảo server đang chạy: npm start");
});

req.write(postData);
req.end();

