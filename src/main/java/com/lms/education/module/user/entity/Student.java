package com.lms.education.module.user.entity;

import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết 1-1 với bảng Users
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @Column(name = "student_code", nullable = false, unique = true, length = 50)
    private String studentCode;

    @Column(name = "full_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(name = "parent_name", length = 100, columnDefinition = "NVARCHAR(100)")
    private String parentName;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(name = "target_score", length = 50)
    private String targetScore;

    @Column(nullable = false, length = 50)
    private String status; // STUDYING, RESERVED, GRADUATED, DROPPED

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20, columnDefinition = "NVARCHAR(20)")
    private String gender;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}