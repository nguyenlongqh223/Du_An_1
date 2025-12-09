const Cart = require("../models/Cart");
const Product = require("../models/Product");
const mongoose = require("mongoose");

/**
 * Utility: Validate ObjectId
 */
const validateObjectId = (id, fieldName) => {
  if (!id) {
    return { valid: false, error: `Thiếu ${fieldName}` };
  }
  if (!mongoose.Types.ObjectId.isValid(id)) {
    return { valid: false, error: `${fieldName} không hợp lệ` };
  }
  return { valid: true, value: new mongoose.Types.ObjectId(id) };
};

/**
 * Utility: Format cart response với populate
 */
const formatCartResponse = async (cart) => {
  if (!cart) return null;
  
  await cart.populate({
    path: "items.san_pham_id",
    select: "ten_san_pham gia_goc gia_khuyen_mai hinh_anh mo_ta thuong_hieu danh_muc danh_gia kich_thuoc so_luong_ton _id",
  });
  
  return cart;
};

/**
 * Utility: Format cart response để khớp với app
 * Giữ format cũ: _id, user_id, items[] với san_pham_id (object), so_luong, kich_thuoc, gia
 */
const formatCartForApp = (cart) => {
  if (!cart) return null;

  // Convert cart sang plain object và đảm bảo items có đầy đủ thông tin
  const cartObj = cart.toObject ? cart.toObject() : cart;
  
  return {
    _id: cartObj._id?.toString() || cartObj._id,
    user_id: cartObj.user_id?.toString() || cartObj.user_id,
    items: cartObj.items || [],
    createdAt: cartObj.createdAt,
    updatedAt: cartObj.updatedAt,
  };
};

/**
 * Thêm sản phẩm vào giỏ hàng
 * POST /api/cart
 * Body: { user_id, product_id, size, quantity }
 */
exports.addToCart = async (req, res) => {
  try {
    const { user_id, product_id, size, quantity } = req.body;

    console.log("=== ADD TO CART ===");
    console.log("Request:", { user_id, product_id, size, quantity });

    // Validation
    const userValidation = validateObjectId(user_id, "user_id");
    if (!userValidation.valid) {
      return res.status(400).json({
        success: false,
        message: userValidation.error,
      });
    }

    const productValidation = validateObjectId(product_id, "product_id");
    if (!productValidation.valid) {
      return res.status(400).json({
        success: false,
        message: productValidation.error,
      });
    }

    if (!size || size.trim() === "") {
      return res.status(400).json({
        success: false,
        message: "Thiếu kích thước (size)",
      });
    }

    if (!quantity || !Number.isInteger(Number(quantity)) || quantity <= 0) {
      return res.status(400).json({
        success: false,
        message: "Số lượng phải là số nguyên lớn hơn 0",
      });
    }

    const userIdObjectId = userValidation.value;
    const productIdObjectId = productValidation.value;

    // Kiểm tra sản phẩm có tồn tại không
    const product = await Product.findById(productIdObjectId);
    if (!product) {
      return res.status(404).json({
        success: false,
        message: "Sản phẩm không tồn tại",
      });
    }

    // Kiểm tra size có hợp lệ không
    if (!product.kich_thuoc || !product.kich_thuoc.includes(size)) {
      return res.status(400).json({
        success: false,
        message: `Kích thước ${size} không có sẵn cho sản phẩm này`,
        available_sizes: product.kich_thuoc,
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
      console.log("✅ Created new cart for user");
    }

    // Tạo item mới (mỗi lần thêm sẽ tạo item riêng biệt)
    const newItem = {
      san_pham_id: productIdObjectId,
      so_luong: Number(quantity),
      kich_thuoc: size.trim(),
      gia: price,
    };

    cart.items.push(newItem);
    const savedCart = await cart.save();

    console.log("✅ Added item to cart. Total items:", savedCart.items.length);

    // Populate và format response
    const formattedCart = await formatCartResponse(savedCart);
    const cartForApp = formatCartForApp(formattedCart);

    res.json({
      success: true,
      message: "Đã thêm sản phẩm vào giỏ hàng",
      data: cartForApp,
    });
  } catch (err) {
    console.error("❌ Lỗi khi thêm vào giỏ hàng:", err);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi thêm vào giỏ hàng",
    });
  }
};

/**
 * Lấy giỏ hàng của user (hoặc tất cả nếu không có user_id - cho admin)
 * GET /api/cart?user_id=xxx
 * GET /api/cart (lấy tất cả - cho admin)
 */
