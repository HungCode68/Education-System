package com.lms.education.module.academic.entity;

import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_transfer_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // --- Quan hệ Học sinh ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // --- Quan hệ Lớp cũ (From) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_class_id", nullable = false)
    private PhysicalClass fromClass;

    // --- Quan hệ Lớp mới (To) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_class_id", nullable = false)
    private PhysicalClass toClass;

    // Ngày thực hiện chuyển
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    // Lý do (TEXT)
    @Column(columnDefinition = "TEXT")
    private String reason;

    // --- Người thực hiện (Audit) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Lưu ý: Ràng buộc CHECK (from_class <> to_class)
    // JPA không tự động validate CHECK constraint của DB.
    // Logic này bạn nên kiểm tra ở tầng Service trước khi gọi .save()
}
