package com.lms.education.module.teaching_assignment.entity;

import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.academic.entity.SchoolYear;
import com.lms.education.module.academic.entity.Semester;
import com.lms.education.module.academic.entity.Subject;
import com.lms.education.module.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "teaching_assignments",
        uniqueConstraints = {
                // Ràng buộc duy nhất: Trong 1 Lớp, 1 Môn, 1 Học kỳ -> Chỉ có 1 Giáo viên được phân công (Active)
                @UniqueConstraint(name = "uq_teaching_assignment", columnNames = {"physical_class_id", "subject_id", "semester_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // --- CÁC QUAN HỆ CHÍNH (Có FK) ---

    // Lớp học vật lý (Dạy lớp nào?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_class_id", nullable = false)
    private PhysicalClass physicalClass;

    // Môn học (Dạy môn gì?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // Giáo viên (Ai dạy?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // Năm học
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;

    // --- CÁC TRƯỜNG KHÁC (Chưa có FK trong SQL) ---

    // Học kỳ (Quan trọng để chia giai đoạn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    // Lớp học LMS
    @Column(name = "online_class_id", length = 36)
    private String onlineClassId;

    // --- TRẠNG THÁI & THỜI GIAN ---

    public enum AssignmentStatus {
        active,    // Đang giảng dạy
        inactive   // Đã hủy/Ngưng phân công
    }

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.active;

    // Ngày bắt đầu được phân công
    @Column(name = "assigned_date")
    @Builder.Default
    private LocalDate assignedDate = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
