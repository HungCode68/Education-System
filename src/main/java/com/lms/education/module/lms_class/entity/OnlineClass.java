package com.lms.education.module.lms_class.entity;

import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "online_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String name; // VD: Toán Học - 10A1

    // Liên kết 1-1 với Phân công giảng dạy
    // (1 Phân công chỉ tạo ra 1 Lớp học online)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_assignment_id", nullable = false, unique = true)
    private TeachingAssignment teachingAssignment;

    @Column(length = 20)
    @Builder.Default
    private String status = "active"; // active, archived

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
