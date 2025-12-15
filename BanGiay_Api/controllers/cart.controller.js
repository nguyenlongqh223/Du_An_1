const Cart = require("../models/Cart");
const Product = require("../models/Product");

/**
 * Format cart để khớp với format app mong đợi
 */
const formatCartForApp = (cart) => {
  if (!cart) return null;

  const cartObj = cart.toObject ? cart.toObject() : cart;

  // Ensure user_id is an object if populated, otherwise keep as string
  let userIdFormatted = cartObj.user_id;
  if (cartObj.user_id && typeof cartObj.user_id === 'object' && cartObj.user_id._id) {
    userIdFormatted = {
      _id: cartObj.user_id._id.toString(),
      ho_ten: cartObj.user_id.ho_ten || '',
      email: cartObj.user_id.email || '',
      so_dien_thoai: cartObj.user_id.so_dien_thoai || '',
      ten_dang_nhap: cartObj.user_id.ten_dang_nhap || '',
    };
  } else if (cartObj.user_id) {
    userIdFormatted = cartObj.user_id.toString();
  }

  return {
    _id: cartObj._id?.toString() || cartObj._id,
    user_id: userIdFormatted,
    items: cartObj.items || [],
    tong_tien: cartObj.tong_tien, // Virtual field
    tong_so_luong: cartObj.tong_so_luong, // Virtual field
    createdAt: cartObj.createdAt,
    updatedAt: cartObj.updatedAt,
  };
};

/**
 * Thêm sản phẩm vào giỏ hàng
 * POST /api/cart
 * Body: { user_id, product_id, size, quantity }
 * 
 * Mỗi lần thêm sản phẩm sẽ tạo một item MỚI riêng biệt trong giỏ hàng
 * KHÔNG merge với item cũ, dù cùng sản phẩm và size
 */
