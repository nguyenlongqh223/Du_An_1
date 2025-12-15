const express = require("express");
const router = express.Router();
const CartController = require("../controllers/cart.controller");

/**
 * Cart Routes
 * Base path: /api/cart
 */

// Middleware logging cho tất cả cart routes
router.use((req, res, next) => {
  console.log(`[CART ROUTE] ${req.method} ${req.path}`);
  console.log(`[CART ROUTE] Query:`, req.query);
  if (req.body && Object.keys(req.body).length > 0) {
    console.log(`[CART ROUTE] Body:`, JSON.stringify(req.body, null, 2));
  }
  next();
});

// POST /api/cart - Thêm sản phẩm vào giỏ hàng
router.post("/", CartController.addToCart);

// GET /api/cart?user_id=xxx - Lấy giỏ hàng của user
router.get("/", CartController.getCart);

// PUT /api/cart/item - Cập nhật số lượng sản phẩm trong giỏ hàng
router.put("/item", CartController.updateCartItem);

// DELETE /api/cart/item - Xóa sản phẩm khỏi giỏ hàng
router.delete("/item", CartController.removeFromCart);

// DELETE /api/cart - Xóa toàn bộ giỏ hàng
router.delete("/", CartController.clearCart);

// DELETE /api/cart/:cartId - Xóa giỏ hàng theo ID (Admin only)
router.delete("/:cartId", CartController.deleteCartById);

module.exports = router;

