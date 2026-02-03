package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class GradeDto {

    // --- Phần Output (Response) ---
    private String id;

    // --- Phần Input (Request) + Validate ---

    @NotBlank(message = "Tên khối không được để trống")
    @Size(max = 20, message = "Tên khối tối đa 20 ký tự")
    private String name; // Ví dụ: "Khối 10"

    @NotNull(message = "Cấp độ (Level) không được để trống")
    @Min(value = 1, message = "Level khối phải lớn hơn 0")
    private Integer level; // Quan trọng: Dùng để sắp xếp (VD: Khối 10 < Khối 11)

    // Trạng thái hoạt động
    private Boolean isActive;

    // --- Phần Output (Read-only) ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
