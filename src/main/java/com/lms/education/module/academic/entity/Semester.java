package com.lms.education.module.academic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "semesters",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"school_year_id", "priority"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name; // Học kỳ 1

    @Column(length = 20)
    private String code; // HK1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SemesterStatus status;

    private Integer priority; // 1, 2, 3

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SemesterStatus {
        upcoming, active, finished
    }
}
