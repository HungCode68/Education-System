package com.lms.education.module.assignments.service;

import com.lms.education.module.assignments.dto.AssignmentQuestionDto;
import java.util.List;

public interface AssignmentQuestionService {

    AssignmentQuestionDto create(AssignmentQuestionDto dto, String userId);

    AssignmentQuestionDto update(String id, AssignmentQuestionDto dto, String userId);

    void delete(String id, String userId);

    AssignmentQuestionDto getById(String id);

    // Lấy toàn bộ danh sách câu hỏi của một bài tập (Sắp xếp theo thứ tự)
    List<AssignmentQuestionDto> getByAssignmentId(String assignmentId);
}
