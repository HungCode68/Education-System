package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class TermDto {

    private Long id;

    @NotBlank(message = "Mã kỳ/đợt học không được để trống")
    @Size(max = 50, message = "Mã kỳ/đợt học không vượt quá 50 ký tự")
    private String code;

    @NotBlank(message = "Tên kỳ/đợt học không được để trống")
    @Size(max = 255, message = "Tên kỳ/đợt học không vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull(message = "Năm học không được để trống")
    private Integer year;

    @Size(max = 20, message = "Trạng thái không vượt quá 20 ký tự")
    private String status; // ACTIVE, CLOSED

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
