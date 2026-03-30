package com.lms.education.module.assignments.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Liên kết với Bài nộp
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private AssignmentSubmission submission;

    // Liên kết với Câu hỏi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssignmentQuestion question;

    // Dùng cho câu Tự luận
    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    // Dùng cho câu Trắc nghiệm (Liên kết với Đáp án được chọn)
    // Có thể null vì nếu là câu tự luận thì không có option nào được chọn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    @Builder.Default
    private Double score = 0.0;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}