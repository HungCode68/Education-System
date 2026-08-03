package com.lms.education.module.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassAnnouncementDto {

    private Long id;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classId;

    private Long createdById;

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống")
    private String content;

    private Boolean hasAttachment;

    private String attachmentUrl;

    private Boolean isPinned;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Enriched display fields
    private String className;
    private String classCode;
    private String createdByName;
    private String createdByEmail;
    private String createdByRole;
}
