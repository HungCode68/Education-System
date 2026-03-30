package com.lms.education.module.notification.entity;

import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 20, nullable = false)
    private AnnouncementScope scope;

    // Liên kết với Lớp học Offline (Có thể null nếu thông báo thuộc về lớp Online)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_class_id")
    private PhysicalClass physicalClass;

    // Liên kết với Lớp học Online (Có thể null nếu thông báo thuộc về lớp Offline)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_class_id")
    private OnlineClass onlineClass;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    // Người đăng thông báo (Giáo viên / Admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "published_at")
    @Builder.Default
    private LocalDateTime publishedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- ENUM cho Phạm vi thông báo ---
    public enum AnnouncementScope {
        physical_class,
        online_class
    }
}
