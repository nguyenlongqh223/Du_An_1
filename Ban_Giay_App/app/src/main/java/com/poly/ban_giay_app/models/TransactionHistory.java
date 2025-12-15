package com.poly.ban_giay_app.models;

import java.io.Serializable;

public class TransactionHistory implements Serializable {
    public String id;
    public String userName;
    public String productName;
    public long amount;
    public String paymentDate;
    public String paymentMethod; // "credit_card", "atm_card", "bank_transfer"
    public String deliveryAddress; // Địa chỉ nhận hàng
    public String phoneNumber; // Số điện thoại
    public int quantity; // Số lượng sản phẩm
    public String orderId; // ID đơn hàng để mở OrderDetailActivity
    
    public TransactionHistory() {
    }
    
    public TransactionHistory(String userName, String productName, long amount, String paymentDate, String paymentMethod) {
        this.userName = userName;
        this.productName = productName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.id = String.valueOf(System.currentTimeMillis());
    }
    
    public TransactionHistory(String userName, String productName, long amount, String paymentDate, String paymentMethod, String deliveryAddress) {
        this.userName = userName;
        this.productName = productName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
        this.id = String.valueOf(System.currentTimeMillis());
    }
    
    public TransactionHistory(String userName, String productName, long amount, String paymentDate, String paymentMethod, String deliveryAddress, String orderId) {
        this.userName = userName;
        this.productName = productName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
        this.orderId = orderId;
        this.id = String.valueOf(System.currentTimeMillis());
    }
    
    public TransactionHistory(String userName, String productName, long amount, String paymentDate, String paymentMethod, String deliveryAddress, String phoneNumber, String orderId) {
        this.userName = userName;
        this.productName = productName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
        this.phoneNumber = phoneNumber;
        this.quantity = 1; // Default quantity
        this.orderId = orderId;
        this.id = String.valueOf(System.currentTimeMillis());
    }
    
    public TransactionHistory(String userName, String productName, long amount, String paymentDate, String paymentMethod, String deliveryAddress, String phoneNumber, int quantity, String orderId) {
        this.userName = userName;
        this.productName = productName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.deliveryAddress = deliveryAddress;
        this.phoneNumber = phoneNumber;
        this.quantity = quantity;
        this.orderId = orderId;
        this.id = String.valueOf(System.currentTimeMillis());
    }
    
    public String getPaymentMethodDisplay() {
        if (paymentMethod == null) return "Không xác định";
        switch (paymentMethod) {
            case "credit_card":
                return "Thẻ tín dụng";
            case "atm_card":
                return "Thẻ ATM";
            case "bank_transfer":
                return "Chuyển khoản ngân hàng";
            default:
                return paymentMethod;
        }
    }
}

