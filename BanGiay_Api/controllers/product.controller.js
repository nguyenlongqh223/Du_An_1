const Product = require("../models/Product");
const Notification = require("../models/Notification");
const User = require("../models/User");

// Helper function để format giá theo định dạng Việt Nam
const formatPrice = (price) => {
  if (!price || price <= 0) return "0₫";
  return new Intl.NumberFormat('vi-VN').format(price) + '₫';
};

// Helper function để format giá với dấu chấm phân cách (cho Android)
const formatPriceWithDot = (price) => {
  if (!price || price <= 0) return "0₫";
  return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.') + '₫';
};

// Helper function để lấy giá hiển thị (ưu tiên giá khuyến mãi)
const getDisplayPrice = (product) => {
  return product.gia_khuyen_mai || product.gia_goc || 0;
};

// Helper function để format sản phẩm với giá đã format
const formatProduct = (product) => {
  const productObj = product.toObject ? product.toObject() : product;
  const displayPrice = getDisplayPrice(productObj);
  
  return {
    ...productObj,
    // Đảm bảo giá luôn có giá trị
    gia_goc: productObj.gia_goc || 0,
    gia_khuyen_mai: productObj.gia_khuyen_mai || productObj.gia_goc || 0,
    // Thêm field giá đã format để mobile app dễ hiển thị
    gia_hien_tai: displayPrice,
    gia_formatted: formatPrice(displayPrice),
    gia_formatted_dot: formatPriceWithDot(displayPrice),
    // Thêm field giá gốc đã format (nếu có khuyến mãi)
    gia_goc_formatted: productObj.gia_goc ? formatPrice(productObj.gia_goc) : null,
    gia_goc_formatted_dot: productObj.gia_goc ? formatPriceWithDot(productObj.gia_goc) : null,
  };
};

// Chuẩn hóa và validate danh mục để đồng nhất giữa web admin và app mobile
const normalizeCategory = (rawCategory) => {
  if (!rawCategory) return "unisex";

  const value = rawCategory.toString().trim().toLowerCase();

  // Map các giá trị người dùng hay nhập thành giá trị enum hợp lệ
  if (["nu", "nữ", "female", "women", "girl", "lady", "ladies"].includes(value)) {
    return "nu";
  }
  if (["nam", "male", "man", "men", "boy"].includes(value)) {
    return "nam";
  }
  if (["unisex", "hottrend", "hot trend", "trend"].includes(value)) {
    return "unisex";
  }

  // Nếu đã là giá trị hợp lệ thì trả về nguyên vẹn
  return value;
};

/**
 * Lấy tất cả sản phẩm (có phân trang và lọc)
 * GET /api/product?page=1&limit=10&danh_muc=nam&search=...
 */
