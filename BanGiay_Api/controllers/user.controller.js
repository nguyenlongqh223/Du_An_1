const User = require("../models/User");

// Lấy tất cả User
exports.getAllUsers = async (req, res) => {
  try {
    console.log("\n========== GET ALL USERS ==========");
    
    // Lấy users nhưng không lấy password
    const users = await User.find().select("-mat_khau -otp -otpExpires").sort({ createdAt: -1 });
    
    console.log(`Found ${users.length} users`);
    
    // Format response để đảm bảo có đầy đủ field
    const formattedUsers = users.map(user => {
      const userObj = user.toObject();
      return {
        _id: String(userObj._id),
        ten_dang_nhap: userObj.ten_dang_nhap || "",
        ho_ten: userObj.ho_ten || "",
        email: userObj.email || "",
        so_dien_thoai: userObj.so_dien_thoai || "",
        dia_chi: userObj.dia_chi || "",
        trang_thai: "HOẠT ĐỘNG", // Có thể thêm field trang_thai vào User model sau
        createdAt: userObj.createdAt,
        updatedAt: userObj.updatedAt
      };
    });
    
    console.log(`Formatted ${formattedUsers.length} users`);
    if (formattedUsers.length > 0) {
      console.log("Sample user:", {
        _id: formattedUsers[0]._id,
        ho_ten: formattedUsers[0].ho_ten,
        email: formattedUsers[0].email,
        so_dien_thoai: formattedUsers[0].so_dien_thoai,
        dia_chi: formattedUsers[0].dia_chi
      });
    }
    console.log("==========================================\n");
    
    // Trả về cả 2 format để tương thích
    // Format 1: Wrapped (chuẩn mới)
    // Format 2: Array trực tiếp (tương thích với frontend cũ)
    res.json({
      success: true,
      data: formattedUsers,
      count: formattedUsers.length,
      // Thêm array trực tiếp để tương thích với frontend cũ
      users: formattedUsers
    });
  } catch (err) {
    console.error("❌ Error getting users:", err);
    res.status(500).json({ 
      success: false,
      error: err.message || "Lỗi server khi lấy danh sách user" 
    });
  }
};

// Lấy User theo ID
exports.getUserById = async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ message: "User không tồn tại" });
    res.json(user);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// Tạo User mới
exports.createUser = async (req, res) => {
  try {
    const newUser = new User(req.body);
    await newUser.save();
    res.json({ message: "User được tạo thành công", user: newUser });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// Cập nhật User theo ID
exports.updateUser = async (req, res) => {
  try {
    const updatedUser = await User.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
    });
    if (!updatedUser)
      return res.status(404).json({ message: "User không tồn tại" });
    res.json({ message: "Cập nhật thành công", user: updatedUser });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// Xóa User theo ID
exports.deleteUser = async (req, res) => {
  try {
    const deletedUser = await User.findByIdAndDelete(req.params.id);
    if (!deletedUser)
      return res.status(404).json({ message: "User không tồn tại" });
    res.json({ message: "Xóa User thành công" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
