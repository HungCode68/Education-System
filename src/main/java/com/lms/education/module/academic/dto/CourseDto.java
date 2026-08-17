package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseDto {

    private Long id;

    @NotBlank(message = "Mã khóa học không được để trống")
    @Size(max = 50, message = "Mã khóa học không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên khóa học không được để trống")
    @Size(max = 255, message = "Tên khóa học không vượt quá 255 ký tự")
    private String name;

    private String description;

    @PositiveOrZero(message = "Thời lượng khóa học (giờ) phải lớn hơn hoặc bằng 0")
    private Integer durationHours;

    @PositiveOrZero(message = "Tổng số buổi học phải lớn hơn hoặc bằng 0")
    private Integer totalSessions;
    
    @PositiveOrZero(message = "Số buổi học mỗi tuần phải lớn hơn 0")
    private Integer sessionsPerWeek;

    @PositiveOrZero(message = "Giá khóa học phải lớn hơn hoặc bằng 0")
    private BigDecimal basePrice;

    private String status; // ACTIVE, INACTIVE, DRAFT

    // Dữ liệu mở rộng linh hoạt cho Frontend (Ví dụ: lưu URL thumbnail, tags, level...)
    private Map<String, Object> metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}