const express = require("express");
const router = express.Router();
const ProductController = require("../controllers/product.controller");

// CRUD Sản phẩm
// Lưu ý: Các route cụ thể phải đặt trước route /:id để tránh conflict
router.get("/", ProductController.getAllProducts);
router.get("/best-selling", ProductController.getBestSellingProducts);
router.get("/newest", ProductController.getNewestProducts);
router.get("/hot-trend", ProductController.getHotTrendProducts);
router.get("/category/:danh_muc", ProductController.getProductsByCategory);
router.post("/", ProductController.createProduct);
router.get("/:id", ProductController.getProductById);
router.put("/:id", ProductController.updateProduct);
router.put("/:id/stock", ProductController.updateStock);
router.delete("/:id", ProductController.deleteProduct);

module.exports = router;


