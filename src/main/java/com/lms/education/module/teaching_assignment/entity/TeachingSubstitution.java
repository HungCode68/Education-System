package com.lms.education.module.teaching_assignment.entity;

import com.lms.education.module.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teaching_substitutions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingSubstitution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // LIÊN KẾT: PHÂN CÔNG GỐC
    // (Để biết đang dạy thay cho suất nào: Lớp nào, Môn nào, GV chính là ai)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TeachingAssignment originalAssignment;

    // LIÊN KẾT: GIÁO VIÊN DẠY THAY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_teacher_id", nullable = false)
    private Teacher subTeacher;

    // THỜI GIAN DẠY THAY
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // CHI TIẾT
    @Column(columnDefinition = "TEXT")
    private String reason; // VD: Nghỉ ốm, Thai sản, Công tác

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SubstitutionStatus status = SubstitutionStatus.approved;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum trạng thái
    public enum SubstitutionStatus {
        pending,    // Chờ duyệt (Nếu GV tự tạo request)
        approved,   // Đã duyệt (Chính thức có hiệu lực)
        cancelled,  // Đã hủy
        rejected    // Từ chối
    }
}