exports.getAllProducts = async (req, res) => {
  try {
    console.log("\n========== GET ALL PRODUCTS ==========");
    console.log("Query params:", JSON.stringify(req.query, null, 2));
    
    const {
      page = 1,
      limit = 100, // Tăng limit mặc định để lấy nhiều sản phẩm hơn
      danh_muc,
      thuong_hieu,
      min_price,
      max_price,
      search,
      sort_by = "createdAt",
      sort_order = "desc",
    } = req.query;

    // Xây dựng query - exclude deleted products
    const query = {
      is_deleted: { $ne: true }
    };

    // Lọc theo danh mục
    if (danh_muc) {
      query.danh_muc = danh_muc;
      console.log(`Filter by category: ${danh_muc}`);
    }

    // Lọc theo thương hiệu
    if (thuong_hieu) {
      query.thuong_hieu = thuong_hieu;
      console.log(`Filter by brand: ${thuong_hieu}`);
    }

    // Lọc theo giá
    if (min_price || max_price) {
      query.gia_khuyen_mai = {};
      if (min_price) query.gia_khuyen_mai.$gte = Number(min_price);
      if (max_price) query.gia_khuyen_mai.$lte = Number(max_price);
      console.log(`Filter by price: ${min_price} - ${max_price}`);
    }

    // Tìm kiếm theo tên và mô tả
    if (search) {
      query.$or = [
        { ten_san_pham: { $regex: search, $options: "i" } },
        { mo_ta: { $regex: search, $options: "i" } },
        { thuong_hieu: { $regex: search, $options: "i" } },
      ];
      console.log(`Search: ${search}`);
    }

    // Chỉ lấy sản phẩm active
    query.trang_thai = "active";

    // Sắp xếp
    const sortOptions = {};
    sortOptions[sort_by] = sort_order === "asc" ? 1 : -1;
    console.log(`Sort by: ${sort_by} (${sort_order})`);

    // Phân trang
    const skip = (Number(page) - 1) * Number(limit);
    console.log(`Pagination: page=${page}, limit=${limit}, skip=${skip}`);

    const products = await Product.find(query)
      .sort(sortOptions)
      .skip(skip)
      .limit(Number(limit));

    const total = await Product.countDocuments(query);
    
    console.log(`Found ${products.length} products (total: ${total})`);
    if (products.length > 0) {
      console.log(`First product: ${products[0].ten_san_pham} (ID: ${products[0]._id})`);
      console.log(`First product price: ${products[0].gia_goc} -> ${products[0].gia_khuyen_mai}`);
    } else {
      const totalActive = await Product.countDocuments({ trang_thai: "active" });
      console.log(`⚠️ No products found. Total active products in DB: ${totalActive}`);
    }
    console.log("==========================================\n");

    // Format sản phẩm với giá đã format
    const formattedProducts = products.map(product => formatProduct(product));

    res.json({
      success: true,
      products: formattedProducts,
      pagination: {
        page: Number(page),
        limit: Number(limit),
        total,
        pages: Math.ceil(total / Number(limit)),
      },
    });
  } catch (err) {
    console.error("❌ Lỗi khi lấy sản phẩm:", err);
    console.error("Error stack:", err.stack);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi lấy sản phẩm",
      error: err.message 
    });
  }
};

/**
 * Lấy sản phẩm theo ID
 * GET /api/product/:id
 */
exports.getProductById = async (req, res) => {
  try {
    const { id } = req.params;
    console.log(`\n========== GET PRODUCT BY ID ==========`);
    console.log(`Product ID: ${id}`);
    
    const product = await Product.findById(id);
    
    if (!product) {
      console.log(`❌ Product not found: ${id}`);
      return res.status(404).json({ 
        success: false,
        message: "Sản phẩm không tồn tại" 
      });
    }
    
    console.log(`✅ Product found: ${product.ten_san_pham}`);
    console.log(`Product price: ${product.gia_goc} -> ${product.gia_khuyen_mai}`);
    console.log("==========================================\n");
    
    // Format sản phẩm với giá đã format
    const formattedProduct = formatProduct(product);
    
    res.json({
      success: true,
      product: formattedProduct
    });
  } catch (err) {
    console.error("❌ Lỗi khi lấy sản phẩm theo ID:", err);
    if (err.name === "CastError") {
      return res.status(400).json({ 
        success: false,
        message: "ID sản phẩm không hợp lệ" 
      });
    }
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi lấy sản phẩm" 
    });
  }
};

/**
 * Lấy sản phẩm bán chạy
 * GET /api/product/best-selling?limit=10
 */
