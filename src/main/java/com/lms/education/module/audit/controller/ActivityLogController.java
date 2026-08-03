package com.lms.education.module.audit.controller;

import com.lms.education.module.audit.dto.ActivityLogDto;
import com.lms.education.module.audit.entity.ActivityLog;
import com.lms.education.module.audit.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /**
     * LẤY DANH SÁCH NHẬT KÝ HOẠT ĐỘNG (Có phân trang và bộ lọc)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('LOG_VIEW')")
    public ResponseEntity<Page<ActivityLogDto>> getAllLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) ActivityLog.LogStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ActivityLogDto> logs = activityLogService.searchAndFilterLogs(
                keyword, module, action, status, startDate, endDate, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * LẤY NHẬT KÝ HOẠT ĐỘNG CỦA MỘT NGƯỜI DÙNG
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('LOG_VIEW')")
    public ResponseEntity<Page<ActivityLogDto>> getLogsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ActivityLogDto> logs = activityLogService.getLogsByUserId(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * LẤY CHI TIẾT MỘT NHẬT KÝ HOẠT ĐỘNG
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LOG_VIEW')")
    public ResponseEntity<ActivityLogDto> getLogById(@PathVariable Long id) {
        return ResponseEntity.ok(activityLogService.getById(id));
    }
}
