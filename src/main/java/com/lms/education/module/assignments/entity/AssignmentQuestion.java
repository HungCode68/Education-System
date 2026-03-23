package com.lms.education.module.assignments.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Liên kết Many-To-One với bảng assignments (Khóa ngoại)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 30)
    private QuestionType questionType;

    @Column(columnDefinition = "DECIMAL(5,2)")
    private Double score;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Khai báo Enum khớp với CHECK constraint trong SQL ---
    public enum QuestionType {
        multiple_choice,
        essay,
        file_upload
    }
}
