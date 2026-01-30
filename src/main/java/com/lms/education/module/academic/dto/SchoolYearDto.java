package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.academic.entity.SchoolYear.SchoolYearStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Quan trọng: Nếu trường nào null (như id khi tạo mới) thì không trả về JSON
public class SchoolYearDto {

    // --- Phần Output (Response) ---
    // ID có thể null khi Client gửi Request tạo mới
    private String id;

    // --- Phần Input (Request) + Validate ---
    @NotBlank(message = "Tên năm học không được để trống")
    @Size(max = 50, message = "Tên năm học tối đa 50 ký tự")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    private SchoolYearStatus status;

    // --- Phần Output (Read-only) ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Custom Validation ---
    @AssertTrue(message = "Ngày kết thúc phải diễn ra sau ngày bắt đầu")
    private boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }

}
