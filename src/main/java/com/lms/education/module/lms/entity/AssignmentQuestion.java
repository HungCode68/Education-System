package com.lms.education.module.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "assignment_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentQuestion {

    @EmbeddedId
    private AssignmentQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assignmentId")
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "score_weight")
    @Builder.Default
    private BigDecimal scoreWeight = BigDecimal.ONE;
}
