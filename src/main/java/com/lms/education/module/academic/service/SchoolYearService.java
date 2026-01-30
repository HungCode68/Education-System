package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.SchoolYearDto;
import com.lms.education.module.academic.entity.SchoolYear.SchoolYearStatus;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface SchoolYearService {

    // Tạo mới năm học
    SchoolYearDto create(SchoolYearDto dto);

    // Cập nhật năm học
    SchoolYearDto update(String id, SchoolYearDto dto);

    // Xóa năm học
    void delete(List<String> ids);

    // Lấy chi tiết 1 năm học
    SchoolYearDto getById(String id);

    // Lấy danh sách tất cả
    PageResponse<SchoolYearDto> getAll(String keyword, SchoolYearStatus status, int page, int size);

    // Lấy năm học hiện tại (dựa theo ngày hôm nay)
    SchoolYearDto getCurrentSchoolYear();

    // Hàm kết thúc năm học
    void archive(String id);
}
