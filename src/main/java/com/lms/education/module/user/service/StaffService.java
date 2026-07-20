package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.StaffDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffService {

    StaffDto create(StaffDto dto);

    StaffDto update(Long id, StaffDto dto);

    void delete(Long id);

    StaffDto getById(Long id);

    StaffDto getByStaffCode(String staffCode);

    Page<StaffDto> getAllStaffs(String keyword, Pageable pageable);

    Page<StaffDto> getStaffsByDepartmentId(Long departmentId, Pageable pageable);

    // Cấp tài khoản tự động hàng loạt
    java.util.Map<String, Object> provisionAccounts(com.lms.education.module.user.dto.AccountProvisionDto dto);
}