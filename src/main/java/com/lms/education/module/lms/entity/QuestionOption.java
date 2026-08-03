package com.lms.education.module.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_content", nullable = false, columnDefinition = "TEXT")
    private String optionContent;

    @Column(name = "is_correct")
    @Builder.Default
    private Boolean isCorrect = false;
}
