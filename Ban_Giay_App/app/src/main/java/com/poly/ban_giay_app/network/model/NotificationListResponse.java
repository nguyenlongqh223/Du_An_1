package com.poly.ban_giay_app.network.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NotificationListResponse {
    @SerializedName("notifications")
    private List<NotificationResponse> notifications;

    @SerializedName("data")
    private List<NotificationResponse> data;

    public List<NotificationResponse> getNotifications() {
        // Ưu tiên notifications, sau đó data
        if (notifications != null && !notifications.isEmpty()) {
            return notifications;
        }
        return data;
    }

    public int getUnreadCount() {
        List<NotificationResponse> list = getNotifications();
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (NotificationResponse notification : list) {
            if (notification != null && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }
}
