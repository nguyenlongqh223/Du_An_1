const Product = require("../models/Product");
const Category = require("../models/Category");

/**
 * Lấy tất cả các danh mục với số lượng sản phẩm
 * GET /api/category
 */
exports.getAllCategories = async (req, res) => {
  try {
    console.log("\n========== GET ALL CATEGORIES ==========");
    
    // Lấy tất cả categories từ database - exclude deleted categories
    const dbCategories = await Category.find({ 
      trang_thai: "active",
      is_deleted: { $ne: true }
    }).sort({ ten_danh_muc: 1 });
    
    // Nếu chưa có categories trong DB, tạo 3 categories mặc định
    if (dbCategories.length === 0) {
      console.log("No categories in DB, creating default categories...");
      const defaultCategories = [
        { ten_danh_muc: "Nam", danh_muc: "nam" },
        { ten_danh_muc: "Nữ", danh_muc: "nu" },
        { ten_danh_muc: "Unisex", danh_muc: "unisex" }
      ];
      
      for (const cat of defaultCategories) {
        try {
          await Category.create(cat);
        } catch (err) {
          // Ignore duplicate errors
          console.log(`Category ${cat.danh_muc} might already exist`);
        }
      }
      
      // Reload after creating defaults
      const reloaded = await Category.find({ trang_thai: "active" }).sort({ ten_danh_muc: 1 });
      dbCategories.push(...reloaded);
    }
    
    // Lấy số lượng sản phẩm cho mỗi danh mục
    const categoriesWithCount = await Promise.all(
      dbCategories.map(async (category) => {
        const count = await Product.countDocuments({ 
          danh_muc: category.danh_muc, 
          trang_thai: "active",
          is_deleted: { $ne: true }
        });
        
        return {
          _id: category._id,
          danh_muc: category.danh_muc,
          ten_danh_muc: category.ten_danh_muc,
          so_luong_san_pham: count
        };
      })
    );
    
    console.log(`Found ${categoriesWithCount.length} categories`);
    categoriesWithCount.forEach(cat => {
      console.log(`  - ${cat.ten_danh_muc}: ${cat.so_luong_san_pham} products`);
    });
    console.log("==========================================\n");
    
    res.json({
      success: true,
      data: categoriesWithCount,
      count: categoriesWithCount.length
    });
  } catch (error) {
    console.error("❌ Error in getAllCategories:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy danh sách danh mục",
      error: error.message
    });
  }
};

/**
 * Lấy thông tin chi tiết của một danh mục
 * GET /api/category/:categoryId hoặc /api/category/:danh_muc
 */
exports.getCategoryById = async (req, res) => {
  try {
    const { categoryId, danh_muc } = req.params;
    const identifier = categoryId || danh_muc;
    
    console.log(`\n========== GET CATEGORY: ${identifier} ==========`);
    
    let category;
    
    // Try to find by ID first (MongoDB ObjectId)
    if (identifier.match(/^[0-9a-fA-F]{24}$/)) {
      category = await Category.findById(identifier);
    }
    
    // If not found by ID, try by danh_muc
    if (!category) {
      const normalizedCategory = normalizeCategory(identifier);
      category = await Category.findOne({ danh_muc: normalizedCategory });
    }
    
    // If still not found, use old logic for backward compatibility
    if (!category) {
      const normalizedCategory = normalizeCategory(identifier);
      const validCategories = ["nam", "nu", "unisex"];
      if (!validCategories.includes(normalizedCategory)) {
        return res.status(400).json({
          success: false,
          message: `Danh mục không tồn tại`
        });
      }
      
      // Create a virtual category object
      category = {
        danh_muc: normalizedCategory,
        ten_danh_muc: getCategoryName(normalizedCategory),
      };
    }
    
    // Lấy số lượng sản phẩm
    const productCount = await Product.countDocuments({ 
      danh_muc: category.danh_muc, 
      trang_thai: "active" 
    });
    
    // Lấy một số sản phẩm mẫu (tùy chọn)
    const sampleProducts = await Product.find({ 
      danh_muc: category.danh_muc, 
      trang_thai: "active" 
    })
    .limit(5)
    .select("ten_san_pham gia_khuyen_mai hinh_anh danh_gia")
    .sort({ createdAt: -1 });
    
    const categoryInfo = {
      _id: category._id || null,
      danh_muc: category.danh_muc,
      ten_danh_muc: category.ten_danh_muc || category.ten_danh_muc,
      so_luong_san_pham: productCount,
      sample_products: sampleProducts
    };
    
    console.log(`Category: ${categoryInfo.ten_danh_muc}, Products: ${productCount}`);
    console.log("==========================================\n");
    
    res.json({
      success: true,
      data: categoryInfo
    });
  } catch (error) {
    console.error("❌ Error in getCategoryById:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi lấy thông tin danh mục",
      error: error.message
    });
  }
};

/**
 * Chuẩn hóa danh mục
 */
