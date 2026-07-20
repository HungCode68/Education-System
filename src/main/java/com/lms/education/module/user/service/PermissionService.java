package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.PermissionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionService {

    PermissionDto create(PermissionDto dto);

    PermissionDto update(Long id, PermissionDto dto);

    void delete(Long id);

    PermissionDto getById(Long id);

    // Lấy danh sách có hỗ trợ tìm kiếm và phân trang
    Page<PermissionDto> getAllPermissions(String keyword, Pageable pageable);
}