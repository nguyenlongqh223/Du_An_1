const express = require("express");
const router = express.Router();
const CartController = require("../controllers/cart.controller");

/**
 * Cart Routes
 * Base path: /api/cart
 */

// Middleware logging cho tất cả cart routes
router.use((req, res, next) => {
  console.log(`[CART] ${req.method} ${req.path}`);
  if (Object.keys(req.query).length > 0) {
    console.log(`[CART] Query:`, req.query);
  }
  if (req.body && Object.keys(req.body).length > 0) {
    console.log(`[CART] Body:`, req.body);
  }
  next();
});

// POST /api/cart - Thêm sản phẩm vào giỏ hàng
// Body: { user_id, product_id, size, quantity }
router.post("/", CartController.addToCart);

// GET /api/cart?user_id=xxx - Lấy giỏ hàng của user
router.get("/", CartController.getCart);

// PUT /api/cart/item - Cập nhật số lượng sản phẩm trong giỏ hàng
// Body: { user_id, item_id, quantity }
router.put("/item", CartController.updateCartItem);

// DELETE /api/cart/item - Xóa sản phẩm khỏi giỏ hàng
// Body: { user_id, item_id }
router.delete("/item", CartController.removeFromCart);

// DELETE /api/cart - Xóa toàn bộ giỏ hàng
// Body: { user_id }
router.delete("/", CartController.clearCart);

module.exports = router;
