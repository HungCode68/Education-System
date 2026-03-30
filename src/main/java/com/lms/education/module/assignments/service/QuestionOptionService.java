package com.lms.education.module.assignments.service;

import com.lms.education.module.assignments.dto.QuestionOptionDto;

import java.util.List;

public interface QuestionOptionService {

    // Lấy danh sách đáp án của 1 câu hỏi (Sắp xếp theo thứ tự A, B, C, D)
    List<QuestionOptionDto> getByQuestionId(String questionId);

    // Xử lý tạo mới/cập nhật danh sách đáp án cho 1 câu hỏi
    List<QuestionOptionDto> saveOptionsForQuestion(String questionId, List<QuestionOptionDto> optionDtos);

    // Xóa toàn bộ đáp án của 1 câu hỏi
    void deleteByQuestionId(String questionId);
}
