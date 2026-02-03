package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.GradeSubjectDto;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface GradeSubjectService {

    // Tạo mới cấu hình môn học cho khối
    GradeSubjectDto create(GradeSubjectDto dto);

    // Cập nhật cấu hình (VD: đổi thứ tự, tắt LMS, đổi loại môn)
    GradeSubjectDto update(String id, GradeSubjectDto dto);

    // Xóa môn học khỏi khối
    void delete(String id);

    // Lấy chi tiết
    GradeSubjectDto getById(String id);

    // Tìm kiếm phân trang
    PageResponse<GradeSubjectDto> search(String gradeId, String keyword, int page, int size);

    // Lấy danh sách môn học của một khối
    List<GradeSubjectDto> getByGradeId(String gradeId, boolean onlyLmsEnabled);
}
