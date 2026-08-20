package com.lms.education.module.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentDto {

    private Long id;

    private Long userId;
    private String userEmail;
    private Long currentClassId;

    private String studentCode;

    @NotBlank(message = "Họ và tên học viên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    private String fullName;

    @Size(max = 100, message = "Tên phụ huynh không vượt quá 100 ký tự")
    private String parentName;

    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại phụ huynh không hợp lệ")
    private String parentPhone;

    @Size(max = 50, message = "Mục tiêu điểm số không vượt quá 50 ký tự")
    private String targetScore;

    // Trạng thái học viên (STUDYING, RESERVED, GRADUATED, DROPPED)
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String gender;
    private String address;
    
    // Validate số điện thoại chuẩn mạng viễn thông Việt Nam (có thể null theo yêu cầu user)
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại học viên không hợp lệ")
    private String phone;
    
    private String identityNumber;
}