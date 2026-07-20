package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonDto {

    private Long id;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classId;

    private String classCode;
    private String className;

    @NotBlank(message = "Tên bài học không được để trống")
    private String name;

    @NotNull(message = "Số thứ tự không được để trống")
    private Integer orderNumber;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