exports.addToCart = async (req, res) => {
  try {
    const { user_id, product_id, size, quantity } = req.body;

    console.log("=== ADD TO CART REQUEST ===");
    console.log("Request body:", JSON.stringify(req.body, null, 2));

    // Validation
    if (!user_id) {
      return res.status(400).json({
        success: false,
        message: "Thiếu user_id",
      });
    }

    if (!product_id) {
      return res.status(400).json({
        success: false,
        message: "Thiếu product_id",
      });
    }

    if (!size || size.trim() === "") {
      return res.status(400).json({
        success: false,
        message: "Thiếu kích thước (size)",
      });
    }

    if (!quantity || quantity <= 0) {
      return res.status(400).json({
        success: false,
        message: "Số lượng phải lớn hơn 0",
      });
    }

    // Convert string ID to ObjectId
    const mongoose = require("mongoose");
    let userIdObjectId, productIdObjectId;
    
    try {
      if (!mongoose.Types.ObjectId.isValid(user_id)) {
        return res.status(400).json({
          success: false,
          message: "user_id không hợp lệ",
        });
      }
      userIdObjectId = new mongoose.Types.ObjectId(user_id);

      if (!mongoose.Types.ObjectId.isValid(product_id)) {
        return res.status(400).json({
          success: false,
          message: "product_id không hợp lệ",
        });
      }
      productIdObjectId = new mongoose.Types.ObjectId(product_id);
    } catch (err) {
      return res.status(400).json({
        success: false,
        message: "Lỗi định dạng ID: " + err.message,
      });
    }

    // Kiểm tra sản phẩm có tồn tại không
    const product = await Product.findById(productIdObjectId);
    if (!product) {
      return res.status(404).json({
        success: false,
        message: `Sản phẩm không tồn tại (ID: ${product_id})`,
      });
    }

    // Kiểm tra size có hợp lệ không
    if (!product.kich_thuoc || !product.kich_thuoc.includes(size)) {
      return res.status(400).json({
        success: false,
        message: `Kích thước ${size} không có sẵn cho sản phẩm này`,
      });
    }

    // Kiểm tra số lượng tồn kho
    if (product.so_luong_ton < quantity) {
      return res.status(400).json({
        success: false,
        message: `Số lượng tồn kho không đủ. Chỉ còn ${product.so_luong_ton} sản phẩm`,
      });
    }

    // Lấy giá sản phẩm (ưu tiên giá khuyến mãi)
    const price = product.gia_khuyen_mai || product.gia_goc;

    // Tìm hoặc tạo cart cho user
    let cart = await Cart.findOne({ user_id: userIdObjectId });

    if (!cart) {
      cart = new Cart({
        user_id: userIdObjectId,
        items: [],
      });
      console.log("✅ Created new cart for user:", userIdObjectId);
    } else {
      console.log("✅ Found existing cart. Current items count:", cart.items.length);
    }

    // QUAN TRỌNG: Luôn tạo item MỚI, KHÔNG merge với item cũ
    // Mỗi lần thêm sản phẩm sẽ tạo một item riêng biệt với _id riêng
    const newItem = {
      san_pham_id: productIdObjectId,
      so_luong: quantity,
      kich_thuoc: size,
      gia: price,
    };

    cart.items.push(newItem);
    console.log("✅ Added NEW item to cart (no merge). Total items now:", cart.items.length);
    console.log("New item details:", JSON.stringify(newItem, null, 2));

    // Lưu cart vào MongoDB
    const savedCart = await cart.save();
    console.log("✅ Cart saved successfully! Cart ID:", savedCart._id);
    console.log("✅ Total items in cart:", savedCart.items.length);
    
    // Log từng item để verify
    savedCart.items.forEach((item, index) => {
      console.log(`  Item ${index}: _id=${item._id}, product=${item.san_pham_id}, size=${item.kich_thuoc}, qty=${item.so_luong}`);
    });

    // Populate product info để trả về - Đảm bảo có đầy đủ thông tin sản phẩm
    await savedCart.populate({
      path: "items.san_pham_id",
      select: "ten_san_pham gia_goc gia_khuyen_mai hinh_anh mo_ta thuong_hieu danh_muc danh_gia kich_thuoc so_luong_ton _id",
    });

    // Log để verify populate đã hoạt động
    console.log("=== CART RESPONSE (after populate) ===");
    console.log("Cart ID:", savedCart._id);
    console.log("Items count:", savedCart.items.length);
    savedCart.items.forEach((item, index) => {
      console.log(`Item ${index}:`);
      console.log(`  - san_pham_id type: ${typeof item.san_pham_id}`);
      if (item.san_pham_id && typeof item.san_pham_id === 'object') {
        console.log(`  - Product name: ${item.san_pham_id.ten_san_pham || item.san_pham_id.name}`);
        console.log(`  - Product price: ${item.san_pham_id.gia_khuyen_mai || item.san_pham_id.gia_goc}`);
      } else {
        console.log(`  - san_pham_id is NOT populated: ${item.san_pham_id}`);
      }
    });

    res.json({
      success: true,
      message: "Đã thêm sản phẩm vào giỏ hàng",
      data: savedCart,
    });
  } catch (err) {
    console.error("❌ Lỗi khi thêm vào giỏ hàng:", err);
    console.error("Error stack:", err.stack);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi thêm vào giỏ hàng",
    });
  }
};

/**
 * Lấy giỏ hàng của user
 * GET /api/cart?user_id=xxx
 */
