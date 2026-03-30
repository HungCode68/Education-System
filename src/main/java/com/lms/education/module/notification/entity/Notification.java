package com.lms.education.module.notification.entity;

import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Người NHẬN thông báo (Ví dụ: Học sinh)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Người GỬI thông báo (Ví dụ: Giáo viên - Có thể null nếu là thông báo hệ thống tự động)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType type;

    // Bảng liên quan (VD: 'assignments', 'announcements')
    @Column(name = "related_type", length = 50)
    private String relatedType;

    // ID của bản ghi trong bảng liên quan
    @Column(name = "related_id", length = 36)
    private String relatedId;

    // Lưu dữ liệu dạng JSON
    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // --- ENUM cho Loại thông báo ---
    public enum NotificationType {
        assignment,
        grade,
        announcement,
        system,
        comment
    }
}
