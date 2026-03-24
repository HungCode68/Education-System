package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.ActivityLogDto;
import com.lms.education.module.user.entity.ActivityLog;
import com.lms.education.module.user.service.ActivityLogService;
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
     * LẤY DANH SÁCH NHẬT KÝ HOẠT ĐỘNG (Có phân trang và bộ lọc siêu cấp)
     * Dành cho màn hình "Quản lý hệ thống / Nhật ký hoạt động"
     */
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')") // Thường chỉ có System Admin mới được xem toàn bộ log hệ thống
    public ResponseEntity<Page<ActivityLogDto>> getAllLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) ActivityLog.LogStatus status,
            // Cấu hình để nhận chuỗi thời gian từ Frontend (VD: 2026-03-24T00:00:00)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) { // Mặc định hiển thị log mới nhất lên đầu

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ActivityLogDto> logs = activityLogService.searchAndFilterLogs(
                keyword, module, action, status, startDate, endDate, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * LẤY NHẬT KÝ HOẠT ĐỘNG CỦA MỘT NGƯỜI DÙNG CỤ THỂ
     * Dành cho màn hình "Xem chi tiết hồ sơ Học sinh / Giáo viên" -> Tab "Lịch sử hoạt động"
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Page<ActivityLogDto>> getUserLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Luôn sắp xếp mới nhất lên đầu khi xem log của 1 user
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ActivityLogDto> logs = activityLogService.getUserLogs(userId, pageable);
        return ResponseEntity.ok(logs);
    }
}