exports.getCart = async (req, res) => {
  try {
    const { user_id } = req.query;

    console.log("=== GET CART ===");
    console.log("User ID:", user_id);

    // Nếu không có user_id, trả về tất cả carts (cho admin)
    if (!user_id) {
      const allCarts = await Cart.find()
        .populate("user_id", "ho_ten email so_dien_thoai ten_dang_nhap")
        .populate("items.san_pham_id", "ten_san_pham gia_goc gia_khuyen_mai hinh_anh mo_ta thuong_hieu danh_muc danh_gia kich_thuoc so_luong_ton _id")
        .sort({ createdAt: -1 });

      console.log("✅ Found all carts:", allCarts.length);

      // Format mỗi cart để khớp với app
      const formattedCarts = allCarts.map((cart) => formatCartForApp(cart));

      return res.json({
        success: true,
        data: formattedCarts,
      });
    }

    // Nếu có user_id, trả về cart của user đó
    const mongoose = require("mongoose");
    let userIdObjectId;
    
    if (!mongoose.Types.ObjectId.isValid(user_id)) {
      return res.status(400).json({
        success: false,
        message: "user_id không hợp lệ",
      });
    }
    userIdObjectId = new mongoose.Types.ObjectId(user_id);

    const cart = await Cart.findOne({ 
      user_id: userIdObjectId,
      is_deleted: { $ne: true }
    }).populate({
      path: "items.san_pham_id",
      select: "ten_san_pham gia_goc gia_khuyen_mai hinh_anh mo_ta thuong_hieu danh_muc danh_gia kich_thuoc so_luong_ton _id",
    });

    if (!cart) {
      return res.json({
        success: true,
        data: {
          user_id: user_id,
          items: [],
        },
      });
    }

    // Log để verify populate đã hoạt động
    console.log("=== GET CART RESPONSE (after populate) ===");
    console.log("Cart ID:", cart._id);
    console.log("Items count:", cart.items.length);
    cart.items.forEach((item, index) => {
      console.log(`Item ${index}:`);
      console.log(`  - san_pham_id type: ${typeof item.san_pham_id}`);
      if (item.san_pham_id && typeof item.san_pham_id === 'object' && item.san_pham_id.ten_san_pham) {
        console.log(`  ✅ Product populated: ${item.san_pham_id.ten_san_pham}`);
        console.log(`  - Product ID: ${item.san_pham_id._id}`);
        console.log(`  - Product price: ${item.san_pham_id.gia_khuyen_mai || item.san_pham_id.gia_goc}`);
      } else {
        console.log(`  ⚠️ san_pham_id is NOT populated: ${item.san_pham_id}`);
      }
    });

    // Log để verify populate đã hoạt động
    console.log("=== GET CART RESPONSE (after populate) ===");
    console.log("Cart ID:", cart._id);
    console.log("Items count:", cart.items.length);
    cart.items.forEach((item, index) => {
      console.log(`Item ${index}:`);
      console.log(`  - san_pham_id type: ${typeof item.san_pham_id}`);
      if (item.san_pham_id && typeof item.san_pham_id === 'object' && item.san_pham_id.ten_san_pham) {
        console.log(`  ✅ Product populated: ${item.san_pham_id.ten_san_pham}`);
        console.log(`  - Product ID: ${item.san_pham_id._id}`);
        console.log(`  - Product price: ${item.san_pham_id.gia_khuyen_mai || item.san_pham_id.gia_goc}`);
      } else {
        console.log(`  ⚠️ san_pham_id is NOT populated: ${item.san_pham_id}`);
      }
    });

    res.json({
      success: true,
      data: cart,
    });
  } catch (err) {
    console.error("❌ Lỗi khi lấy giỏ hàng:", err);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi lấy giỏ hàng",
    });
  }
};

/**
 * Cập nhật số lượng sản phẩm trong giỏ hàng
 * PUT /api/cart/item
 * Body: { user_id, item_id, quantity }
 */
exports.updateCartItem = async (req, res) => {
  try {
    const { user_id, item_id, quantity } = req.body;

    if (!user_id || !item_id || !quantity || quantity <= 0) {
      return res.status(400).json({
        success: false,
        message: "Thiếu thông tin hoặc số lượng không hợp lệ",
      });
    }

    const mongoose = require("mongoose");
    const userIdObjectId = new mongoose.Types.ObjectId(user_id);
    const itemIdObjectId = new mongoose.Types.ObjectId(item_id);

    const cart = await Cart.findOne({ user_id: userIdObjectId });
    if (!cart) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy giỏ hàng",
      });
    }

    const itemIndex = cart.items.findIndex(
      (item) => item._id.toString() === itemIdObjectId.toString()
    );

    if (itemIndex === -1) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy sản phẩm trong giỏ hàng",
      });
    }

    cart.items[itemIndex].so_luong = quantity;
    await cart.save();

    res.json({
      success: true,
      message: "Đã cập nhật số lượng",
      data: cart,
    });
  } catch (err) {
    console.error("❌ Lỗi khi cập nhật giỏ hàng:", err);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi cập nhật giỏ hàng",
    });
  }
};

