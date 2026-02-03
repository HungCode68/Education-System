package com.lms.education.module.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String code;

    public enum PermissionScope {
        SYSTEM,
        ACADEMIC_YEAR,
        GRADE,
        CLASS,
        USER,
        SUBJECT,
        ASSIGNMENT,
        MATERIAL,
        GRADEBOOK,
        REPORT
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionScope scope;

    @Column(length = 120, columnDefinition = "NVARCHAR(120)")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
