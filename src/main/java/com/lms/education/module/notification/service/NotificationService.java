package com.lms.education.module.notification.service;

import com.lms.education.module.notification.dto.NotificationDto;
import com.lms.education.module.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // TẠO THÔNG BÁO
    void sendNotification(String userId, String senderId, String title, String message,
                          Notification.NotificationType type, String relatedType, String relatedId, String metadata);

    // Lấy danh sách thông báo của người dùng đang đăng nhập (Có phân trang)
    Page<NotificationDto> getUserNotifications(String userId, Pageable pageable);

    // Lấy số lượng thông báo chưa đọc (Để hiện cục màu đỏ trên cái chuông)
    long getUnreadCount(String userId);

    //  Đánh dấu 1 thông báo cụ thể là "Đã đọc"
    void markAsRead(String notificationId, String userId);

    // Đánh dấu tất cả thông báo của người dùng là "Đã đọc"
    void markAllAsRead(String userId);
}
