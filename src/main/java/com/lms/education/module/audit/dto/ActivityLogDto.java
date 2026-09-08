package com.lms.education.module.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.audit.entity.ActivityLog;
import lombok.*;

import java.time.Instant;

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
    private String method;
    private String endpoint;
    private String oldValue;
    private String newValue;
    private String details;
    private ActivityLog.LogStatus status;
    private String ipAddress;
    private String userAgent;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;
}
