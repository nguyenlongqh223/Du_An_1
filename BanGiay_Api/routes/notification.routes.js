const express = require("express");
const router = express.Router();
const Notification = require("../models/Notification");

/**
 * Test endpoint - Kiểm tra route có hoạt động không
 * GET /api/notification/test
 */
router.get("/test", (req, res) => {
  res.status(200).json({
    success: true,
    message: "Notification API endpoint is working!",
    timestamp: new Date().toISOString()
  });
});

/**
 * Lấy số thông báo chưa đọc
 * GET /api/notification/:user_id/unread/count
 * 
 * LƯU Ý: Route này PHẢI được đặt TRƯỚC route /:user_id để tránh conflict
 * Express sẽ match route cụ thể trước route dynamic
 */
router.get("/:user_id/unread/count", async (req, res) => {
  try {
    const { user_id } = req.params;
    console.log(`\n========== GET UNREAD COUNT ==========`);
    console.log(`User ID: ${user_id}`);
    
    // Convert user_id to ObjectId nếu cần
    const mongoose = require("mongoose");
    let userIdQuery = user_id;
    if (mongoose.Types.ObjectId.isValid(user_id)) {
      userIdQuery = new mongoose.Types.ObjectId(user_id);
    }
    
    // Đếm thông báo chưa đọc
    // Ưu tiên field da_doc, nếu không có thì dùng is_read
    const count = await Notification.countDocuments({
      user_id: userIdQuery,
      $or: [
        { da_doc: false },
        { is_read: false }
      ]
    });
    
    console.log(`Unread count: ${count}`);
    console.log("==========================================\n");
    
    res.status(200).json({
      success: true,
      count,
    });
  } catch (error) {
    console.error("❌ Error getting unread count:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy số thông báo chưa đọc",
      error: error.message,
    });
  }
});

/**
 * Lấy tất cả thông báo của user
 * GET /api/notification/:user_id
 * 
 * LƯU Ý: Route này PHẢI được đặt SAU route /:user_id/unread/count
 * để route cụ thể được match trước route dynamic
 */
router.get("/:user_id", async (req, res) => {
  try {
    const { user_id } = req.params;
    console.log(`\n========== GET NOTIFICATIONS ==========`);
    console.log(`Request URL: ${req.originalUrl}`);
    console.log(`Request method: ${req.method}`);
    console.log(`User ID: ${user_id}`);
    console.log(`User ID type: ${typeof user_id}`);
    console.log(`User ID length: ${user_id ? user_id.length : 0}`);
    
    // Convert user_id to ObjectId nếu cần
    const mongoose = require("mongoose");
    let userIdQuery = user_id;
    if (mongoose.Types.ObjectId.isValid(user_id)) {
      userIdQuery = new mongoose.Types.ObjectId(user_id);
    }
    
    const notifications = await Notification.find({ user_id: userIdQuery })
      .sort({ createdAt: -1 });
    
    console.log(`Found ${notifications.length} notifications`);
    
    // Format response để đảm bảo tương thích
    const formattedNotifications = notifications.map(notif => {
      // Convert to plain object
      const notifObj = notif.toObject();
      
      // Debug: Log raw data
      if (notifications.length > 0 && notifications.indexOf(notif) === 0) {
        console.log("Sample raw notification:", JSON.stringify(notifObj, null, 2));
      }
      
      // Đảm bảo có đầy đủ field cho Android app
      const formatted = {
        _id: String(notifObj._id),
        user_id: String(notifObj.user_id),
        // Ưu tiên format tiếng Việt
        tieu_de: notifObj.tieu_de || notifObj.title || "Thông báo",
        noi_dung: notifObj.noi_dung || notifObj.message || "",
        loai: notifObj.loai || notifObj.type || "notification",
        da_doc: notifObj.da_doc !== undefined ? notifObj.da_doc : (notifObj.is_read || false),
        duong_dan: notifObj.duong_dan || notifObj.link || "",
        metadata: notifObj.metadata || {},
        createdAt: notifObj.createdAt,
        updatedAt: notifObj.updatedAt,
      };
      
      // Debug: Log formatted data
      if (notifications.length > 0 && notifications.indexOf(notif) === 0) {
        console.log("Sample formatted notification:", JSON.stringify(formatted, null, 2));
      }
      
      return formatted;
    });
    
    console.log(`Formatted ${formattedNotifications.length} notifications`);
    console.log("==========================================\n");
    
    // Trả về cả 2 format để tương thích
    // Format 1: Wrapped trong object (chuẩn)
    // Format 2: Array trực tiếp (nếu Android app expect)
    const response = {
      success: true,
      data: formattedNotifications,
      count: formattedNotifications.length,
      // Thêm array trực tiếp để tương thích với Android app
      notifications: formattedNotifications
    };
    
    // Log response để debug
    if (formattedNotifications.length > 0) {
      console.log("Response sample:", JSON.stringify(response.data[0], null, 2));
    }
    
    // Kiểm tra query parameter để trả về format phù hợp
    const { format } = req.query;
    if (format === 'array') {
      // Trả về array trực tiếp nếu Android app yêu cầu
      console.log("Returning array format for Android app");
      return res.status(200).json(formattedNotifications);
    }
    
    res.status(200).json(response);
  } catch (error) {
    console.error("❌ Error getting notifications:", error);
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
    console.log(`\n========== MARK NOTIFICATION AS READ ==========`);
    console.log(`Notification ID: ${id}`);
    
    const notification = await Notification.findByIdAndUpdate(
      id,
      { 
        da_doc: true,
        is_read: true // Cập nhật cả 2 field để tương thích
      },
      { new: true }
    );
    
    if (!notification) {
      console.log(`❌ Notification not found: ${id}`);
      return res.status(404).json({
        success: false,
        message: "Thông báo không tồn tại",
      });
    }
    
    console.log(`✅ Notification marked as read`);
    console.log("==========================================\n");
    
    // Format response
    const notifObj = notification.toObject();
    res.status(200).json({
      success: true,
      data: {
        _id: String(notifObj._id),
        user_id: String(notifObj.user_id),
        tieu_de: notifObj.tieu_de || notifObj.title || "",
        noi_dung: notifObj.noi_dung || notifObj.message || "",
        loai: notifObj.loai || notifObj.type || "notification",
        da_doc: true,
        duong_dan: notifObj.duong_dan || notifObj.link || "",
        metadata: notifObj.metadata || {},
        createdAt: notifObj.createdAt,
        updatedAt: notifObj.updatedAt,
      },
    });
  } catch (error) {
    console.error("❌ Error updating notification:", error);
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

module.exports = router;
