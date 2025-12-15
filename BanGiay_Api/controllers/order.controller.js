const Order = require("../models/Order");
const Cart = require("../models/Cart");
const Product = require("../models/Product");
const Notification = require("../models/Notification");
const User = require("../models/User");

// Tạo đơn hàng mới từ giỏ hàng
exports.createOrder = async (req, res) => {
  try {
    const { user_id, items, tong_tien, dia_chi_giao_hang, so_dien_thoai, ghi_chu } = req.body;

    // Validation
    if (!user_id || !items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({
        success: false,
        message: "Thiếu thông tin bắt buộc: user_id, items",
      });
    }

    if (!tong_tien || tong_tien <= 0) {
      return res.status(400).json({
        success: false,
        message: "Tổng tiền phải lớn hơn 0",
      });
    }

    // Tính lại tổng tiền từ items để đảm bảo chính xác
    let calculatedTotal = 0;
    for (const item of items) {
      if (!item.gia || !item.so_luong) {
        return res.status(400).json({
          success: false,
          message: "Thiếu thông tin sản phẩm: gia, so_luong",
        });
      }
      calculatedTotal += item.gia * item.so_luong;
    }

    // Tạo đơn hàng mới
    const newOrder = new Order({
      user_id,
      items,
      tong_tien: calculatedTotal,
      trang_thai: "pending", // Mặc định là chờ xác nhận
      dia_chi_giao_hang: dia_chi_giao_hang || "",
      so_dien_thoai: so_dien_thoai || "",
      ghi_chu: ghi_chu || "",
    });

    await newOrder.save();
    console.log("✅ Order created successfully! Order ID:", newOrder._id);

    // Xóa các items đã thanh toán khỏi giỏ hàng (không xóa toàn bộ cart)
    const mongoose = require("mongoose");
    const userIdObjectId = mongoose.Types.ObjectId.isValid(user_id) ? new mongoose.Types.ObjectId(user_id) : user_id;
    const cart = await Cart.findOne({ user_id: userIdObjectId });
    
    if (cart && cart.items && cart.items.length > 0) {
      console.log("Cart before removing items:", cart.items.length);
      
      // Tạo map để đếm số lượng items cần xóa cho mỗi product_id + size
      const itemsToRemove = new Map();
      items.forEach(orderItem => {
        const key = `${orderItem.san_pham_id}_${orderItem.kich_thuoc}`;
        const currentCount = itemsToRemove.get(key) || 0;
        itemsToRemove.set(key, currentCount + orderItem.so_luong);
      });
      
      console.log("Items to remove map:", Array.from(itemsToRemove.entries()));
      
      // Xóa items theo thứ tự, đảm bảo xóa đúng số lượng
      const itemsToKeep = [];
      const removalCount = new Map();
      
      cart.items.forEach(cartItem => {
        const key = `${cartItem.san_pham_id.toString()}_${cartItem.kich_thuoc}`;
        const needToRemove = itemsToRemove.get(key) || 0;
        const alreadyRemoved = removalCount.get(key) || 0;
        
        if (needToRemove > 0 && alreadyRemoved < needToRemove) {
          // Cần xóa item này
          removalCount.set(key, alreadyRemoved + 1);
          console.log(`Removing item: product=${cartItem.san_pham_id}, size=${cartItem.kich_thuoc}`);
        } else {
          // Giữ lại item này
          itemsToKeep.push(cartItem);
        }
      });
      
      const removedCount = cart.items.length - itemsToKeep.length;
      console.log(`Removed ${removedCount} items from cart. Remaining: ${itemsToKeep.length}`);
      
      cart.items = itemsToKeep;
      
      if (cart.items.length > 0) {
        await cart.save();
        console.log("✅ Cart updated, remaining items:", cart.items.length);
      } else {
        // Nếu cart rỗng, xóa cart
        await Cart.findByIdAndDelete(cart._id);
        console.log("✅ Cart deleted (empty after checkout)");
      }
    } else {
      console.log("⚠️ Cart not found or empty, nothing to remove");
    }

    res.status(201).json({
      success: true,
      message: "Đơn hàng đã được tạo thành công",
      data: newOrder,
    });
  } catch (err) {
    console.error("Lỗi khi tạo đơn hàng:", err);
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi tạo đơn hàng",
    });
  }
};