const normalizeCategory = (rawCategory) => {
  if (!rawCategory) return "unisex";
  
  const value = rawCategory.toString().trim().toLowerCase();
  
  if (["nu", "nữ", "female", "women", "girl", "lady", "ladies"].includes(value)) {
    return "nu";
  }
  if (["nam", "male", "man", "men", "boy"].includes(value)) {
    return "nam";
  }
  if (["unisex", "hottrend", "hot trend", "trend"].includes(value)) {
    return "unisex";
  }
  
  return value;
};

/**
 * Lấy tên hiển thị của danh mục
 */
const getCategoryName = (category) => {
  const categoryNames = {
    "nam": "Nam",
    "nu": "Nữ",
    "unisex": "Unisex"
  };
  return categoryNames[category] || category;
};

/**
 * Tạo danh mục mới
 * POST /api/category
 */
exports.createCategory = async (req, res) => {
  try {
    const { ten_danh_muc } = req.body;

    if (!ten_danh_muc || !ten_danh_muc.trim()) {
      return res.status(400).json({
        success: false,
        message: "Tên danh mục không được để trống",
      });
    }

    // Tạo danh_muc từ ten_danh_muc (lowercase, no spaces, no special chars)
    const danh_muc = ten_danh_muc
      .toLowerCase()
      .trim()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "") // Remove diacritics
      .replace(/[^a-z0-9]/g, ""); // Remove special chars

    // Kiểm tra trùng lặp
    const existing = await Category.findOne({
      $or: [
        { ten_danh_muc: ten_danh_muc.trim() },
        { danh_muc: danh_muc }
      ]
    });

    if (existing) {
      return res.status(400).json({
        success: false,
        message: "Tên danh mục đã tồn tại",
      });
    }

    const newCategory = await Category.create({
      ten_danh_muc: ten_danh_muc.trim(),
      danh_muc: danh_muc,
      trang_thai: "active",
    });

    console.log("✅ Category created:", newCategory.ten_danh_muc);

    res.status(201).json({
      success: true,
      message: "Đã tạo danh mục thành công",
      data: newCategory,
    });
  } catch (error) {
    console.error("❌ Error creating category:", error);
    if (error.code === 11000) {
      return res.status(400).json({
        success: false,
        message: "Tên danh mục đã tồn tại",
      });
    }
    res.status(500).json({
      success: false,
      message: "Lỗi khi tạo danh mục",
      error: error.message,
    });
  }
};

/**
 * Cập nhật danh mục
 * PUT /api/category/:categoryId
 */
exports.updateCategory = async (req, res) => {
  try {
    const { categoryId } = req.params;
    const { ten_danh_muc } = req.body;

    if (!ten_danh_muc || !ten_danh_muc.trim()) {
      return res.status(400).json({
        success: false,
        message: "Tên danh mục không được để trống",
      });
    }

    const category = await Category.findById(categoryId);
    if (!category) {
      return res.status(404).json({
        success: false,
        message: "Danh mục không tồn tại",
      });
    }

    // Tạo danh_muc mới từ ten_danh_muc
    const newDanhMuc = ten_danh_muc
      .toLowerCase()
      .trim()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]/g, "");

    // Kiểm tra trùng lặp (trừ chính nó)
    const existing = await Category.findOne({
      _id: { $ne: categoryId },
      $or: [
        { ten_danh_muc: ten_danh_muc.trim() },
        { danh_muc: newDanhMuc }
      ]
    });

    if (existing) {
      return res.status(400).json({
        success: false,
        message: "Tên danh mục đã tồn tại",
      });
    }

    category.ten_danh_muc = ten_danh_muc.trim();
    category.danh_muc = newDanhMuc;
    await category.save();

    console.log("✅ Category updated:", category.ten_danh_muc);

    res.json({
      success: true,
      message: "Đã cập nhật danh mục thành công",
      data: category,
    });
  } catch (error) {
    console.error("❌ Error updating category:", error);
    if (error.code === 11000) {
      return res.status(400).json({
        success: false,
        message: "Tên danh mục đã tồn tại",
      });
    }
    res.status(500).json({
      success: false,
      message: "Lỗi khi cập nhật danh mục",
      error: error.message,
    });
  }
};

/**
 * Xóa danh mục
 * DELETE /api/category/:categoryId
 */
exports.deleteCategory = async (req, res) => {
  try {
    const { categoryId } = req.params;

    const category = await Category.findById(categoryId);
    if (!category) {
      return res.status(404).json({
        success: false,
        message: "Danh mục không tồn tại",
      });
    }

    // Soft delete - ẩn danh mục, giữ trong MongoDB
    category.is_deleted = true;
    category.deleted_at = new Date();
    await category.save();

    console.log("✅ Category soft deleted (hidden):", category.ten_danh_muc);

    res.json({
      success: true,
      message: "Đã ẩn danh mục thành công (dữ liệu vẫn được giữ trong database)",
    });
  } catch (error) {
    console.error("❌ Error deleting category:", error);
    res.status(500).json({
      success: false,
      message: "Lỗi khi xóa danh mục",
      error: error.message,
    });
  }
};

