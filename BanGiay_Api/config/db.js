const mongoose = require("mongoose");

const connectDB = async () => {
  try {
    await mongoose.connect("mongodb://localhost:27017/BanGiay_App");
    console.log("MongoDB kết nối thành công!");
  } catch (error) {
    console.log("Lỗi kết nối MongoDB: ", error);
    process.exit(1);
  }
};

module.exports = connectDB;
