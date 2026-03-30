package com.lms.education.module.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.notification.entity.Announcement;
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
public class AnnouncementDto {

    private String id;

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    private String title;

    private String content;

    @NotNull(message = "Phạm vi thông báo (scope) không được để trống")
    private Announcement.AnnouncementScope scope;

    // Thông tin lớp học (Tùy thuộc vào scope, cái nào không dùng sẽ trả về null)
    private String physicalClassId;
    private String physicalClassName;

    private String onlineClassId;
    private String onlineClassName;

    // Quản lý file đính kèm
    private String attachmentPath; // Tên object trên MinIO
    private String attachmentUrl;  // Link tải thực tế (Service sẽ tự động fill)

    // Thông tin người đăng
    private String createdById;
    private String createdByName;
    private String createdByEmail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