// Lấy danh sách đơn hàng của user (hoặc tất cả nếu không có user_id - cho admin)
exports.getOrders = async (req, res) => {
  try {
    const { user_id, trang_thai } = req.query;

    console.log("=== GET ORDERS ===");
    console.log("Query params:", { user_id, trang_thai });

    // Xây dựng query
    const query = {};
    if (user_id) {
      // Convert string user_id to ObjectId
      const mongoose = require("mongoose");
      if (!mongoose.Types.ObjectId.isValid(user_id)) {
        console.error("❌ Invalid user_id format:", user_id);
        return res.status(400).json({
          success: false,
          message: "user_id không hợp lệ",
        });
      }
      query.user_id = new mongoose.Types.ObjectId(user_id);
      console.log("Converted user_id to ObjectId:", query.user_id.toString());
    }
    if (trang_thai) {
      query.trang_thai = trang_thai;
    }

    console.log("MongoDB query:", JSON.stringify(query));

    const orders = await Order.find(query)
      .sort({ createdAt: -1 }) // Mới nhất trước
      .populate("items.san_pham_id", "ten_san_pham hinh_anh gia_goc gia_khuyen_mai")
      .populate("user_id", "ho_ten email so_dien_thoai ten_dang_nhap");

    console.log(`✅ Found ${orders.length} orders`);

    // Format orders để khớp với OrderResponse model
    // Dùng JSON.stringify/parse để đảm bảo ObjectId được convert thành string
    const formattedOrders = orders.map(order => {
      // Convert to plain object và serialize để convert ObjectId
      const orderStr = JSON.stringify(order);
      const orderObj = JSON.parse(orderStr);
      
      // Format items - QUAN TRỌNG: san_pham_id phải là string, không phải object
      const formattedItems = (orderObj.items || []).map(item => {
        // Xử lý san_pham_id - sau khi JSON.parse, nếu là object thì sẽ có _id
        let sanPhamId = "";
        if (item.san_pham_id) {
          if (typeof item.san_pham_id === 'object' && item.san_pham_id._id) {
            sanPhamId = String(item.san_pham_id._id);
          } else if (typeof item.san_pham_id === 'object') {
            sanPhamId = String(item.san_pham_id);
          } else {
            sanPhamId = String(item.san_pham_id);
          }
        }

        return {
          san_pham_id: sanPhamId, // PHẢI là string
          ten_san_pham: item.ten_san_pham || "",
          so_luong: item.so_luong || 0,
          kich_thuoc: item.kich_thuoc || "",
          gia: item.gia || 0,
        };
      });

      // Xử lý user_id - giữ lại thông tin user đã populate
      let userIdStr = "";
      let userInfo = null;
      
      if (orderObj.user_id) {
        if (typeof orderObj.user_id === 'object' && orderObj.user_id._id) {
          userIdStr = String(orderObj.user_id._id);
          // Giữ lại thông tin user đã populate
          userInfo = {
            _id: String(orderObj.user_id._id),
            ho_ten: orderObj.user_id.ho_ten || "",
            email: orderObj.user_id.email || "",
            so_dien_thoai: orderObj.user_id.so_dien_thoai || "",
            ten_dang_nhap: orderObj.user_id.ten_dang_nhap || ""
          };
        } else if (typeof orderObj.user_id === 'object') {
          userIdStr = String(orderObj.user_id);
        } else {
          userIdStr = String(orderObj.user_id);
        }
      }

      return {
        _id: String(orderObj._id || ""),
        user_id: userIdStr,
        user: userInfo, // Thêm thông tin user đã populate
        items: formattedItems,
        tong_tien: orderObj.tong_tien || 0,
        trang_thai: orderObj.trang_thai || "pending",
        dia_chi_giao_hang: orderObj.dia_chi_giao_hang || "",
        so_dien_thoai: orderObj.so_dien_thoai || "",
        ghi_chu: orderObj.ghi_chu || "",
        createdAt: orderObj.createdAt,
        updatedAt: orderObj.updatedAt,
      };
    });

    console.log(`✅ Formatted ${formattedOrders.length} orders`);
    if (formattedOrders.length > 0 && formattedOrders[0].items.length > 0) {
      console.log("Sample item san_pham_id type:", typeof formattedOrders[0].items[0].san_pham_id);
      console.log("Sample item san_pham_id value:", formattedOrders[0].items[0].san_pham_id);
    }

    res.json({
      success: true,
      data: formattedOrders,
      count: formattedOrders.length,
    });
  } catch (err) {
    console.error("❌ Lỗi khi lấy danh sách đơn hàng:", err);
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi lấy danh sách đơn hàng",
    });
  }
};

