package com.lms.education.module.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.user.entity.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto {

    private String id;
    private String userId;
    private String actorName;
    private String module;
    private String action;
    private String targetType;
    private String targetId;

    // JSON chuỗi chứa chi tiết thay đổi (Cũ -> Mới)
    private String details;

    private ActivityLog.LogStatus status;
    private String ipAddress;
    private String userAgent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
