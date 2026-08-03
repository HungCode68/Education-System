package com.lms.education.module.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "submission_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_question",
                columnNames = {"submission_id", "question_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuestionOption selectedOption;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "earned_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal earnedScore = BigDecimal.ZERO;

    @Column(name = "is_auto_graded")
    @Builder.Default
    private Boolean isAutoGraded = false;
}
