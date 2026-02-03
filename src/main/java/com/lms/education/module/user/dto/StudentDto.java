package com.lms.education.module.user.dto;

import com.lms.education.module.user.entity.Student;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDto {
    private String id;

    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private Student.Gender gender;

    private String currentClassId;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Parent phone is required")
    private String parentPhone;

    @NotBlank(message = "Parent name is required")
    private String parentName;

    private Integer admissionYear;

    private Student.Status status;
}
