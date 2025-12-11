const express = require("express");
const router = express.Router();
const Notification = require("../models/Notification");

/**
 * Lấy tất cả thông báo của user
 * GET /api/notification/:user_id
 */
router.get("/:user_id", async (req, res) => {
  try {
    const { user_id } = req.params;
    const notifications = await Notification.find({ user_id }).sort({
      createdAt: -1,
    });
    res.status(200).json({
      success: true,
      data: notifications,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy thông báo",
      error: error.message,
    });
  }
});

/**
 * Đánh dấu thông báo là đã đọc
 * PUT /api/notification/:id/read
 */
router.put("/:id/read", async (req, res) => {
  try {
    const { id } = req.params;
    const notification = await Notification.findByIdAndUpdate(
      id,
      { is_read: true },
      { new: true }
    );
    res.status(200).json({
      success: true,
      data: notification,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: "Lỗi khi cập nhật thông báo",
      error: error.message,
    });
  }
});

/**
 * Xóa thông báo
 * DELETE /api/notification/:id
 */
router.delete("/:id", async (req, res) => {
  try {
    const { id } = req.params;
    await Notification.findByIdAndDelete(id);
    res.status(200).json({
      success: true,
      message: "Thông báo đã được xóa",
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: "Lỗi khi xóa thông báo",
      error: error.message,
    });
  }
});

/**
 * Lấy số thông báo chưa đọc
 * GET /api/notification/:user_id/unread/count
 */
router.get("/:user_id/unread/count", async (req, res) => {
  try {
    const { user_id } = req.params;
    const count = await Notification.countDocuments({
      user_id,
      is_read: false,
    });
    res.status(200).json({
      success: true,
      count,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy số thông báo chưa đọc",
      error: error.message,
    });
  }
});

module.exports = router;
