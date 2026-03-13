package com.lms.education.module.learning_material.entity;

import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.user.entity.User; // Đảm bảo bạn import đúng đường dẫn Entity User
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_materials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // --- MỐI QUAN HỆ: LỚP HỌC ONLINE ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_class_id", nullable = false)
    private OnlineClass onlineClass;

    // --- THÔNG TIN TÀI LIỆU ---
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 20)
    private FileType fileType;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MaterialStatus status = MaterialStatus.unpublished;

    // --- MỐI QUAN HỆ: NGƯỜI TẢI LÊN ---
    // Khóa ngoại liên kết tới bảng users (Giáo viên hoặc Admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    // --- AUDIT ---
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- ENUMS ---
    public enum FileType {
        slide,      // Bài giảng PowerPoint/PDF
        video,      // Video bài giảng (MP4)
        document,   // Tài liệu Word/Excel/PDF
        link,       // Đường dẫn ngoài (Youtube, Drive...)
        other       // Định dạng khác
    }

    public enum MaterialStatus {
        published,  // Đã xuất bản (Học sinh xem được)
        unpublished // Nháp/Đang ẩn (Chỉ giáo viên thấy)
    }
}
