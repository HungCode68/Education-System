package com.lms.education.module.teaching_assignment.service;

import com.lms.education.module.teaching_assignment.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface TeachingSubstitutionService {

    // Tạo yêu cầu dạy thay mới (Có validate trùng lịch)
    TeachingSubstitutionDto create(TeachingSubstitutionDto dto);

    // Hủy yêu cầu dạy thay (Chuyển status sang cancelled)
    void cancel(String id);

    // Duyệt yêu cầu (Nếu dùng workflow duyệt)
    void updateStatus(String id, TeachingSubstitution.SubstitutionStatus status);

    // Tìm kiếm và phân trang (Cho Admin)
    PageResponse<TeachingSubstitutionDto> search(
            String schoolYearId,
            String semesterId,
            String keyword,
            int page,
            int size
    );

    // Lấy danh sách dạy thay của một giáo viên (Cho GV xem lịch mình phải dạy thay)
    List<TeachingSubstitutionDto> getBySubTeacher(String teacherId);
}
