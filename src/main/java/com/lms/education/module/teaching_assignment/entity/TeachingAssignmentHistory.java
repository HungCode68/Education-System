package com.lms.education.module.teaching_assignment.entity;

import com.lms.education.module.user.entity.Teacher;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teaching_assignment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingAssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // --- LIÊN KẾT: PHÂN CÔNG GỐC ---
    // Log này thuộc về phân công nào?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TeachingAssignment assignment;

    // --- LIÊN KẾT: GIÁO VIÊN CŨ & MỚI ---

    // Giáo viên cũ (Có thể null nếu là lần phân công đầu tiên)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_teacher_id")
    private Teacher oldTeacher;

    // Giáo viên mới (Có thể null nếu là hành động Hủy phân công)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_teacher_id")
    private Teacher newTeacher;

    // --- THÔNG TIN THAY ĐỔI ---

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String reason; // Lý do thay đổi (VD: Giáo viên nghỉ hưu, điều chuyển...)

    // --- AUDIT ---

    // Lưu ID của người thực hiện (Admin/Hiệu trưởng)
    // và giữ log tồn tại ngay cả khi User Admin bị xóa.
    @Column(name = "changed_by", length = 36)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    // --- ENUM ĐỊNH NGHĨA HÀNH ĐỘNG ---
    public enum ActionType {
        ASSIGNED,       // Phân công lần đầu (Old = null, New = A)
        REPLACED,       // Thay đổi giáo viên (Old = A, New = B)
        UNASSIGNED,     // Hủy phân công (Old = A, New = null)
        SUBSTITUTED     // (Tùy chọn) Ghi nhận có giáo viên dạy thay
    }
}
