package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.AssignPermissionDto;
import com.lms.education.module.user.dto.RoleDto;
import com.lms.education.module.user.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    @LogActivity(module = "ROLE", action = "CREATE", targetType = "role", description = "Tạo mới vai trò hệ thống")
    public ResponseEntity<Map<String, Object>> createRole(@Valid @RequestBody RoleDto dto) {
        RoleDto createdRole = roleService.create(dto);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo vai trò thành công!");
        response.put("data", createdRole);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    @LogActivity(module = "ROLE", action = "UPDATE", targetType = "role", description = "Cập nhật thông tin vai trò")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto dto) {
        RoleDto updatedRole = roleService.update(id, dto);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật vai trò thành công!");
        response.put("data", updatedRole);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    @LogActivity(module = "ROLE", action = "DELETE", targetType = "role", description = "Xóa vai trò hệ thống")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long id) {
        roleService.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa vai trò thành công!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<Page<RoleDto>> getAllRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(roleService.getAllRoles(keyword, pageable));
    }

    @PostMapping("/assign-permissions")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN_PERMISSION')")
    @LogActivity(module = "ROLE", action = "ASSIGN", targetType = "permission", description = "Cấp quyền cho vai trò")
    public ResponseEntity<Map<String, Object>> assignPermissions(@Valid @RequestBody AssignPermissionDto dto) {
        RoleDto updatedRole = roleService.assignPermissions(dto);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cấp quyền cho vai trò thành công!");
        response.put("data", updatedRole);
        return ResponseEntity.ok(response);
    }
}