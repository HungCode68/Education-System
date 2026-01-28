package com.lms.education.module.academic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    // Lưu tên môn tiếng Việt (Ví dụ: "Toán cao cấp")
    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String name;

    // Map TEXT cho mô tả chi tiết
    @Column(columnDefinition = "TEXT")
    private String description;

    // Map is_active
    // @Builder.Default để khi tạo mới object bằng Builder, giá trị mặc định là true
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Mở rộng quan hệ (Optional - Để bạn tham khảo) ---
    // Môn học thường thuộc về một Tổ chuyên môn (Department)
    // Nếu bảng subjects có cột department_id, bạn có thể thêm đoạn dưới:
    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    */
}
