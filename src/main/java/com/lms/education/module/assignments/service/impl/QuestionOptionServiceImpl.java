package com.lms.education.module.assignments.service.impl;

import com.lms.education.module.assignments.dto.QuestionOptionDto;
import com.lms.education.module.assignments.entity.AssignmentQuestion; // Bạn tạo tạm một Entity rỗng nếu chưa có nhé
import com.lms.education.module.assignments.entity.QuestionOption;
import com.lms.education.module.assignments.repository.QuestionOptionRepository;
import com.lms.education.module.assignments.service.QuestionOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionOptionServiceImpl implements QuestionOptionService {

    private final QuestionOptionRepository questionOptionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionOptionDto> getByQuestionId(String questionId) {
        return questionOptionRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<QuestionOptionDto> saveOptionsForQuestion(String questionId, List<QuestionOptionDto> optionDtos) {
        // VALIDATE LOGIC BÀI TẬP TRẮC NGHIỆM
        if (optionDtos == null || optionDtos.size() < 2) {
            throw new IllegalArgumentException("Lỗi: Một câu hỏi trắc nghiệm phải có ít nhất 2 đáp án (Ví dụ: Đúng/Sai hoặc A/B/C/D)!");
        }

        long correctCount = optionDtos.stream().filter(dto -> Boolean.TRUE.equals(dto.getIsCorrect())).count();
        if (correctCount == 0) {
            throw new IllegalArgumentException("Lỗi: Vui lòng chọn ít nhất 1 đáp án đúng cho câu hỏi này!");
        }

        // XÓA SẠCH ĐÁP ÁN CŨ (Tuyệt chiêu giúp cập nhật siêu dễ dàng, không sợ rác dữ liệu)
        questionOptionRepository.deleteByQuestionId(questionId);

        // CHUẨN BỊ LIÊN KẾT KHÓA NGOẠI
        AssignmentQuestion questionRef = new AssignmentQuestion();
        questionRef.setId(questionId);

        // MAP VÀ INSERT DANH SÁCH MỚI
        List<QuestionOption> newOptions = optionDtos.stream().map(dto -> {
            return QuestionOption.builder()
                    .question(questionRef)
                    .displayOrder(dto.getDisplayOrder())
                    .optionText(dto.getOptionText())
                    .isCorrect(dto.getIsCorrect() != null ? dto.getIsCorrect() : false)
                    .build();
        }).collect(Collectors.toList());

        List<QuestionOption> savedOptions = questionOptionRepository.saveAll(newOptions);
        log.info("Đã lưu thành công {} đáp án cho Câu hỏi ID: {}", savedOptions.size(), questionId);

        return savedOptions.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByQuestionId(String questionId) {
        questionOptionRepository.deleteByQuestionId(questionId);
        log.info("Đã xóa toàn bộ đáp án của Câu hỏi ID: {}", questionId);
    }

    // --- Hàm Helper ---
    private QuestionOptionDto mapToDto(QuestionOption option) {
        return QuestionOptionDto.builder()
                .id(option.getId())
                .questionId(option.getQuestion().getId())
                .displayOrder(option.getDisplayOrder())
                .optionText(option.getOptionText())
                .isCorrect(option.getIsCorrect())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }
}