exports.getBestSellingProducts = async (req, res) => {
  try {
    const limit = Number(req.query.limit) || 10;
    console.log(`\n========== GET BEST SELLING PRODUCTS ==========`);
    console.log(`Limit: ${limit}`);
    
    const products = await Product.find({ 
      trang_thai: "active",
      is_deleted: { $ne: true }
    })
      .sort({ so_luong_da_ban: -1 })
      .limit(limit);
    
    console.log(`Found ${products.length} products`);
    if (products.length > 0) {
      console.log(`Top product: ${products[0].ten_san_pham} - Sold: ${products[0].so_luong_da_ban}`);
    } else {
      const totalProducts = await Product.countDocuments({ trang_thai: "active" });
      console.log(`⚠️ No products found. Total active products in DB: ${totalProducts}`);
    }
    console.log(`==========================================\n`);
    
    // Format sản phẩm với giá đã format
    const formattedProducts = products.map(product => formatProduct(product));
    
    // Trả về array trực tiếp như Android app expect
    res.json(formattedProducts);
  } catch (err) {
    console.error("❌ Error in getBestSellingProducts:", err);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi lấy sản phẩm bán chạy" 
    });
  }
};

/**
 * Lấy sản phẩm mới nhất
 * GET /api/product/newest?limit=10
 */
exports.getNewestProducts = async (req, res) => {
  try {
    const limit = Number(req.query.limit) || 10;
    console.log(`\n========== GET NEWEST PRODUCTS ==========`);
    console.log(`Limit: ${limit}`);
    
    const products = await Product.find({ 
      trang_thai: "active",
      is_deleted: { $ne: true }
    })
      .sort({ createdAt: -1 })
      .limit(limit);
    
    console.log(`Found ${products.length} products`);
    console.log(`==========================================\n`);
    
    // Format sản phẩm với giá đã format
    const formattedProducts = products.map(product => formatProduct(product));
    
    res.json(formattedProducts);
  } catch (err) {
    console.error("❌ Error in getNewestProducts:", err);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi lấy sản phẩm mới nhất" 
    });
  }
};

/**
 * Lấy sản phẩm hot trend
 * GET /api/product/hot-trend?limit=10
 */
exports.getHotTrendProducts = async (req, res) => {
  try {
    const limit = Number(req.query.limit) || 10;
    console.log(`\n========== GET HOT TREND PRODUCTS ==========`);
    console.log(`Limit: ${limit}`);
    
    // Lấy sản phẩm hot trend: sản phẩm trong danh mục "unisex" hoặc có đánh giá cao
    const products = await Product.find({ 
      trang_thai: "active",
      is_deleted: { $ne: true },
      $or: [
        { danh_muc: "unisex" },
        { danh_gia: { $gte: 4.5 } }
      ]
    })
      .sort({ danh_gia: -1, so_luong_da_ban: -1, createdAt: -1 })
      .limit(limit);
    
    console.log(`Found ${products.length} hot trend products`);
    if (products.length > 0) {
      console.log(`Top hot trend product: ${products[0].ten_san_pham} - Rating: ${products[0].danh_gia}`);
    }
    console.log(`==========================================\n`);
    
    // Format sản phẩm với giá đã format
    const formattedProducts = products.map(product => formatProduct(product));
    
    // Trả về array trực tiếp như Android app expect
    res.json(formattedProducts);
  } catch (err) {
    console.error("❌ Error in getHotTrendProducts:", err);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi lấy sản phẩm hot trend" 
    });
  }
};

/**
 * Lấy sản phẩm theo danh mục
 * GET /api/product/category/:danh_muc
 */
