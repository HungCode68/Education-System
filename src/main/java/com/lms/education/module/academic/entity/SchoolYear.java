package com.lms.education.module.academic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_years")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String name; // Ví dụ: "2025-2026", "Học kỳ 1"

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public enum SchoolYearStatus {
        active,
        archived
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    @Builder.Default
    private SchoolYearStatus status = SchoolYearStatus.active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
