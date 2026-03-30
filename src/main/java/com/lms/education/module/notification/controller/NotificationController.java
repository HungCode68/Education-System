package com.lms.education.module.notification.controller;

import com.lms.education.module.notification.dto.NotificationDto;
import com.lms.education.module.notification.service.NotificationService;
import com.lms.education.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * TẤT CẢ USER: LẤY DANH SÁCH THÔNG BÁO CỦA MÌNH (Phân trang)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()") // Bất kỳ ai đăng nhập cũng có quyền xem thông báo của mình
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang lấy danh sách thông báo (Trang {})", userId, page);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<NotificationDto> notifications = notificationService.getUserNotifications(userId, pageable);

        return ResponseEntity.ok(notifications);
    }

    /**
     * TẤT CẢ USER: LẤY SỐ LƯỢNG THÔNG BÁO CHƯA ĐỌC
     * (Frontend sẽ gọi API này thường xuyên để update số đỏ trên cái chuông)
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {

        String userId = getUserId(principal);
        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * TẤT CẢ USER: ĐÁNH DẤU 1 THÔNG BÁO LÀ ĐÃ ĐỌC
     * (Gọi API này khi User click chuột vào 1 thông báo cụ thể)
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable String id,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đánh dấu đã đọc thông báo ID: {}", userId, id);

        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đọc thành công"));
    }

    /**
     * TẤT CẢ USER: ĐÁNH DẤU TẤT CẢ LÀ ĐÃ ĐỌC
     * (Gọi API này khi User bấm nút "Mark all as read")
     */
    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAllAsRead(Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đánh dấu ĐÃ ĐỌC TẤT CẢ", userId);

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đọc tất cả thông báo"));
    }


    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}