/**
 * Xóa sản phẩm khỏi giỏ hàng
 * DELETE /api/cart/item
 * Body: { user_id, item_id }
 */
exports.removeFromCart = async (req, res) => {
  try {
    const { user_id, item_id } = req.body;

    if (!user_id || !item_id) {
      return res.status(400).json({
        success: false,
        message: "Thiếu user_id hoặc item_id",
      });
    }

    const mongoose = require("mongoose");
    const userIdObjectId = new mongoose.Types.ObjectId(user_id);
    const itemIdObjectId = new mongoose.Types.ObjectId(item_id);

    const cart = await Cart.findOne({ user_id: userIdObjectId });
    if (!cart) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy giỏ hàng",
      });
    }

    const initialLength = cart.items.length;
    cart.items = cart.items.filter(
      (item) => item._id.toString() !== itemIdObjectId.toString()
    );

    if (cart.items.length === initialLength) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy sản phẩm trong giỏ hàng",
      });
    }

    await cart.save();

    res.json({
      success: true,
      message: "Đã xóa sản phẩm khỏi giỏ hàng",
      data: cart,
    });
  } catch (err) {
    console.error("❌ Lỗi khi xóa khỏi giỏ hàng:", err);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi xóa khỏi giỏ hàng",
    });
  }
};

/**
 * Xóa toàn bộ giỏ hàng
 * DELETE /api/cart
 * Body: { user_id }
 */
exports.clearCart = async (req, res) => {
  try {
    const { user_id } = req.body;

    if (!user_id) {
      return res.status(400).json({
        success: false,
        message: "Thiếu user_id",
      });
    }

    const mongoose = require("mongoose");
    const userIdObjectId = new mongoose.Types.ObjectId(user_id);

    const cart = await Cart.findOne({ user_id: userIdObjectId });
    if (!cart) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy giỏ hàng",
      });
    }

    cart.items = [];
    await cart.save();

    res.json({
      success: true,
      message: "Đã xóa toàn bộ giỏ hàng",
      data: cart,
    });
  } catch (err) {
    console.error("❌ Lỗi khi xóa giỏ hàng:", err);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi xóa giỏ hàng",
    });
  }
};

/**
 * Xóa giỏ hàng theo ID (Admin only)
 * DELETE /api/cart/:cartId
 */
exports.deleteCartById = async (req, res) => {
  try {
    const { cartId } = req.params;

    console.log(`\n========== DELETE CART BY ID ==========`);
    console.log(`Cart ID: ${cartId}`);

    const cart = await Cart.findById(cartId);

    if (!cart) {
      console.log(`❌ Cart not found: ${cartId}`);
      return res.status(404).json({
        success: false,
        message: "Giỏ hàng không tồn tại",
      });
    }

    console.log(`✅ Cart found: ${cart._id}`);

    // Soft delete - ẩn giỏ hàng, giữ trong MongoDB
    cart.is_deleted = true;
    cart.deleted_at = new Date();
    await cart.save();

    console.log(`✅ Cart soft deleted (hidden): ${cartId}`);
    console.log("==========================================\n");

    res.json({
      success: true,
      message: "Đã ẩn giỏ hàng thành công (dữ liệu vẫn được giữ trong database)",
      data: {
        cartId: cartId,
        deletedAt: cart.deleted_at,
      },
    });
  } catch (err) {
    console.error("❌ Lỗi khi xóa giỏ hàng:", err);
    if (err.name === "CastError") {
      return res.status(400).json({
        success: false,
        message: "ID giỏ hàng không hợp lệ",
      });
    }
    res.status(500).json({
      success: false,
      error: err.message || "Lỗi server khi xóa giỏ hàng",
    });
  }
};
