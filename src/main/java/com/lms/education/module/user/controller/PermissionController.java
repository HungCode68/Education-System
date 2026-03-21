package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.PermissionDto;
import com.lms.education.module.user.entity.Permission;
import com.lms.education.module.user.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * TẠO MỚI QUYỀN (PERMISSION)
     */
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody PermissionDto dto) {
        log.info("REST request - Tạo mới quyền: {}", dto.getCode());
        PermissionDto createdPermission = permissionService.create(dto);
        return new ResponseEntity<>(createdPermission, HttpStatus.CREATED);
    }

    /**
     * CẬP NHẬT QUYỀN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<PermissionDto> updatePermission(
            @PathVariable Integer id,
            @Valid @RequestBody PermissionDto dto) {
        log.info("REST request - Cập nhật quyền ID: {}", id);
        PermissionDto updatedPermission = permissionService.update(id, dto);
        return ResponseEntity.ok(updatedPermission);
    }

    /**
     * XÓA QUYỀN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> deletePermission(@PathVariable Integer id) {
        log.info("REST request - Xóa quyền ID: {}", id);
        permissionService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa permission thành công"));
    }

    /**
     * LẤY CHI TIẾT 1 QUYỀN THEO ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable Integer id) {
        PermissionDto permission = permissionService.getById(id);
        return ResponseEntity.ok(permission);
    }

    /**
     * LẤY DANH SÁCH QUYỀN THEO NHÓM (SCOPE)
     * Rất hữu ích khi Frontend cần render các Checkbox phân quyền theo từng cụm (VD: nhóm quyền CLASS, nhóm quyền ASSIGNMENT...)
     */
    @GetMapping("/scope/{scope}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<PermissionDto>> getPermissionsByScope(@PathVariable Permission.PermissionScope scope) {
        List<PermissionDto> permissions = permissionService.getByScope(scope);
        return ResponseEntity.ok(permissions);
    }

    /**
     * LẤY DANH SÁCH TẤT CẢ CÁC QUYỀN (Có phân trang, tìm kiếm và lọc theo Scope)
     * Dành cho màn hình Bảng Quản trị (Data Table)
     */
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Page<PermissionDto>> getAllPermissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Permission.PermissionScope scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy, // Bảng permission thường sort theo ID cho dễ nhìn
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PermissionDto> permissions = permissionService.getAllPermissions(keyword, scope, pageable);
        return ResponseEntity.ok(permissions);
    }
}
