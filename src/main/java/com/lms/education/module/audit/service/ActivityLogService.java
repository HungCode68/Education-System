package com.lms.education.module.audit.service;

import com.lms.education.module.audit.dto.ActivityLogDto;
import com.lms.education.module.audit.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityLogService {

    void logAction(
            String module,
            String action,
            String targetType,
            String targetId,
            String oldValue,
            String newValue,
            String details,
            ActivityLog.LogStatus status
    );

    Page<ActivityLogDto> searchAndFilterLogs(
            String keyword,
            String module,
            String action,
            ActivityLog.LogStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<ActivityLogDto> getLogsByUserId(Long userId, Pageable pageable);

    ActivityLogDto getById(Long id);
}
