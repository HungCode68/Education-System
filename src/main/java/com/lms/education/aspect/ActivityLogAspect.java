package com.lms.education.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.entity.ActivityLog;
import com.lms.education.module.user.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;

    // Dùng ObjectMapper để chuyển đổi text thông thường sang chuẩn JSON an toàn
    private final ObjectMapper objectMapper;

    // TỰ ĐỘNG GHI LOG NẾU HÀM CHẠY THÀNH CÔNG
    @AfterReturning(pointcut = "@annotation(logActivity)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        String detailsJson = "{}";
        try {
            // Đóng gói thành định dạng chuẩn {"message": "Hành động: ..."}
            Map<String, String> detailsMap = new HashMap<>();
            detailsMap.put("message", "Hành động: " + logActivity.description());
            detailsJson = objectMapper.writeValueAsString(detailsMap);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse JSON khi ghi log: ", e);
        }

        activityLogService.logAction(
                logActivity.module(),
                logActivity.action(),
                logActivity.targetType(),
                null,
                detailsJson, // Truyền chuỗi đã format chuẩn JSON xuống DB
                ActivityLog.LogStatus.success
        );
    }

    // TỰ ĐỘNG GHI LOG NẾU HÀM BỊ LỖI
    @AfterThrowing(pointcut = "@annotation(logActivity)", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, LogActivity logActivity, Exception exception) {
        String detailsJson = "{}";
        try {
            // Đóng gói thành định dạng chuẩn {"error": "Lỗi: ..."}
            Map<String, String> detailsMap = new HashMap<>();
            detailsMap.put("error", "Lỗi: " + exception.getMessage());
            detailsJson = objectMapper.writeValueAsString(detailsMap);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse JSON khi ghi log lỗi: ", e);
        }

        activityLogService.logAction(
                logActivity.module(),
                logActivity.action(),
                logActivity.targetType(),
                null,
                detailsJson, // Truyền chuỗi chuẩn JSON
                ActivityLog.LogStatus.error
        );
    }
}
