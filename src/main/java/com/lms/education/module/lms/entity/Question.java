package com.lms.education.module.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType; // MULTIPLE_CHOICE, ESSAY, LISTENING, READING, FILL_BLANK, TRUE_FALSE

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Column(name = "reading_passage", columnDefinition = "TEXT")
    private String readingPassage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
