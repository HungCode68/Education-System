package com.lms.education.module.lms_class.service;

import com.lms.education.module.lms_class.dto.OnlineClassDto;
import com.lms.education.utils.PageResponse; // Class util phân trang của bạn

import java.util.List;

public interface OnlineClassService {

    // Lấy chi tiết lớp online
    OnlineClassDto getById(String id);

    // Lấy danh sách lớp tôi dạy (Cho Giáo viên)
    List<OnlineClassDto> getMyClasses(String teacherId);

    // Tìm kiếm (Cho Admin)
    PageResponse<OnlineClassDto> search(String keyword, String status, int page, int size);

    // Cập nhật thông tin cơ bản (Đổi tên, Đổi trạng thái Archive)
    // Lưu ý: Không cho tạo mới ở đây, vì tạo mới do TeachingAssignment tự làm
    OnlineClassDto update(String id, OnlineClassDto dto);
}