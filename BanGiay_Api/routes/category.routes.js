const express = require("express");
const router = express.Router();
const CategoryController = require("../controllers/category.controller");

// Category routes
router.get("/", CategoryController.getAllCategories);
router.post("/", CategoryController.createCategory);
router.get("/:categoryId", CategoryController.getCategoryById);
router.put("/:categoryId", CategoryController.updateCategory);
router.delete("/:categoryId", CategoryController.deleteCategory);

// Keep old route for backward compatibility
router.get("/:danh_muc", CategoryController.getCategoryById);

module.exports = router;

