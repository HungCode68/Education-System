package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.PermissionDto;
import com.lms.education.module.user.service.PermissionService;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    @LogActivity(module = "PERMISSION", action = "CREATE", targetType = "permission", description = "Tạo mới quyền hệ thống")
    public ResponseEntity<Map<String, Object>> createPermission(@Valid @RequestBody PermissionDto dto) {
        PermissionDto createdPermission = permissionService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo quyền hệ thống thành công!");
        response.put("data", createdPermission);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    @LogActivity(module = "PERMISSION", action = "UPDATE", targetType = "permission", description = "Cập nhật quyền hệ thống")
    public ResponseEntity<Map<String, Object>> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody PermissionDto dto) {

        PermissionDto updatedPermission = permissionService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật quyền thành công!");
        response.put("data", updatedPermission);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    @LogActivity(module = "PERMISSION", action = "DELETE", targetType = "permission", description = "Xóa quyền hệ thống")
    public ResponseEntity<Map<String, String>> deletePermission(@PathVariable Long id) { // Đổi Integer thành Long
        permissionService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa quyền thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_VIEW')")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable Long id) { // Đổi Integer thành Long
        return ResponseEntity.ok(permissionService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSION_VIEW')")
    public ResponseEntity<Page<PermissionDto>> getAllPermissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(permissionService.getAllPermissions(keyword, pageable));
    }
}