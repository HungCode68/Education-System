package com.lms.education.module.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.audit.entity.ActivityLog;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogDto {

    private Long id;
    private Long userId;
    private String actorName;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private ActivityLog.LogStatus status;
    private String ipAddress;
    private String userAgent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
