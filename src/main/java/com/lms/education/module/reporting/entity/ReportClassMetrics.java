package com.lms.education.module.reporting.entity;

import com.lms.education.module.academic.entity.Classes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_class_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportClassMetrics {

    @Id
    @Column(name = "class_id")
    private Long classId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Classes classes;

    @Column(name = "total_students")
    @Builder.Default
    private Integer totalStudents = 0;

    @Column(name = "average_attendance_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averageAttendanceRate = BigDecimal.ZERO;

    @Column(name = "average_assignment_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averageAssignmentScore = BigDecimal.ZERO;

    @Column(name = "dropped_students")
    @Builder.Default
    private Integer droppedStudents = 0;

    @UpdateTimestamp
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;
}
