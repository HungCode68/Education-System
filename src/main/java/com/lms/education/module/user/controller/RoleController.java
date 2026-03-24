package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.AssignPermissionDto;
import com.lms.education.module.user.dto.RoleDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.service.RoleService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    /**
     * TẠO MỚI VAI TRÒ
     */
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleDto dto) {
        log.info("REST request - Tạo mới vai trò: {}", dto.getCode());
        RoleDto createdRole = roleService.create(dto);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    /**
     * CẬP NHẬT VAI TRÒ
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable String id,
            @Valid @RequestBody RoleDto dto) {
        log.info("REST request - Cập nhật vai trò ID: {}", id);
        RoleDto updatedRole = roleService.update(id, dto);
        return ResponseEntity.ok(updatedRole);
    }

    /**
     * XÓA VAI TRÒ (Xóa mềm)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable String id) {
        log.info("REST request - Xóa vai trò ID: {}", id);
        roleService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xử lý xóa vai trò thành công"));
    }

    /**
     * LẤY CHI TIẾT 1 VAI TRÒ THEO ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable String id) {
        RoleDto role = roleService.getById(id);
        return ResponseEntity.ok(role);
    }

    /**
     * LẤY CHI TIẾT 1 VAI TRÒ THEO MÃ CODE (VD: /api/v1/roles/code/STUDENT)
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RoleDto> getRoleByCode(@PathVariable String code) {
        RoleDto role = roleService.getByCode(code);
        return ResponseEntity.ok(role);
    }

    /**
     * LẤY DANH SÁCH VAI TRÒ (Có phân trang, tìm kiếm và lọc trạng thái)
     */
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Page<RoleDto>> getAllRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role.RoleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RoleDto> roles = roleService.getAllRoles(keyword, status, pageable);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/assign-permissions")
    public ResponseEntity<RoleDto> assignPermissions(@Valid @RequestBody AssignPermissionDto dto) {
        log.info("REST request - Cấp quyền cho Vai trò ID: {}", dto.getRoleId());

        RoleDto updatedRole = roleService.assignPermissions(dto);

        return ResponseEntity.ok(updatedRole);
    }
}
