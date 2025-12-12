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
  res.json({
    message: "API BanGiay đang chạy...",
    timestamp: new Date().toISOString(),
    endpoints: {
      products: "/api/product",
      bestSelling: "/api/product/best-selling",
      cart: "/api/cart",
      orders: "/api/order",
      auth: "/api/auth",
      notifications: "/api/notification",
      categories: "/api/category",
      users: "/api/user"
    }
  });
});

// Health check endpoint
app.get("/health", async (req, res) => {
  const mongoose = require("mongoose");
  const Product = require("./models/Product");
  
  try {
    const dbStatus = mongoose.connection.readyState === 1 ? "connected" : "disconnected";
    const productCount = await Product.countDocuments();
    const activeProductCount = await Product.countDocuments({ trang_thai: "active" });
    
    res.json({
      status: "ok",
      database: dbStatus,
      products: {
        total: productCount,
        active: activeProductCount
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    res.status(500).json({
      status: "error",
      error: error.message
    });
  }
});

// ------------------- Routes -------------------
// Auth: đăng ký, đăng nhập, quên mật khẩu
app.use("/api/auth", require("./routes/auth.routes"));

// User CRUD
app.use("/api/user", require("./routes/user.routes"));

// Product CRUD
app.use("/api/product", require("./routes/product.routes"));

// Cart APIs
app.use("/api/cart", require("./routes/cart.routes"));

// Order APIs
app.use("/api/order", require("./routes/order.routes"));

// Notification APIs
app.use("/api/notification", require("./routes/notification.routes"));

// Payment APIs
app.use("/api/payment", require("./routes/payment.routes"));

// Category APIs
app.use("/api/category", require("./routes/category.routes"));

// ------------------- Server -------------------
const PORT = process.env.PORT || 3000;
// Listen trên tất cả interfaces (0.0.0.0) để có thể truy cập từ mạng local
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server đang chạy tại http://localhost:${PORT}`);
  console.log(`Server có thể truy cập từ mạng local tại: http://192.168.0.100:${PORT}`);
});
