const express = require("express");
const cors = require("cors");
const connectDB = require("./config/db");

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Kết nối MongoDB
connectDB();

// ------------------- Test API -------------------
app.get("/", (req, res) => {
  res.send("API BanGiay đang chạy...");
});

// ------------------- Routes -------------------
// Auth: đăng ký, đăng nhập, quên mật khẩu
app.use("/api/auth", require("./routes/auth.routes"));

// User CRUD (nếu bạn tạo routes/user.routes.js sau này)
app.use("/api/user", require("./routes/user.routes"));

// ------------------- Server -------------------
const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Server đang chạy tại http://localhost:${PORT}`);
});