exports.getProductsByCategory = async (req, res) => {
  try {
    const { danh_muc } = req.params;
    console.log(`\n========== GET PRODUCTS BY CATEGORY ==========`);
    console.log(`Category: ${danh_muc}`);
    
    // BỎ validation enum - chấp nhận bất kỳ tên danh mục nào
    // Tìm sản phẩm có danh_muc khớp với tên danh mục (case-insensitive)
    const categoryName = decodeURIComponent(danh_muc).trim();
    
    console.log(`Searching for products with category: "${categoryName}"`);
    
    // TRƯỚC TIÊN: Tìm sản phẩm với tên danh mục chính xác (không map)
    // Điều này đảm bảo sản phẩm với danh mục mới sẽ được tìm thấy
    let products = await Product.find({
      $or: [
        { danh_muc: { $regex: new RegExp(`^${categoryName}$`, "i") } },
        { danh_muc: categoryName }
      ],
      trang_thai: "active",
      is_deleted: { $ne: true },
    });
    
    console.log(`Found ${products.length} products with exact category name "${categoryName}"`);
    
    // NẾU KHÔNG TÌM THẤY: Thử map về enum để tìm sản phẩm cũ (backward compatibility)
    if (products.length === 0) {
      const categoryNameLower = categoryName.toLowerCase();
      let queryCategory = categoryNameLower;
      
      // Map các danh mục enum cũ
      const enumMap = {
        "nam": "nam",
        "nu": "nu",
        "nữ": "nu",
        "unisex": "unisex"
      };
      
      // Kiểm tra xem có phải enum cũ không
      if (enumMap[categoryNameLower]) {
        queryCategory = enumMap[categoryNameLower];
        console.log(`No products found with category "${categoryName}", trying enum mapping to "${queryCategory}"`);
      } else {
        // Auto-map dựa trên từ khóa
        if (categoryNameLower.includes("nam") || categoryNameLower.includes("male") || categoryNameLower.includes("men")) {
          queryCategory = "nam";
          console.log(`Auto-mapping "${categoryName}" to "nam" (contains 'nam')`);
        } else if (categoryNameLower.includes("nu") || categoryNameLower.includes("nữ") || categoryNameLower.includes("female") || categoryNameLower.includes("women")) {
          queryCategory = "nu";
          console.log(`Auto-mapping "${categoryName}" to "nu" (contains 'nu')`);
        } else {
          queryCategory = "unisex";
          console.log(`Auto-mapping "${categoryName}" to "unisex" (default)`);
        }
      }
      
      // Tìm với enum mapped
      products = await Product.find({
        is_deleted: { $ne: true },
        $or: [
          { danh_muc: { $regex: new RegExp(`^${queryCategory}$`, "i") } },
          { danh_muc: queryCategory }
        ],
        trang_thai: "active",
      });
      
      console.log(`Found ${products.length} products with mapped category "${queryCategory}"`);
    }
    
    console.log(`Found ${products.length} products in category "${danh_muc}"`);
    if (products.length > 0) {
      console.log(`First product: ${products[0].ten_san_pham} (ID: ${products[0]._id}, danh_muc: ${products[0].danh_muc})`);
      console.log(`First product price: ${products[0].gia_goc} -> ${products[0].gia_khuyen_mai}`);
    } else {
      const totalProducts = await Product.countDocuments({ trang_thai: "active" });
      const allCategories = await Product.distinct("danh_muc", { trang_thai: "active" });
      console.log(`⚠️ No products found in category "${danh_muc}"`);
      console.log(`Total active products in DB: ${totalProducts}`);
      console.log(`Available categories in products: ${allCategories.join(", ")}`);
    }
    console.log(`==========================================\n`);
    
    // Format sản phẩm với giá đã format
    const formattedProducts = products.map(product => formatProduct(product));
    
    // Trả về array trực tiếp như Android app expect
    res.json(formattedProducts);
  } catch (err) {
    console.error("❌ Error in getProductsByCategory:", err);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi lấy sản phẩm theo danh mục" 
    });
  }
};

/**
 * Tạo sản phẩm mới (Admin)
 * POST /api/product
 */