exports.getCart = async (req, res) => {
  try {
    const { user_id } = req.query;

    console.log("=== GET CART ===");
    console.log("User ID:", user_id);

    // Nếu không có user_id, trả về tất cả carts (cho admin)
    if (!user_id) {
      const allCarts = await Cart.find()
        .populate("user_id", "ho_ten email so_dien_thoai")
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

    const userValidation = validateObjectId(user_id, "user_id");
    if (!userValidation.valid) {
      return res.status(400).json({
        success: false,
        message: userValidation.error,
      });
    }

    const userIdObjectId = userValidation.value;

    const cart = await Cart.findOne({ user_id: userIdObjectId });

    if (!cart) {
      return res.json({
        success: true,
        data: {
          user_id: user_id,
          items: [],
        },
      });
    }

    // Populate và format response
    const formattedCart = await formatCartResponse(cart);
    const cartForApp = formatCartForApp(formattedCart);

    console.log("✅ Cart found. Items count:", cartForApp.items.length);

    res.json({
      success: true,
      data: cartForApp,
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

    console.log("=== UPDATE CART ITEM ===");
    console.log("Request:", { user_id, item_id, quantity });

    // Validation
    const userValidation = validateObjectId(user_id, "user_id");
    if (!userValidation.valid) {
      return res.status(400).json({
        success: false,
        message: userValidation.error,
      });
    }

    const itemValidation = validateObjectId(item_id, "item_id");
    if (!itemValidation.valid) {
      return res.status(400).json({
        success: false,
        message: itemValidation.error,
      });
    }

    if (!quantity || !Number.isInteger(Number(quantity)) || quantity <= 0) {
      return res.status(400).json({
        success: false,
        message: "Số lượng phải là số nguyên lớn hơn 0",
      });
    }

    const userIdObjectId = userValidation.value;
    const itemIdObjectId = itemValidation.value;

    // Tìm cart
    const cart = await Cart.findOne({ user_id: userIdObjectId });
    if (!cart) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy giỏ hàng",
      });
    }

    // Tìm item trong cart
    const item = cart.items.id(itemIdObjectId);
    if (!item) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy sản phẩm trong giỏ hàng",
      });
    }

    // Kiểm tra số lượng tồn kho
    const product = await Product.findById(item.san_pham_id);
    if (product && product.so_luong_ton < quantity) {
      return res.status(400).json({
        success: false,
        message: `Số lượng tồn kho không đủ. Chỉ còn ${product.so_luong_ton} sản phẩm`,
      });
    }

    // Cập nhật số lượng
    item.so_luong = Number(quantity);
    const savedCart = await cart.save();

    console.log("✅ Updated cart item quantity");

    // Populate và format response
    const formattedCart = await formatCartResponse(savedCart);
    const cartForApp = formatCartForApp(formattedCart);

    res.json({
      success: true,
      message: "Đã cập nhật số lượng",
      data: cartForApp,
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

    console.log("=== REMOVE FROM CART ===");
    console.log("Request:", { user_id, item_id });

    // Validation
    const userValidation = validateObjectId(user_id, "user_id");
    if (!userValidation.valid) {
      return res.status(400).json({
        success: false,
        message: userValidation.error,
      });
    }

    const itemValidation = validateObjectId(item_id, "item_id");
    if (!itemValidation.valid) {
      return res.status(400).json({
        success: false,
        message: itemValidation.error,
      });
    }

    const userIdObjectId = userValidation.value;
    const itemIdObjectId = itemValidation.value;

    // Tìm cart
    const cart = await Cart.findOne({ user_id: userIdObjectId });
    if (!cart) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy giỏ hàng",
      });
    }

    // Xóa item
    const item = cart.items.id(itemIdObjectId);
    if (!item) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy sản phẩm trong giỏ hàng",
      });
    }

    item.remove();
    const savedCart = await cart.save();

    console.log("✅ Removed item from cart");

    // Populate và format response
    const formattedCart = await formatCartResponse(savedCart);
    const cartForApp = formatCartForApp(formattedCart);

    res.json({
      success: true,
      message: "Đã xóa sản phẩm khỏi giỏ hàng",
      data: cartForApp,
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

    console.log("=== CLEAR CART ===");
    console.log("User ID:", user_id);

    const userValidation = validateObjectId(user_id, "user_id");
    if (!userValidation.valid) {
      return res.status(400).json({
        success: false,
        message: userValidation.error,
      });
    }

    const userIdObjectId = userValidation.value;

    // Tìm và xóa tất cả items
    const cart = await Cart.findOne({ user_id: userIdObjectId });
    if (!cart) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy giỏ hàng",
      });
    }

    cart.items = [];
    const savedCart = await cart.save();

    console.log("✅ Cleared cart");

    const cartForApp = formatCartForApp(savedCart);

    res.json({
      success: true,
      message: "Đã xóa toàn bộ giỏ hàng",
      data: cartForApp,
    });
  } catch (err) {
    console.error("❌ Lỗi khi xóa giỏ hàng:", err);
    res.status(500).json({
      success: false,
      message: err.message || "Lỗi server khi xóa giỏ hàng",
    });
  }
};
