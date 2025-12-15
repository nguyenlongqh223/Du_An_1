package com.poly.ban_giay_app.network.request;

public class CancelOrderRequest {
    private String reason;

    public CancelOrderRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
