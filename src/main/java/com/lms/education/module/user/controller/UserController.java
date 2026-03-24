package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.ChangeRoleRequest;
import com.lms.education.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // PATCH /api/v1/users/{userId}/role
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('USER_UPDATE')") // Chỉ Admin mới được đổi role
    public ResponseEntity<Map<String, String>> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        userService.changeUserRole(userId, request.getRoleCode());
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật quyền thành công!"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<org.springframework.data.domain.Page<com.lms.education.module.user.dto.UserDto>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.lms.education.module.user.entity.User.UserStatus status,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc")
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

        return ResponseEntity.ok(userService.getAllUsers(keyword, status, roleCode, pageable));
    }

    /**
     * LẤY CHI TIẾT 1 TÀI KHOẢN
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<com.lms.education.module.user.dto.UserDto> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * KHÓA / MỞ KHÓA TÀI KHOẢN (Đổi Status)
     * Body truyền lên dạng chuỗi string, VD: "inactive" hoặc "suspended"
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> request) {

        String statusStr = request.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không được để trống!"));
        }

        try {
            com.lms.education.module.user.entity.User.UserStatus status =
                    com.lms.education.module.user.entity.User.UserStatus.valueOf(statusStr.toLowerCase());

            userService.updateUserStatus(id, status);
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái tài khoản thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không hợp lệ! (Chỉ nhận active, inactive, suspended)"));
        }
    }

    // PATCH /api/v1/users/{userId}/reset-password
    @PatchMapping("/{userId}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable String userId) {
        userService.resetPassword(userId);
        return ResponseEntity.ok(Map.of("message", "Đã đặt lại mật khẩu về mã mặc định thành công!"));
    }
}
