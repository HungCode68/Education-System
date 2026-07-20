package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @LogActivity(module = "USER", action = "CREATE", targetType = "user", description = "Tạo mới tài khoản người dùng")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto createdUser = userService.createUser(userDto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo tài khoản thành công!");
        response.put("data", createdUser);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email")
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    public ResponseEntity<UserDto> getUserByEmail(@RequestParam String email) {
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(userService.getUserById(user.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @LogActivity(module = "USER", action = "UPDATE", targetType = "user", description = "Cập nhật trạng thái tài khoản")
    public ResponseEntity<Map<String, String>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusRequest) {

        String status = statusRequest.get("status");
        userService.updateUserStatus(id, status);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cập nhật trạng thái người dùng thành công sang: " + status);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/logout-session")
    @LogActivity(module = "AUTH", action = "DELETE", targetType = "session", description = "Buộc đăng xuất phiên làm việc")
    public ResponseEntity<Map<String, String>> forceLogout(@RequestParam String refreshToken) {
        userService.deleteRefreshToken(refreshToken);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã hủy phiên đăng nhập và xóa token thành công.");

        return ResponseEntity.ok(response);
    }
}