exports.createProduct = async (req, res) => {
  try {
    console.log("\n========== CREATE PRODUCT ==========");
    console.log("Request body:", JSON.stringify(req.body, null, 2));
    
    // Chấp nhận bất kỳ tên danh mục nào từ Category model
    // Nếu không có danh mục, mặc định là "unisex"
    if (!req.body.danh_muc || req.body.danh_muc.trim() === "") {
      req.body.danh_muc = "unisex";
    } else {
      req.body.danh_muc = req.body.danh_muc.trim();
    }
    
    console.log(`Product category: "${req.body.danh_muc}"`);

    // Validation các trường bắt buộc
    const { ten_san_pham, gia_goc, gia_khuyen_mai, hinh_anh } = req.body;

    if (!ten_san_pham || ten_san_pham.trim() === "") {
      return res.status(400).json({ 
        success: false,
        message: "Tên sản phẩm là bắt buộc" 
      });
    }
    if (!gia_goc || gia_goc <= 0) {
      return res.status(400).json({ 
        success: false,
        message: "Giá gốc phải lớn hơn 0" 
      });
    }
    if (!gia_khuyen_mai || gia_khuyen_mai <= 0) {
      return res.status(400).json({ 
        success: false,
        message: "Giá khuyến mãi phải lớn hơn 0" 
      });
    }
    if (!hinh_anh || hinh_anh.trim() === "") {
      return res.status(400).json({ 
        success: false,
        message: "Hình ảnh là bắt buộc" 
      });
    }

    // Tạo sản phẩm mới
    const newProduct = new Product(req.body);
    await newProduct.save();
    
    console.log(`✅ Product created: ${newProduct.ten_san_pham} (ID: ${newProduct._id})`);
    
    // Tạo thông báo cho tất cả users về sản phẩm mới
    try {
      console.log("🔔 Starting to create notifications for new product...");
      const users = await User.find({}).select("_id");
      console.log(`🔔 Found ${users.length} users to notify about new product`);
      
      if (users.length === 0) {
        console.log("⚠️ No users found, skipping notification creation");
      } else {
        const notifications = users.map(user => ({
          user_id: user._id,
          loai: "new_product",
          tieu_de: "Sản phẩm mới",
          noi_dung: `Sản phẩm "${newProduct.ten_san_pham}" vừa được thêm vào cửa hàng. Hãy khám phá ngay!`,
          duong_dan: `/product/${newProduct._id}`,
          metadata: {
            product_id: newProduct._id.toString(),
            product_name: newProduct.ten_san_pham,
          },
          da_doc: false,
        }));

        console.log(`📝 Prepared ${notifications.length} notifications to insert`);
        
        if (notifications.length > 0) {
          const result = await Notification.insertMany(notifications);
          console.log(`✅ Successfully created ${result.length} notifications for new product`);
          console.log(`   Product: ${newProduct.ten_san_pham}`);
          console.log(`   Notified ${users.length} users`);
        }
      }
    } catch (notifErr) {
      console.error("❌ Failed to create notifications:", notifErr);
      console.error("Error details:", notifErr.message);
      console.error("Error stack:", notifErr.stack);
      // Không fail request nếu tạo thông báo lỗi
    }
    
    console.log("==========================================\n");
    
    // Format sản phẩm với giá đã format
    const formattedProduct = formatProduct(newProduct);
    
    res.status(201).json({ 
      success: true,
      message: "Sản phẩm được tạo thành công", 
      product: formattedProduct 
    });
  } catch (err) {
    console.error("❌ Lỗi khi tạo sản phẩm:", err);
    // Xử lý lỗi validation của Mongoose
    if (err.name === "ValidationError") {
      const errors = Object.values(err.errors).map((e) => e.message);
      return res.status(400).json({ 
        success: false,
        message: "Dữ liệu không hợp lệ", 
        errors: errors 
      });
    }
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi tạo sản phẩm" 
    });
  }
};

/**
 * Cập nhật sản phẩm theo ID (Admin)
 * PUT /api/product/:id
 */
