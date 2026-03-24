package com.lms.education.module.user.service.impl;

import com.lms.education.module.user.dto.ActivityLogDto;
import com.lms.education.module.user.entity.ActivityLog;
import com.lms.education.module.user.repository.ActivityLogRepository;
import com.lms.education.module.user.service.ActivityLogService;
import com.lms.education.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Override
    @Transactional
    public void logAction(String module, String action, String targetType, String targetId, String details, ActivityLog.LogStatus status) {
        try {
            String userId = null;
            String actorName = "Hệ thống / Vô danh";
            String ipAddress = null;
            String userAgent = null;

            // TỰ ĐỘNG LẤY NGƯỜI DÙNG HIỆN TẠI (Từ Spring Security)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
                userId = userDetails.getId();
                actorName = userDetails.getEmail(); // Lấy email làm tên người thao tác
            }

            // TỰ ĐỘNG LẤY IP VÀ USER AGENT (Từ Request Context)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }

            // ĐÓNG GÓI VÀ LƯU XUỐNG DATABASE
            ActivityLog logEntry = ActivityLog.builder()
                    .userId(userId)
                    .actorName(actorName)
                    .module(module)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details(details) // Chuỗi JSON chứa thông tin cũ/mới
                    .status(status != null ? status : ActivityLog.LogStatus.success)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            activityLogRepository.save(logEntry);

        } catch (Exception e) {
            // CỰC KỲ QUAN TRỌNG: Ghi log thất bại thì KHÔNG ĐƯỢC làm sập chức năng chính
            // Ví dụ: Đang tạo học sinh mà lỗi ghi log thì học sinh vẫn phải được tạo thành công
            log.error("Lỗi hệ thống khi cố gắng ghi lại Activity Log: ", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> searchAndFilterLogs(String keyword, String module, String action, ActivityLog.LogStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.searchAndFilterLogs(keyword, module, action, status, startDate, endDate, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getUserLogs(String userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    // HÀM HELPER: Lấy IP chuẩn xác (Ngay cả khi hệ thống chạy qua Nginx / Proxy)
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Nếu đi qua nhiều proxy, X-Forwarded-For sẽ trả về chuỗi IP (VD: "ip1, ip2"), ta chỉ lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    // --- Hàm Helper Map Entity to DTO ---
    private ActivityLogDto mapToDto(ActivityLog log) {
        return ActivityLogDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .actorName(log.getActorName())
                .module(log.getModule())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .details(log.getDetails())
                .status(log.getStatus())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
