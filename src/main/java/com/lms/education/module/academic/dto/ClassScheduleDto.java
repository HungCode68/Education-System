package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassScheduleDto {

    private Long id;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classId;

    private String classCode;
    private String className;

    private Long roomId;
    private String roomName;

    @NotNull(message = "Thứ trong tuần không được để trống")
    @Min(value = 2, message = "Thứ trong tuần tối thiểu là 2 (Thứ 2)")
    @Max(value = 8, message = "Thứ trong tuần tối đa là 8 (Chủ nhật)")
    private Integer dayOfWeek;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
}
