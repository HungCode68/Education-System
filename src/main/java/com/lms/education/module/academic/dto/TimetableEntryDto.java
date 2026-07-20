package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimetableEntryDto {

    private Long scheduleId;
    private Long classId;
    private String classCode;
    private String className;
    private String roomName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private Long teacherId;
    private String teacherName;
    private String teacherCode;
    private String role; // MAIN_TEACHER, NATIVE_TEACHER, ASSISTANT, etc.
    private Boolean isSubstituted; // true if this session has a substitute teacher
}
