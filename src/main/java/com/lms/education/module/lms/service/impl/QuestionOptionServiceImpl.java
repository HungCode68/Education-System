package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.entity.QuestionOption;
import com.lms.education.module.lms.repository.QuestionOptionRepository;
import com.lms.education.module.lms.repository.QuestionRepository;
import com.lms.education.module.lms.service.QuestionOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionOptionServiceImpl implements QuestionOptionService {

    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionRepository questionRepository;

    public static void validateOptionsForQuestionType(String questionType, List<QuestionOptionDto> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        if ("MULTIPLE_CHOICE".equalsIgnoreCase(questionType) || "TRUE_FALSE".equalsIgnoreCase(questionType)) {
            boolean hasCorrect = options.stream()
                    .anyMatch(opt -> Boolean.TRUE.equals(opt.getIsCorrect()));
            if (!hasCorrect) {
                throw new OperationNotPermittedException(
                        "Câu hỏi loại " + questionType + " bắt buộc phải có ít nhất 1 đáp án đúng (isCorrect = true)!");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionOptionDto> getByQuestionId(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId);
        }
        return questionOptionRepository.findByQuestionIdOrderByIdAsc(questionId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionOptionDto create(Long questionId, QuestionOptionDto dto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId));

        if (dto.getOptionContent() == null || dto.getOptionContent().trim().isEmpty()) {
            throw new OperationNotPermittedException("Nội dung lựa chọn không được để trống!");
        }

        QuestionOption option = QuestionOption.builder()
                .question(question)
                .optionContent(dto.getOptionContent().trim())
                .isCorrect(Boolean.TRUE.equals(dto.getIsCorrect()))
                .build();

        QuestionOption saved = questionOptionRepository.save(option);
        log.info("Đã tạo mới lựa chọn ID: {} cho câu hỏi ID: {}", saved.getId(), questionId);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public QuestionOptionDto update(Long id, QuestionOptionDto dto) {
        QuestionOption existing = questionOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lựa chọn với ID: " + id));

        if (dto.getOptionContent() != null && !dto.getOptionContent().trim().isEmpty()) {
            existing.setOptionContent(dto.getOptionContent().trim());
        }

        if (dto.getIsCorrect() != null) {
            existing.setIsCorrect(dto.getIsCorrect());
        }

        QuestionOption updated = questionOptionRepository.save(existing);
        log.info("Đã cập nhật lựa chọn ID: {}", id);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        QuestionOption existing = questionOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lựa chọn với ID: " + id));

        questionOptionRepository.delete(existing);
        log.info("Đã xóa lựa chọn ID: {}", id);
    }

    @Override
    @Transactional
    public List<QuestionOptionDto> replaceAllForQuestion(Long questionId, List<QuestionOptionDto> dtos) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId));

        questionOptionRepository.deleteByQuestionId(questionId);
        questionOptionRepository.flush();

        if (dtos == null || dtos.isEmpty()) {
            log.info("Đã xóa toàn bộ lựa chọn của câu hỏi ID: {}", questionId);
            return new ArrayList<>();
        }

        validateOptionsForQuestionType(question.getQuestionType(), dtos);

        List<QuestionOption> toSave = new ArrayList<>();
        for (QuestionOptionDto optDto : dtos) {
            if (optDto.getOptionContent() == null || optDto.getOptionContent().trim().isEmpty()) {
                continue;
            }
            QuestionOption option = QuestionOption.builder()
                    .question(question)
                    .optionContent(optDto.getOptionContent().trim())
                    .isCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()))
                    .build();
            toSave.add(option);
        }

        List<QuestionOption> savedList = questionOptionRepository.saveAll(toSave);
        log.info("Đã cập nhật hàng loạt {} lựa chọn cho câu hỏi ID: {}", savedList.size(), questionId);

        return savedList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private QuestionOptionDto mapToDto(QuestionOption entity) {
        return QuestionOptionDto.builder()
                .id(entity.getId())
                .questionId(entity.getQuestion() != null ? entity.getQuestion().getId() : null)
                .optionContent(entity.getOptionContent())
                .isCorrect(entity.getIsCorrect())
                .build();
    }
}
