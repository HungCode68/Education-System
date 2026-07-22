package com.lms.education.module.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "assignment_type", length = 50)
    @Builder.Default
    private String assignmentType = "HOMEWORK"; // HOMEWORK, ESSAY, QUIZ, PROJECT

    @Column(name = "time_limit_minutes")
    @Builder.Default
    private Integer timeLimitMinutes = 0; // 0 = Không giới hạn thời gian

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 1;

    @Column(length = 20)
    @Builder.Default
    private String status = "PUBLISHED"; // UNPUBLISHED, PUBLISHED, CLOSED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
