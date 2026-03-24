package com.lms.education.module.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.user.entity.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {

    private String id;

    @NotBlank(message = "Tên phòng ban/tổ bộ môn không được để trống")
    @Size(max = 100, message = "Tên phòng ban không được vượt quá 100 ký tự")
    private String name;

    private String description;

    // Sẽ nhận giá trị 'academic' hoặc 'office'
    private Department.DepartmentType type;

    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
