package com.lms.education.module.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Ẩn các trường null khi trả về JSON
public class StaffDto {

    private Long id;

    // --- Xử lý liên kết User ---
    private Long userId;

    private String userEmail;

    // Xử lý liên kết Department
    private Long departmentId; // Có thể null (Ví dụ nhân sự tự do chưa phân phòng ban)

    private String departmentName;

    // Thông tin cá nhân
    private String staffCode;

    @NotBlank(message = "Loại nhân sự không được để trống")
    private String staffType; // TEACHER, TEACHING_ASSISTANT, CONSULTANT, MANAGER

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    private String fullName;

    // Validate số điện thoại chuẩn mạng viễn thông Việt Nam
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private String contractType; // FULLTIME, PARTTIME, VISITING

    @PositiveOrZero(message = "Lương cơ bản phải lớn hơn hoặc bằng 0")
    private BigDecimal baseSalary;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}