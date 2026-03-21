package com.lms.education.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    public enum RoleStatus {
        active,
        inactive
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Ví dụ: ROLE_ADMIN, ROLE_USER

    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String name; // Ví dụ: Quản trị hệ thống, Khách hàng

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    @Builder.Default
    private RoleStatus status = RoleStatus.active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER) // EAGER để khi lấy Role thì lấy luôn Permission
    @JoinTable(
            name = "role_permission",
            // 'role_id' trỏ về bảng Roles (đang là UUID String)
            joinColumns = @JoinColumn(name = "role_id"),
            // 'permission_id' trỏ về bảng Permissions (đang là Integer)
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
