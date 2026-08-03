package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.AccountProvisionDto;
import com.lms.education.module.user.dto.StaffDto;
import com.lms.education.module.user.service.StaffService;
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
@RequestMapping("/api/v1/staffs")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF_CREATE')")
    @LogActivity(module = "STAFF", action = "CREATE", targetType = "staff", description = "Tạo mới hồ sơ nhân sự")
    public ResponseEntity<Map<String, Object>> createStaff(@Valid @RequestBody StaffDto dto) {
        StaffDto createdStaff = staffService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo hồ sơ nhân sự thành công!");
        response.put("data", createdStaff);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF_UPDATE')")
    @LogActivity(module = "STAFF", action = "UPDATE", targetType = "staff", description = "Cập nhật thông tin nhân sự")
    public ResponseEntity<Map<String, Object>> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffDto dto) {

        StaffDto updatedStaff = staffService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin nhân sự thành công!");
        response.put("data", updatedStaff);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF_DELETE')")
    @LogActivity(module = "STAFF", action = "DELETE", targetType = "staff", description = "Xóa hồ sơ nhân sự")
    public ResponseEntity<Map<String, String>> deleteStaff(@PathVariable Long id) {
        staffService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa hồ sơ nhân sự thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public ResponseEntity<java.util.List<StaffDto>> getTeachers() {
        return ResponseEntity.ok(staffService.getTeachers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public ResponseEntity<StaffDto> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getById(id));
    }

    @GetMapping("/code/{staffCode}")
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public ResponseEntity<StaffDto> getStaffByCode(@PathVariable String staffCode) {
        return ResponseEntity.ok(staffService.getByStaffCode(staffCode));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public ResponseEntity<Page<StaffDto>> getAllStaffs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy, // Thường danh sách nhân sự nên hiển thị người mới
                                                                     // tạo lên đầu
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(staffService.getAllStaffs(keyword, pageable));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('STAFF_VIEW')")
    public ResponseEntity<Page<StaffDto>> getStaffsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(staffService.getStaffsByDepartmentId(departmentId, pageable));
    }

    @PostMapping("/provision-accounts")
    @PreAuthorize("hasAuthority('STAFF_PROVISION')")
    @LogActivity(module = "STAFF", action = "PROVISION", targetType = "user", description = "Cấp tài khoản hàng loạt cho nhân sự")
    public ResponseEntity<Map<String, Object>> provisionAccounts(@Valid @RequestBody AccountProvisionDto dto) {

        Map<String, Object> report = staffService.provisionAccounts(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tiến trình cấp tài khoản đã hoàn tất!");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }
}