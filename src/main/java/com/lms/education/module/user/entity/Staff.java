package com.lms.education.module.user.entity;

import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết 1-1 với bảng Users (1 tài khoản User chỉ thuộc về 1 Nhân viên)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true, unique = true)
    private User user;

    // Liên kết N-1 với bảng Departments (Nhiều nhân viên có thể thuộc 1 Phòng ban)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;

    @Column(name = "staff_code", nullable = false, unique = true, length = 50)
    private String staffCode;

    @Column(name = "staff_type", nullable = false, length = 50)
    private String staffType;

    @Column(name = "job_title", length = 150, columnDefinition = "NVARCHAR(150)")
    private String jobTitle;

    @Column(name = "full_name", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "contract_type", length = 50)
    private String contractType;

    // Dùng BigDecimal cho tiền tệ để tính toán chính xác nhất, không bị sai số thập phân
    @Column(name = "base_salary", precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20, columnDefinition = "NVARCHAR(20)")
    private String gender;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String address;

    @Column(length = 50)
    private String nationality;

    @Column(name = "identity_number", length = 50)
    private String identityNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}