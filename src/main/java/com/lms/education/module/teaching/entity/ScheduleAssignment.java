package com.lms.education.module.teaching.entity;

import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.user.entity.Staff;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "schedule_assignments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_staff", columnNames = {"schedule_id", "staff_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff teacher;

    @Column(length = 50)
    private String role; // MAIN_TEACHER, NATIVE_TEACHER, ASSISTANT

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