// Lấy chi tiết đơn hàng
exports.getOrderById = async (req, res) => {
  try {
    const { orderId } = req.params;

    console.log("=== GET ORDER BY ID ===");
    console.log("Order ID:", orderId);

    // Dùng lean() để lấy plain object, không phải Mongoose document
    const order = await Order.findById(orderId)
      .lean()
      .populate("items.san_pham_id", "ten_san_pham hinh_anh gia_goc gia_khuyen_mai")
      .populate("user_id", "ho_ten email so_dien_thoai ten_dang_nhap");

    if (!order) {
      return res.status(404).json({
        success: false,
        message: "Đơn hàng không tồn tại",
      });
    }

    // Format items - QUAN TRỌNG: san_pham_id phải là string, không phải object
    // Sau khi populate, san_pham_id là object với _id bên trong
    const formattedItems = (order.items || []).map(item => {
      // Xử lý san_pham_id - nếu là object (đã populate), lấy _id
      let sanPhamId = "";
      if (item.san_pham_id) {
        // Nếu là object (đã populate), lấy _id
        if (typeof item.san_pham_id === 'object' && item.san_pham_id._id) {
          sanPhamId = String(item.san_pham_id._id);
        } else if (typeof item.san_pham_id === 'object') {
          // Nếu là ObjectId trực tiếp
          sanPhamId = String(item.san_pham_id);
        } else {
          // Nếu đã là string
          sanPhamId = String(item.san_pham_id);
        }
      }

      return {
        san_pham_id: sanPhamId, // PHẢI là string
        ten_san_pham: item.ten_san_pham || "",
        so_luong: item.so_luong || 0,
        kich_thuoc: item.kich_thuoc || "",
        gia: item.gia || 0,
      };
    });

    // Xử lý user_id - giữ lại thông tin user đã populate
    let userIdStr = "";
    let userInfo = null;
    
    if (order.user_id) {
      if (typeof order.user_id === 'object' && order.user_id._id) {
        userIdStr = String(order.user_id._id);
        // Giữ lại thông tin user đã populate
        userInfo = {
          _id: String(order.user_id._id),
          ho_ten: order.user_id.ho_ten || "",
          email: order.user_id.email || "",
          so_dien_thoai: order.user_id.so_dien_thoai || "",
          ten_dang_nhap: order.user_id.ten_dang_nhap || ""
        };
      } else if (typeof order.user_id === 'object') {
        userIdStr = String(order.user_id);
      } else {
        userIdStr = String(order.user_id);
      }
    }

    // Dùng JSON serialize/deserialize để convert ObjectId thành string cho các trường khác
    const orderStr = JSON.stringify(order);
    const orderObj = JSON.parse(orderStr);

    const formattedOrder = {
      _id: String(orderObj._id || ""),
      user_id: userIdStr,
      user: userInfo, // Thêm thông tin user đã populate
      items: formattedItems,
      tong_tien: orderObj.tong_tien || 0,
      trang_thai: orderObj.trang_thai || "pending",
      dia_chi_giao_hang: orderObj.dia_chi_giao_hang || "",
      so_dien_thoai: orderObj.so_dien_thoai || "",
      ghi_chu: orderObj.ghi_chu || "",
      createdAt: orderObj.createdAt,
      updatedAt: orderObj.updatedAt,
    };

    console.log("✅ Order detail formatted:", {
      _id: formattedOrder._id,
      items_count: formattedOrder.items.length,
      trang_thai: formattedOrder.trang_thai,
    });
    if (formattedOrder.items.length > 0) {
      console.log("Sample item san_pham_id type:", typeof formattedOrder.items[0].san_pham_id);
      console.log("Sample item san_pham_id value:", formattedOrder.items[0].san_pham_id);
    }

    res.json({
      success: true,
      data: formattedOrder,
    });
  } catch (err) {
    console.error("Lỗi khi lấy chi tiết đơn hàng:", err);
    if (err.name === "CastError") {
      return res.status(400).json({
        success: false,
        message: "ID đơn hàng không hợp lệ",
      });
    }
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi lấy chi tiết đơn hàng",
    });
  }
};

