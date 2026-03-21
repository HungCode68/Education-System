package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.AssignPermissionDto;
import com.lms.education.module.user.dto.RoleDto;
import com.lms.education.module.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    RoleDto create(RoleDto dto);

    RoleDto update(String id, RoleDto dto);

    // Đối với Role, thường dùng xóa mềm.
    void delete(String id);

    RoleDto getById(String id);

    RoleDto getByCode(String code);

    // Hàm lấy danh sách có hỗ trợ tìm kiếm và lọc theo trạng thái
    Page<RoleDto> getAllRoles(String keyword, Role.RoleStatus status, Pageable pageable);

    // Hàm cấp quyền cho Vai trò
    RoleDto assignPermissions(AssignPermissionDto dto);
}
