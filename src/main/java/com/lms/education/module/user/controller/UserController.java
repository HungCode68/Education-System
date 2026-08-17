package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @LogActivity(module = "USER", action = "UPDATE", targetType = "user", description = "Cập nhật thông tin tài khoản")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @RequestBody UserDto userDto) {

        UserDto updatedUser = userService.updateUser(id, userDto.getFullName(), userDto.getRoles() != null ? new ArrayList<>(userDto.getRoles()) : null);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin tài khoản thành công!");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @LogActivity(module = "USER", action = "UPDATE", targetType = "user", description = "Gán lại vai trò cho tài khoản")
    public ResponseEntity<Map<String, Object>> updateUserRoles(
            @PathVariable Long id,
            @RequestBody List<String> roleNames) {

        UserDto updatedUser = userService.updateUserRoles(id, roleNames);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật vai trò tài khoản thành công!");
        response.put("data", updatedUser);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @LogActivity(module = "USER", action = "UPDATE", targetType = "user", description = "Đặt lại mật khẩu tài khoản")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> passwordRequest) {

        String newPassword = passwordRequest.get("newPassword");
        userService.resetPassword(id, newPassword);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Đặt lại mật khẩu thành công!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_DELETE')")
    @LogActivity(module = "USER", action = "DELETE", targetType = "user", description = "Thu hồi/Xóa tài khoản")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã thu hồi tài khoản thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(userService.getAllUsers(keyword, pageable));
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