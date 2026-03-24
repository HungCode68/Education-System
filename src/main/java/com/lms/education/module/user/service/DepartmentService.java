package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.DepartmentDto;
import com.lms.education.module.user.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DepartmentService {

    DepartmentDto create(DepartmentDto dto);

    DepartmentDto update(String id, DepartmentDto dto);

    // Dùng xóa mềm (Chuyển isActive = false)
    void delete(String id);

    DepartmentDto getById(String id);

    // Lấy danh sách có phân trang và lọc cho Bảng quản trị
    Page<DepartmentDto> getAll(String keyword, Department.DepartmentType type, Boolean isActive, Pageable pageable);

    // Lấy toàn bộ phòng ban đang hoạt động (Dùng cho Dropdown chung)
    List<DepartmentDto> getAllActive();

    // Lấy phòng ban đang hoạt động theo loại (VD: Chỉ lấy Tổ chuyên môn 'academic')
    List<DepartmentDto> getActiveByType(Department.DepartmentType type);
}
