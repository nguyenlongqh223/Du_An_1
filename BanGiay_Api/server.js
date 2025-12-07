const express = require("express");
const cors = require("cors");
const connectDB = require("./config/db");
require("dotenv").config();

const app = express();

// Middleware
app.use(cors({
  origin: "*", // Cho phép tất cả origin (có thể thay bằng domain cụ thể)
  methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization"],
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Middleware logging requests
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
  if (req.method === "POST" || req.method === "PUT") {
    console.log("Body:", JSON.stringify(req.body, null, 2));
  }
  next();
});

// Kết nối MongoDB
connectDB();

// ------------------- Test API -------------------
app.get("/", (req, res) => {
  res.send("API BanGiay đang chạy...");
});

// ------------------- Routes -------------------
// Auth: đăng ký, đăng nhập, quên mật khẩu
app.use("/api/auth", require("./routes/auth.routes"));

// User CRUD
app.use("/api/user", require("./routes/user.routes"));

// Product CRUD
app.use("/api/product", require("./routes/product.routes"));

// Payment
app.use("/api/payment", require("./routes/payment.routes"));

// ------------------- Server -------------------
const PORT = process.env.PORT || 3000;
// Listen trên tất cả interfaces (0.0.0.0) để có thể truy cập từ mạng local
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server đang chạy tại http://localhost:${PORT}`);
  console.log(`Server có thể truy cập từ mạng local tại: http://192.168.0.100:${PORT}`);
});
