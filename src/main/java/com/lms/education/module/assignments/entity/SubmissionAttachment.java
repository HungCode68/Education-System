package com.lms.education.module.assignments.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Liên kết với bảng bài nộp
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private AssignmentSubmission submission;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // Tên object lưu trên MinIO (VD: 123e4567-e89b-12d3...pdf)

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50)
    private FileType fileType;

    @Column(name = "file_size")
    private Long fileSize; // Tính bằng bytes

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // --- ENUM cho các loại file ---
    public enum FileType {
        image,
        document,
        video,
        audio,
        compressed,
        other
    }
}
