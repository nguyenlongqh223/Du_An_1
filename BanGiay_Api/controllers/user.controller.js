const User = require("../models/User");

// Lấy tất cả User
exports.getAllUsers = async (req, res) => {
  try {
    const users = await User.find();
    res.json(users);
  } catch (err) {
    res.status(500).json({ error: err.message });
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
