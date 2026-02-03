package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubjectDto {

    // --- Phần Output (Dùng khi trả về Client) ---
    private String id;

    // --- Phần Input (Dùng khi Client gửi lên) + Validate ---
    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 100, message = "Tên môn học tối đa 100 ký tự")
    private String name;

    private String description;

    // Trạng thái hoạt động (True/False)
    private Boolean isActive;

    // --- Phần Output (Read-only) ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
