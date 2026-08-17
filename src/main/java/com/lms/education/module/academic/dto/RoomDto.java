package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomDto {

    private Long id;

    @NotBlank(message = "Tên phòng học không được để trống")
    @Size(max = 100, message = "Tên phòng học không vượt quá 100 ký tự")
    private String name;

    @Min(value = 1, message = "Sức chứa tối thiểu phải là 1")
    private Integer capacity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}