package com.lms.education.module.reporting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_center_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCenterStatistics {

    @Id
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_active_students")
    @Builder.Default
    private Integer totalActiveStudents = 0;

    @Column(name = "new_students_today")
    @Builder.Default
    private Integer newStudentsToday = 0;

    @Column(name = "dropped_students_today")
    @Builder.Default
    private Integer droppedStudentsToday = 0;

    @Column(name = "total_teachers")
    @Builder.Default
    private Integer totalTeachers = 0;

    @Column(name = "new_teachers_today")
    @Builder.Default
    private Integer newTeachersToday = 0;

    @Column(name = "resigned_teachers_today")
    @Builder.Default
    private Integer resignedTeachersToday = 0;

    @Column(name = "total_other_staffs")
    @Builder.Default
    private Integer totalOtherStaffs = 0;

    @Column(name = "new_staffs_today")
    @Builder.Default
    private Integer newStaffsToday = 0;

    @Column(name = "resigned_staffs_today")
    @Builder.Default
    private Integer resignedStaffsToday = 0;

    @Column(name = "total_courses")
    @Builder.Default
    private Integer totalCourses = 0;

    @Column(name = "total_active_classes")
    @Builder.Default
    private Integer totalActiveClasses = 0;

    @Column(name = "new_classes_opened")
    @Builder.Default
    private Integer newClassesOpened = 0;

    @Column(name = "classes_closed_today")
    @Builder.Default
    private Integer classesClosedToday = 0;

    @Column(name = "new_student_ids", columnDefinition = "JSON")
    private String newStudentIds;

    @Column(name = "dropped_student_ids", columnDefinition = "JSON")
    private String droppedStudentIds;

    @Column(name = "new_teacher_ids", columnDefinition = "JSON")
    private String newTeacherIds;

    @Column(name = "resigned_teacher_ids", columnDefinition = "JSON")
    private String resignedTeacherIds;

    @Column(name = "new_class_ids", columnDefinition = "JSON")
    private String newClassIds;

    @Column(name = "closed_class_ids", columnDefinition = "JSON")
    private String closedClassIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
