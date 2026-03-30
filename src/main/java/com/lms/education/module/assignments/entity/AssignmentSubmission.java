package com.lms.education.module.assignments.entity;

import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_submissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_assignment_student", columnNames = {"assignment_id", "student_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Liên kết với Bài tập
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    // Liên kết với Học sinh
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "student_note", columnDefinition = "TEXT")
    private String studentNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", length = 20)
    @Builder.Default
    private SubmissionStatus submissionStatus = SubmissionStatus.not_submitted;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "is_late")
    @Builder.Default
    private Boolean isLate = false;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 1;

    // Dùng Double cho điểm số (Tương đương DECIMAL(5,2))
    @Column(columnDefinition = "DECIMAL(5,2)")
    private Double score;

    @Column(name = "teacher_feedback", columnDefinition = "TEXT")
    private String teacherFeedback;

    // Liên kết với Giáo viên/Admin chấm bài
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_method", length = 20)
    @Builder.Default
    private GradingMethod gradingMethod = GradingMethod.manual;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- ENUMS ---
    public enum SubmissionStatus {
        not_submitted,
        draft,
        submitted,
        graded,
        late
    }

    public enum GradingMethod {
        auto,
        manual
    }
}
