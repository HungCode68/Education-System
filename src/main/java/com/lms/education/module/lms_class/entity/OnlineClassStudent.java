package com.lms.education.module.lms_class.entity;

import com.lms.education.module.user.entity.Student; // Đảm bảo bạn đã có Entity Student
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "online_class_students", uniqueConstraints = {
        @UniqueConstraint(name = "uq_online_class_student", columnNames = {"online_class_id", "student_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // --- MỐI QUAN HỆ: LỚP HỌC LMS ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_class_id", nullable = false)
    private OnlineClass onlineClass;

    // --- MỐI QUAN HỆ: HỌC SINH ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // --- THÔNG TIN GHI DANH ---

    // Nguồn gốc: Tự động từ hệ thống hay thêm tay?
    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_source", length = 20)
    @Builder.Default
    private EnrollmentSource enrollmentSource = EnrollmentSource.system;

    @Column(name = "enrolled_date")
    @Builder.Default
    private LocalDate enrolledDate = LocalDate.now();

    // Trạng thái học viên trong lớp
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.active;

    // --- AUDIT ---
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- ENUMS ---

    public enum EnrollmentSource {
        system, // Tự động đồng bộ từ Lớp vật lý (Physical Class)
        manual  // Giáo viên tự add thêm (Học ghép, học bù...)
    }

    public enum StudentStatus {
        active,     // Đang học bình thường
        removed,    // Bị xóa khỏi lớp (hoặc chuyển lớp)
        completed   // Đã hoàn thành khóa học (Cuối kỳ)
    }
}
