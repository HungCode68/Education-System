package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.ActivityLogDto;
import com.lms.education.module.user.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityLogService {

    // Dùng để gọi ở mọi nơi trong code khi muốn ghi nhận một hành động
    void logAction(String module, String action, String targetType, String targetId, String details, ActivityLog.LogStatus status);

    // Lấy toàn bộ log (Có lọc và phân trang cho Admin)
    Page<ActivityLogDto> searchAndFilterLogs(
            String keyword, String module, String action, ActivityLog.LogStatus status,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Lấy log của một User cụ thể
    Page<ActivityLogDto> getUserLogs(String userId, Pageable pageable);
}
