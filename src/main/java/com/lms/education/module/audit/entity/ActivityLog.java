package com.lms.education.module.audit.entity;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

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

    @Column(columnDefinition = "JSON")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LogStatus status = LogStatus.success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum LogStatus {
        success,
        failure,
        error
    }
}
