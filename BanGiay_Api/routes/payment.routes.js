const express = require("express");
const router = express.Router();
const paymentController = require("../controllers/payment.controller");

// Tạo payment mới
router.post("/", paymentController.createPayment);

// Lấy tất cả payments
router.get("/", paymentController.getAllPayments);

// Lấy payments theo user_id
router.get("/user/:user_id", paymentController.getPaymentsByUserId);

// Lấy payments theo email
router.get("/email/:email", paymentController.getPaymentsByEmail);

// Cập nhật trạng thái payment
router.put("/:id/status", paymentController.updatePaymentStatus);

module.exports = router;

