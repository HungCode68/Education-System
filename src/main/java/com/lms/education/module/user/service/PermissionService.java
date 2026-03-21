package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.PermissionDto;
import com.lms.education.module.user.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PermissionService {

    PermissionDto create(PermissionDto dto);

    PermissionDto update(Integer id, PermissionDto dto);

    void delete(Integer id);

    PermissionDto getById(Integer id);

    // Lấy danh sách quyền theo nhóm (Dùng để nhóm các checkbox trên giao diện phân quyền)
    List<PermissionDto> getByScope(Permission.PermissionScope scope);

    // Lấy danh sách có hỗ trợ tìm kiếm và phân trang (Dùng cho bảng quản lý Permission)
    Page<PermissionDto> getAllPermissions(String keyword, Permission.PermissionScope scope, Pageable pageable);
}
