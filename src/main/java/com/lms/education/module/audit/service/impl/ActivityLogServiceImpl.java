package com.lms.education.module.audit.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.audit.dto.ActivityLogDto;
import com.lms.education.module.audit.entity.ActivityLog;
import com.lms.education.module.audit.repository.ActivityLogRepository;
import com.lms.education.module.audit.service.ActivityLogService;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void logAction(String module, String action, String targetType, String targetId, String oldValue, String newValue, String details, ActivityLog.LogStatus status) {
        try {
            Long userId = null;
            String actorName = "System / Anonymous";
            String ipAddress = null;
            String userAgent = null;
            String method = null;
            String endpoint = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    userId = user.getId();
                    actorName = user.getEmail();
                } else {
                    actorName = email;
                }
            }

            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                if (request != null) {
                    ipAddress = getClientIp(request);
                    userAgent = request.getHeader("User-Agent");
                    method = request.getMethod();
                    endpoint = request.getRequestURI();
                }
            }

            ActivityLog entity = ActivityLog.builder()
                    .userId(userId)
                    .actorName(actorName)
                    .module(module)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .method(method)
                    .endpoint(endpoint)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .details(details)
                    .status(status != null ? status : ActivityLog.LogStatus.success)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            activityLogRepository.save(entity);
            log.debug("Đã ghi nhật ký hoạt động: [{} - {}] bởi {}", module, action, actorName);
        } catch (Exception e) {
            log.error("Lỗi khi ghi nhật ký hoạt động xuống DB: ", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> searchAndFilterLogs(String keyword, String module, String action, ActivityLog.LogStatus status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.searchAndFilterLogs(keyword, module, action, status, startDate, endDate, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> getLogsByUserId(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityLogDto getById(Long id) {
        ActivityLog logEntity = activityLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký hoạt động có ID: " + id));
        return toDto(logEntity);
    }

    private ActivityLogDto toDto(ActivityLog entity) {
        return ActivityLogDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .actorName(entity.getActorName())
                .module(entity.getModule())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .method(entity.getMethod())
                .endpoint(entity.getEndpoint())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .details(entity.getDetails())
                .status(entity.getStatus())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
