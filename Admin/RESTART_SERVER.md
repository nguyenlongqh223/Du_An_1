# ⚠️ QUAN TRỌNG: Cần Restart Backend Server

## Vấn đề
Sau khi thêm endpoint mới `/api/order/:orderId/payment`, backend server cần được **restart** để áp dụng thay đổi.

## Cách restart server:

### Windows (PowerShell):
```powershell
cd ..\BanGiay_Api
# Dừng server hiện tại (Ctrl+C trong terminal đang chạy server)
# Sau đó chạy lại:
node server.js
# hoặc
npm start
```

### Hoặc nếu dùng nodemon:
```powershell
cd ..\BanGiay_Api
nodemon server.js
```

## Kiểm tra server đã chạy:
1. Mở browser và truy cập: `http://localhost:3000/health`
2. Nếu thấy JSON response với status "ok" → Server đã chạy
3. Kiểm tra console của server có log: "Server đang chạy tại http://localhost:3000"

## Sau khi restart:
- Endpoint `/api/order/:orderId/payment` sẽ hoạt động
- Checkbox thanh toán sẽ không còn lỗi 404

## Các thay đổi đã được thực hiện:
✅ Thêm route `PUT /api/order/:orderId/payment` vào `routes/order.routes.js`
✅ Thêm controller `updatePaymentStatus` vào `controllers/order.controller.js`
✅ Thêm trường `da_thanh_toan` và `is_paid` vào `models/Order.js`
✅ Cập nhật frontend để sử dụng endpoint mới

