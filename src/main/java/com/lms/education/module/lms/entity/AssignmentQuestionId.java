package com.lms.education.module.lms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentQuestionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "question_id")
    private Long questionId;
}
