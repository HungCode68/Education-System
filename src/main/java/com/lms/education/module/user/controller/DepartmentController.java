package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.DepartmentDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.service.DepartmentService;
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

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * TẠO MỚI PHÒNG BAN / TỔ BỘ MÔN
     */
    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_UPDATE') or hasAuthority('DEPARTMENT_CREATE')")
    @LogActivity(module = "DEPARTMENT", action = "CREATE", targetType = "departments", description = "Tạo mới phòng ban/tổ bộ môn")
    public ResponseEntity<DepartmentDto> create(@Valid @RequestBody DepartmentDto dto) {
        log.info("REST request - Tạo mới phòng ban: {}", dto.getName());
        return new ResponseEntity<>(departmentService.create(dto), HttpStatus.CREATED);
    }

    /**
     * CẬP NHẬT PHÒNG BAN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_UPDATE') or hasAuthority('DEPARTMENT_UPDATE')")
    public ResponseEntity<DepartmentDto> update(
            @PathVariable String id,
            @Valid @RequestBody DepartmentDto dto) {
        log.info("REST request - Cập nhật phòng ban ID: {}", id);
        return ResponseEntity.ok(departmentService.update(id, dto));
    }

    /**
     * XÓA PHÒNG BAN (Tự động nhận diện Xóa cứng hoặc Xóa mềm)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_UPDATE') or hasAuthority('DEPARTMENT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("REST request - Xóa phòng ban ID: {}", id);
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * LẤY CHI TIẾT 1 PHÒNG BAN
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<DepartmentDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    /**
     * LẤY DANH SÁCH CÓ PHÂN TRANG VÀ LỌC (Dùng cho Bảng quản trị)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<Page<DepartmentDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Department.DepartmentType type,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DepartmentDto> departments = departmentService.getAll(keyword, type, isActive, pageable);

        return ResponseEntity.ok(departments);
    }

    /**
     * LẤY TẤT CẢ PHÒNG BAN ĐANG HOẠT ĐỘNG (Dùng cho Dropdown chung)
     */
    @GetMapping("/active")
    public ResponseEntity<List<DepartmentDto>> getAllActive() {
        return ResponseEntity.ok(departmentService.getAllActive());
    }

    /**
     * LẤY PHÒNG BAN ĐANG HOẠT ĐỘNG THEO LOẠI (Dùng cho Dropdown phân loại)
     * VD: /api/departments/active/type/academic -> Lấy các Tổ bộ môn để phân công dạy
     */
    @GetMapping("/active/type/{type}")
    public ResponseEntity<List<DepartmentDto>> getActiveByType(@PathVariable Department.DepartmentType type) {
        return ResponseEntity.ok(departmentService.getActiveByType(type));
    }
}
