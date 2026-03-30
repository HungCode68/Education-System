package com.lms.education.module.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;

    // Dành cho Frontend xử lý logic hiển thị tên người gửi
    private String senderId;
    private String senderName;

    private String title;
    private String message;

    private Notification.NotificationType type;

    private String relatedType;
    private String relatedId;

    // Frontend có thể parse chuỗi này thành JSON Object để dùng
    private String metadata;

    // Cờ báo hiệu đã đọc hay chưa (Cực kỳ tiện cho Frontend hiển thị dấu chấm đỏ)
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