// Cập nhật trạng thái đơn hàng
exports.updateOrderStatus = async (req, res) => {
  try {
    const { orderId } = req.params;
    const { trang_thai } = req.body;

    if (!trang_thai) {
      return res.status(400).json({
        success: false,
        message: "Thiếu trạng thái",
      });
    }

    const validStatuses = ["pending", "confirmed", "shipping", "delivered", "cancelled"];
    if (!validStatuses.includes(trang_thai)) {
      return res.status(400).json({
        success: false,
        message: "Trạng thái không hợp lệ. Các trạng thái hợp lệ: " + validStatuses.join(", "),
      });
    }

    // Lấy đơn hàng cũ để so sánh trạng thái
    const oldOrder = await Order.findById(orderId).populate("user_id", "ho_ten email");
    if (!oldOrder) {
      return res.status(404).json({
        success: false,
        message: "Đơn hàng không tồn tại",
      });
    }

    // Không cho phép cập nhật trạng thái nếu đơn hàng đã bị hủy
    if (oldOrder.trang_thai === "cancelled") {
      return res.status(400).json({
        success: false,
        message: "Không thể cập nhật trạng thái đơn hàng đã bị hủy",
      });
    }

    const oldStatus = oldOrder.trang_thai;

    // Cập nhật trạng thái
    const order = await Order.findByIdAndUpdate(
      orderId,
      { trang_thai },
      { new: true, runValidators: true }
    );

    // Tạo thông báo cho user về thay đổi trạng thái đơn hàng
    const statusMessages = {
      pending: "Chờ xác nhận",
      confirmed: "Đã xác nhận",
      shipping: "Đang giao hàng",
      delivered: "Đã giao hàng",
      cancelled: "Đã hủy",
    };

    const statusText = statusMessages[trang_thai] || trang_thai;
    const title = "Cập nhật đơn hàng";
    const message = `ĐƠN HÀNG CỦA BẠN ĐÃ ĐƯỢC ${statusText.toUpperCase()}`;

    try {
      // Đảm bảo user_id là ObjectId hoặc string hợp lệ
      let userId = order.user_id;
      if (userId && typeof userId === "object" && userId._id) {
        userId = userId._id;
      } else if (userId && typeof userId === "object") {
        userId = userId.toString();
      }

      const notification = new Notification({
        user_id: userId,
        title,
        message,
        type: "order",
        link: `/order/${orderId}`,
        is_read: false,
      });
      await notification.save();
      console.log("✅ Notification created for order status update:", {
        user_id: userId,
        order_id: orderId,
        new_status: trang_thai,
      });
    } catch (notifErr) {
      console.error("⚠️ Failed to create notification:", notifErr);
      // Không fail request nếu tạo thông báo lỗi
    }

    res.json({
      success: true,
      message: "Đã cập nhật trạng thái đơn hàng",
      data: order,
    });
  } catch (err) {
    console.error("Lỗi khi cập nhật trạng thái đơn hàng:", err);
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi cập nhật trạng thái đơn hàng",
    });
  }
};

// Hủy đơn hàng
exports.cancelOrder = async (req, res) => {
  try {
    const { orderId } = req.params;
    const { ly_do_huy } = req.body; // Lý do hủy đơn hàng

    const order = await Order.findById(orderId);

    if (!order) {
      return res.status(404).json({
        success: false,
        message: "Đơn hàng không tồn tại",
      });
    }

    // Chỉ cho phép hủy đơn hàng ở trạng thái pending hoặc confirmed
    if (order.trang_thai === "delivered") {
      return res.status(400).json({
        success: false,
        message: "Không thể hủy đơn hàng đã giao",
      });
    }

    if (order.trang_thai === "cancelled") {
      return res.status(400).json({
        success: false,
        message: "Đơn hàng đã được hủy trước đó",
      });
    }

    // Kiểm tra lý do hủy
    if (!ly_do_huy || ly_do_huy.trim() === "") {
      return res.status(400).json({
        success: false,
        message: "Vui lòng nhập lý do hủy đơn hàng",
      });
    }

    order.trang_thai = "cancelled";
    order.ly_do_huy = ly_do_huy.trim();
    await order.save();

    console.log("✅ Order cancelled:", {
      order_id: orderId,
      reason: ly_do_huy,
    });

    res.json({
      success: true,
      message: "Đã hủy đơn hàng",
      data: order,
    });
  } catch (err) {
    console.error("Lỗi khi hủy đơn hàng:", err);
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi hủy đơn hàng",
    });
  }
};

/**
 * Xóa đơn hàng (Admin only)
 * DELETE /api/order/:orderId
 */
exports.deleteOrder = async (req, res) => {
  try {
    const { orderId } = req.params;

    console.log(`\n========== DELETE ORDER ==========`);
    console.log(`Order ID: ${orderId}`);

    const order = await Order.findById(orderId);

    if (!order) {
      console.log(`❌ Order not found: ${orderId}`);
      return res.status(404).json({
        success: false,
        message: "Đơn hàng không tồn tại",
      });
    }

    console.log(`✅ Order found: ${order._id}, Status: ${order.trang_thai}`);

    // Xóa đơn hàng
    await Order.findByIdAndDelete(orderId);

    console.log(`✅ Order deleted: ${orderId}`);
    console.log("==========================================\n");

    res.json({
      success: true,
      message: "Đã xóa đơn hàng thành công",
      data: {
        orderId: orderId,
        deletedAt: new Date(),
      },
    });
  } catch (err) {
    console.error("❌ Lỗi khi xóa đơn hàng:", err);
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi xóa đơn hàng",
    });
  }
};