exports.updateProduct = async (req, res) => {
  try {
    const { id } = req.params;
    console.log(`\n========== UPDATE PRODUCT ==========`);
    console.log(`Product ID: ${id}`);
    console.log("Update data:", JSON.stringify(req.body, null, 2));

    // Chấp nhận bất kỳ tên danh mục nào từ Category model
    if (req.body.danh_muc !== undefined) {
      if (req.body.danh_muc && req.body.danh_muc.trim() !== "") {
        req.body.danh_muc = req.body.danh_muc.trim();
      } else {
        req.body.danh_muc = "unisex"; // Mặc định
      }
      console.log(`Product category updated to: "${req.body.danh_muc}"`);
    }
    
    const updatedProduct = await Product.findByIdAndUpdate(
      id,
      req.body,
      {
        new: true,
        runValidators: true,
      }
    );
    
    if (!updatedProduct) {
      console.log(`❌ Product not found: ${id}`);
      return res.status(404).json({ 
        success: false,
        message: "Sản phẩm không tồn tại" 
      });
    }
    
    console.log(`✅ Product updated: ${updatedProduct.ten_san_pham}`);
    console.log(`Product price: ${updatedProduct.gia_goc} -> ${updatedProduct.gia_khuyen_mai}`);
    console.log("==========================================\n");
    
    // Format sản phẩm với giá đã format
    const formattedProduct = formatProduct(updatedProduct);
    
    res.json({
      success: true,
      message: "Cập nhật sản phẩm thành công",
      product: formattedProduct,
    });
  } catch (err) {
    console.error("❌ Lỗi khi cập nhật sản phẩm:", err);
    if (err.name === "ValidationError") {
      const errors = Object.values(err.errors).map((e) => e.message);
      return res.status(400).json({ 
        success: false,
        message: "Dữ liệu không hợp lệ",
        errors: errors 
      });
    }
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi cập nhật sản phẩm" 
    });
  }
};

/**
 * Xóa sản phẩm theo ID (Admin)
 * DELETE /api/product/:id
 */
exports.deleteProduct = async (req, res) => {
  try {
    const { id } = req.params;
    console.log(`\n========== SOFT DELETE PRODUCT ==========`);
    console.log(`Product ID: ${id}`);
    
    const product = await Product.findById(id);
    
    if (!product) {
      console.log(`❌ Product not found: ${id}`);
      return res.status(404).json({ 
        success: false,
        message: "Sản phẩm không tồn tại" 
      });
    }
    
    // Soft delete - ẩn sản phẩm, giữ trong MongoDB
    product.is_deleted = true;
    product.deleted_at = new Date();
    await product.save();
    
    console.log(`✅ Product soft deleted (hidden): ${product.ten_san_pham}`);
    console.log("==========================================\n");
    
    res.json({ 
      success: true,
      message: "Đã ẩn sản phẩm thành công (dữ liệu vẫn được giữ trong database)" 
    });
  } catch (err) {
    console.error("❌ Lỗi khi ẩn sản phẩm:", err);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi ẩn sản phẩm" 
    });
  }
};

/**
 * Cập nhật số lượng tồn kho (Admin)
 * PUT /api/product/:id/stock
 */
exports.updateStock = async (req, res) => {
  try {
    const { id } = req.params;
    const { so_luong_ton } = req.body;
    
    console.log(`\n========== UPDATE STOCK ==========`);
    console.log(`Product ID: ${id}`);
    console.log(`New stock: ${so_luong_ton}`);
    
    if (so_luong_ton === undefined || so_luong_ton < 0) {
      return res.status(400).json({
        success: false,
        message: "Số lượng tồn kho phải >= 0"
      });
    }
    
    const product = await Product.findByIdAndUpdate(
      id,
      { so_luong_ton },
      { new: true }
    );
    
    if (!product) {
      console.log(`❌ Product not found: ${id}`);
      return res.status(404).json({ 
        success: false,
        message: "Sản phẩm không tồn tại" 
      });
    }
    
    console.log(`✅ Stock updated: ${product.ten_san_pham} - New stock: ${product.so_luong_ton}`);
    console.log("==========================================\n");
    
    // Format sản phẩm với giá đã format
    const formattedProduct = formatProduct(product);
    
    res.json({
      success: true,
      message: "Cập nhật số lượng tồn kho thành công",
      product: formattedProduct,
    });
  } catch (err) {
    console.error("❌ Lỗi khi cập nhật số lượng tồn kho:", err);
    res.status(500).json({ 
      success: false,
      message: err.message || "Lỗi server khi cập nhật số lượng tồn kho" 
    });
  }
};
