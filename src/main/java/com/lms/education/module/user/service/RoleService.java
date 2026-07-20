package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.AssignPermissionDto;
import com.lms.education.module.user.dto.RoleDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    RoleDto create(RoleDto dto);

    RoleDto update(Long id, RoleDto dto);

    void delete(Long id);

    RoleDto getById(Long id);

    RoleDto getByName(String name);

    // Lấy danh sách Role có hỗ trợ tìm kiếm theo từ khóa
    Page<RoleDto> getAllRoles(String keyword, Pageable pageable);

    // Hàm cấp quyền cho Vai trò
    RoleDto assignPermissions(AssignPermissionDto dto);
}