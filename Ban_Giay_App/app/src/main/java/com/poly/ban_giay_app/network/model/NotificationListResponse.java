package com.poly.ban_giay_app.network.model;

import java.util.List;

public class NotificationListResponse {
    private List<NotificationResponse> notifications;

    public List<NotificationResponse> getNotifications() {
        return notifications;
    }

    public int getUnreadCount() {
        if (notifications == null) {
            return 0;
        }
        int count = 0;
        for (NotificationResponse notification : notifications) {
            if (notification != null && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }
}
