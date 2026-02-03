package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.GradeDto;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface GradeService {

    GradeDto create(GradeDto dto);

    GradeDto update(String id, GradeDto dto);

    void delete(String id);

    GradeDto getById(String id);

    // Tìm kiếm phân trang cho Admin
    PageResponse<GradeDto> search(String keyword, Boolean isActive, int page, int size);

    // Lấy danh sách Active (đã sort theo level) cho Dropdown
    List<GradeDto> getAllActive();
}
