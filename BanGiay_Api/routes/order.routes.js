const express = require("express");
const router = express.Router();
const OrderController = require("../controllers/order.controller");

// POST /api/order - Tạo đơn hàng mới
router.post("/", OrderController.createOrder);

// GET /api/order - Lấy danh sách đơn hàng của user
router.get("/", OrderController.getOrders);

// PUT /api/order/:orderId/status - Cập nhật trạng thái đơn hàng (phải đặt trước /:orderId)
router.put("/:orderId/status", OrderController.updateOrderStatus);

// PUT /api/order/:orderId/payment - Cập nhật trạng thái thanh toán (phải đặt trước /:orderId)
router.put("/:orderId/payment", OrderController.updatePaymentStatus);

// PUT /api/order/:orderId/cancel - Hủy đơn hàng (phải đặt trước /:orderId)
router.put("/:orderId/cancel", OrderController.cancelOrder);

// DELETE /api/order/:orderId - Xóa đơn hàng (phải đặt trước /:orderId)
router.delete("/:orderId", OrderController.deleteOrder);

// GET /api/order/:orderId - Lấy chi tiết đơn hàng (phải đặt cuối cùng)
router.get("/:orderId", OrderController.getOrderById);

module.exports = router;

