package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassesDto {

    private Long id;

    @NotNull(message = "ID khóa học không được để trống")
    private Long courseId;

    private String courseCode;
    private String courseName;

    private Long termId;

    private String termCode;
    private String termName;

    @NotBlank(message = "Mã lớp học không được để trống")
    @Size(max = 50, message = "Mã lớp học không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên lớp học không được để trống")
    @Size(max = 255, message = "Tên lớp học không vượt quá 255 ký tự")
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Min(value = 10, message = "Sức chứa tối đa của lớp học phải lớn hơn hoặc bằng 10")
    private Integer maxStudents;

    private Integer currentStudents;

    @Size(max = 20, message = "Trạng thái không vượt quá 20 ký tự")
    private String status; // OPENING, ONGOING, CLOSED, CANCELLED

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
