package com.lms.education.module.lms.entity;

import com.lms.education.module.academic.entity.Course;
import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "material_scope", length = 20)
    @Builder.Default
    private String materialScope = "LESSON"; // COURSE, LESSON

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType; // DOCUMENT, SLIDE, VIDEO, IMAGE, EXTERNAL_LINK

    @Column(name = "source_type", length = 50)
    @Builder.Default
    private String sourceType = "MINIO"; // MINIO, EXTERNAL

    @Column(name = "resource_url", nullable = false, columnDefinition = "TEXT")
    private String resourceUrl; // MinIO Object key hoặc URL liên kết ngoài

    @Column(name = "file_size")
    private Long fileSize; // Tính bằng byte, null nếu là link ngoài

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_official")
    @Builder.Default
    private Boolean isOfficial = false;

    @Column(name = "is_rag_enabled")
    @Builder.Default
    private Boolean isRagEnabled = false;

    @Column(name = "indexing_status", length = 50)
    @Builder.Default
    private String indexingStatus = "NOT_INDEXED"; // NOT_INDEXED, PENDING, INDEXED, FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
