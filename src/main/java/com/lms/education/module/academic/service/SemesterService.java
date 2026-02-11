package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.SemesterDto;
import com.lms.education.module.academic.entity.Semester;

import java.util.List;

public interface SemesterService {

    SemesterDto create(SemesterDto dto);

    SemesterDto update(String id, SemesterDto dto);

    void delete(String id);

    SemesterDto getById(String id);

    // Lấy danh sách học kỳ của một năm học (Sắp xếp theo thứ tự)
    List<SemesterDto> getAllBySchoolYear(String schoolYearId);

    // Cập nhật trạng thái (VD: Kích hoạt học kỳ này, đóng học kỳ kia)
    void updateStatus(String id, Semester.SemesterStatus status);
}
