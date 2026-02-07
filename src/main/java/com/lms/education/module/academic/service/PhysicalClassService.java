package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.PhysicalClassDto;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface PhysicalClassService {

    // Tạo lớp mới
    PhysicalClassDto create(PhysicalClassDto dto);

    // Cập nhật thông tin lớp (Tên, Phòng, GVCN...)
    PhysicalClassDto update(String id, PhysicalClassDto dto);

    // Xóa lớp
    void delete(String id);

    // Lấy chi tiết
    PhysicalClassDto getById(String id);

    // Tìm kiếm phân trang
    PageResponse<PhysicalClassDto> search(String schoolYearId, String gradeId, String keyword, int page, int size);

    // Lấy danh sách lớp theo Năm và Khối (Dùng cho Dropdown)
    List<PhysicalClassDto> getDropdownList(String schoolYearId, String gradeId);
}