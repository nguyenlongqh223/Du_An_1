const Order = require("../models/Order");
const Cart = require("../models/Cart");
const Product = require("../models/Product");

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

    // Xây dựng query
    const query = {};
    if (user_id) {
      query.user_id = user_id;
    }
    if (trang_thai) {
      query.trang_thai = trang_thai;
    }

    const orders = await Order.find(query)
      .sort({ createdAt: -1 }) // Mới nhất trước
      .populate("items.san_pham_id", "ten_san_pham hinh_anh")
      .populate("user_id", "ho_ten email so_dien_thoai");

    res.json({
      success: true,
      data: orders,
    });
  } catch (err) {
    console.error("Lỗi khi lấy danh sách đơn hàng:", err);
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

    const order = await Order.findById(orderId).populate(
      "items.san_pham_id",
      "ten_san_pham hinh_anh gia_goc gia_khuyen_mai"
    );

    if (!order) {
      return res.status(404).json({
        success: false,
        message: "Đơn hàng không tồn tại",
      });
    }

    res.json({
      success: true,
      data: order,
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

    const order = await Order.findByIdAndUpdate(
      orderId,
      { trang_thai },
      { new: true, runValidators: true }
    );

    if (!order) {
      return res.status(404).json({
        success: false,
        message: "Đơn hàng không tồn tại",
      });
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

    order.trang_thai = "cancelled";
    await order.save();

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

