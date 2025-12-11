package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {
    @SerializedName("_id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("is_read")
    private boolean read;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
