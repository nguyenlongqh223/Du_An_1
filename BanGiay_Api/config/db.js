const mongoose = require("mongoose");

const connectDB = async () => {
  try {
    const uri = process.env.MONGODB_URI || "mongodb://localhost:27017/BanGiay_App";
    await mongoose.connect(uri);
    console.log("MongoDB kết nối thành công!");
  } catch (error) {
    console.log("Lỗi kết nối MongoDB: ", error);
    process.exit(1);
  }
};

module.exports = connectDB;
