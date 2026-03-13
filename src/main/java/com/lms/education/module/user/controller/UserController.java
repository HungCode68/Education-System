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
    @PreAuthorize("hasAuthority('USER_UPDATE')") // Chỉ Admin có quyền này mới được đổi role
    public ResponseEntity<Map<String, String>> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        userService.changeUserRole(userId, request.getRoleCode());
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật quyền thành công!"));
    }
}
