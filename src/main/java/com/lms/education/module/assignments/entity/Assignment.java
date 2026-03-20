package com.lms.education.module.assignments.entity;

import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Liên kết n-1 với bảng online_classes
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_class_id", nullable = false)
    private OnlineClass onlineClass;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", length = 30)
    private AssignmentType assignmentType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "allow_late_submission")
    private Boolean allowLateSubmission;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions;

    @Column(name = "view_answers")
    private Boolean viewAnswers;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssignmentStatus status;

    // Liên kết n-1 với bảng users (Người tạo bài tập)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Định nghĩa các Enum
    public enum AssignmentType {
        multiple_choice,
        essay,
        file_upload,
        mixed
    }

    public enum AssignmentStatus {
        published,
        unpublished,
        draft
    }

    // Set các giá trị mặc định trước khi lưu vào Database
    @PrePersist
    public void prePersist() {
        if (maxScore == null) maxScore = new BigDecimal("10.00");
        if (allowLateSubmission == null) allowLateSubmission = false;
        if (maxAttempts == null) maxAttempts = 1;
        if (shuffleQuestions == null) shuffleQuestions = false;
        if (viewAnswers == null) viewAnswers = false;
        if (status == null) status = AssignmentStatus.unpublished;
    }
}
