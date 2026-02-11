package com.lms.education.module.teaching_assignment.service;

import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentHistoryDto;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignmentHistory;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.utils.PageResponse;

import java.util.List;

public interface TeachingAssignmentHistoryService {

    // --- HÀM GHI LOG (Dùng cho Internal Service gọi) ---
    void log(TeachingAssignment assignment,
             Teacher oldTeacher,
             Teacher newTeacher,
             TeachingAssignmentHistory.ActionType actionType,
             String reason,
             String changedBy);

    // --- HÀM TRA CỨU (Dùng cho Controller/UI) ---

    // Lấy lịch sử của 1 phân công
    List<TeachingAssignmentHistoryDto> getByAssignment(String assignmentId);

    // Lấy lịch sử biến động của 1 Lớp
    List<TeachingAssignmentHistoryDto> getByClass(String classId);

    // Lấy lịch sử biến động của 1 Giáo viên
    List<TeachingAssignmentHistoryDto> getByTeacher(String teacherId);

    // Tìm kiếm chung (Admin)
    PageResponse<TeachingAssignmentHistoryDto> search(String keyword, int page, int size);
}
