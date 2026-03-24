package com.lms.education.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Lưu thẳng ID thay vì dùng @ManyToOne(User) để tốc độ ghi Log được nhanh nhất,
    // tránh việc Hibernate phải query bảng User trước khi Insert Log.
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "actor_name", columnDefinition = "NVARCHAR(100)")
    private String actorName;

    @Column(length = 50)
    private String module;

    @Column(length = 50, nullable = false)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 36)
    private String targetId;

    // Lưu dữ liệu dạng chuỗi JSON
    @Column(columnDefinition = "JSON")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LogStatus status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Enum trạng thái hành động
    public enum LogStatus {
        success,
        failure,
        error
    }

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = LogStatus.success;
        }
    }
}