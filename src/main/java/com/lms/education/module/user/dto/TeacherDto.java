package com.lms.education.module.user.dto;

import com.lms.education.module.user.entity.Teacher;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDto {
    private String id;

    @NotBlank(message = "Teacher code is required")
    private String teacherCode;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private LocalDate dateOfBirth;

    private Teacher.Gender gender;

    private String phone;

    private String emailContact;

    @NotBlank(message = "Address is required")
    private String address;

    private String departmentId;

    @NotBlank(message = "Position is required")
    private String position;

    @NotBlank(message = "Degree is required")
    private String degree;

    private String major;

    private LocalDate startDate;

    private Teacher.Status status;
}
