package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.SubjectDto;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface SubjectService {

    // Tạo mới
    SubjectDto create(SubjectDto dto);

    // Cập nhật
    SubjectDto update(String id, SubjectDto dto);

    // Xóa
    void delete(String id);

    // Lấy chi tiết
    SubjectDto getById(String id);

    // Tìm kiếm & Phân trang (Admin dùng)
    PageResponse<SubjectDto> search(String keyword, Boolean isActive, int page, int size);

    // Lấy danh sách các môn đang hoạt động (Dùng cho Dropdown khi tạo lớp)
    List<SubjectDto> getAllActive();
}
