package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.AssignmentQuestionDto;
import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.entity.Assignment;
import com.lms.education.module.lms.entity.AssignmentQuestion;
import com.lms.education.module.lms.entity.AssignmentQuestionId;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.repository.*;
import com.lms.education.module.lms.service.AssignmentQuestionService;
import com.lms.education.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor
@Slf4j
public class AssignmentQuestionServiceImpl implements AssignmentQuestionService {

    private final AssignmentQuestionRepository assignmentQuestionRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SubmissionRepository submissionRepository;
    private final MinioStorageService minioStorageService;

    private void checkSubmissionSafeguard(Long assignmentId) {
        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw new OperationNotPermittedException(
                    "Bài tập này đã có học viên mở bài làm hoặc nộp bài, không thể thay đổi danh sách câu hỏi!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentQuestionDto> getByAssignmentId(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId);
        }

        return assignmentQuestionRepository.findByAssignmentIdOrderByOrderNumberAsc(assignmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AssignmentQuestionDto addQuestionToAssignment(Long assignmentId, AssignmentQuestionDto dto) {
        checkSubmissionSafeguard(assignmentId);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));

        if (dto.getQuestionId() == null) {
            throw new OperationNotPermittedException("ID câu hỏi (questionId) không được để trống!");
        }

        Question question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + dto.getQuestionId()));

        if (assignmentQuestionRepository.existsByAssignmentIdAndQuestionId(assignmentId, dto.getQuestionId())) {
            throw new OperationNotPermittedException("Câu hỏi này đã có trong bài tập!");
        }

        int nextOrder = dto.getOrderNumber() != null ? dto.getOrderNumber() :
                (assignmentQuestionRepository.findByAssignmentIdOrderByOrderNumberAsc(assignmentId).size() + 1);

        if (dto.getScoreWeight() != null && dto.getScoreWeight().compareTo(BigDecimal.ZERO) < 0) {
            throw new OperationNotPermittedException("Điểm số câu hỏi không được nhỏ hơn 0!");
        }
        BigDecimal weight = dto.getScoreWeight() != null ? dto.getScoreWeight() : BigDecimal.ONE;


        AssignmentQuestionId id = AssignmentQuestionId.builder()
                .assignmentId(assignmentId)
                .questionId(dto.getQuestionId())
                .build();

        AssignmentQuestion entity = AssignmentQuestion.builder()
                .id(id)
                .assignment(assignment)
                .question(question)
                .orderNumber(nextOrder)
                .scoreWeight(weight)
                .build();

        AssignmentQuestion saved = assignmentQuestionRepository.save(entity);
        log.info("Đã thêm câu hỏi ID: {} vào bài tập ID: {} (Thứ tự: {}, Điểm: {})",
                dto.getQuestionId(), assignmentId, nextOrder, weight);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public AssignmentQuestionDto updateQuestionInAssignment(Long assignmentId, Long questionId, Integer orderNumber, BigDecimal scoreWeight) {
        checkSubmissionSafeguard(assignmentId);

        AssignmentQuestion existing = assignmentQuestionRepository.findByAssignmentIdAndQuestionId(assignmentId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi ID: " + questionId + " trong bài tập ID: " + assignmentId));

        if (orderNumber != null && orderNumber > 0) {
            existing.setOrderNumber(orderNumber);
        }

        if (scoreWeight != null) {
            if (scoreWeight.compareTo(BigDecimal.ZERO) < 0) {
                throw new OperationNotPermittedException("Điểm số câu hỏi không được nhỏ hơn 0!");
            }
            existing.setScoreWeight(scoreWeight);
        }


        AssignmentQuestion updated = assignmentQuestionRepository.save(existing);
        log.info("Đã cập nhật câu hỏi ID: {} trong bài tập ID: {}", questionId, assignmentId);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void removeQuestionFromAssignment(Long assignmentId, Long questionId) {
        checkSubmissionSafeguard(assignmentId);

        AssignmentQuestion existing = assignmentQuestionRepository.findByAssignmentIdAndQuestionId(assignmentId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi ID: " + questionId + " trong bài tập ID: " + assignmentId));

        assignmentQuestionRepository.delete(existing);
        log.info("Đã xóa câu hỏi ID: {} khỏi bài tập ID: {}", questionId, assignmentId);
    }

    @Override
    @Transactional
    public List<AssignmentQuestionDto> batchReplaceAssignmentQuestions(Long assignmentId, List<AssignmentQuestionDto> dtos) {
        checkSubmissionSafeguard(assignmentId);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài tập với ID: " + assignmentId));

        assignmentQuestionRepository.deleteByAssignmentId(assignmentId);
        assignmentQuestionRepository.flush();

        if (dtos == null || dtos.isEmpty()) {
            log.info("Đã xóa toàn bộ câu hỏi khỏi bài tập ID: {}", assignmentId);
            return new ArrayList<>();
        }

        Set<Long> seenQuestionIds = new HashSet<>();
        List<AssignmentQuestion> toSave = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            AssignmentQuestionDto item = dtos.get(i);
            if (item.getQuestionId() == null) {
                continue;
            }
            if (!seenQuestionIds.add(item.getQuestionId())) {
                throw new OperationNotPermittedException("Danh sách câu hỏi cập nhật bị trùng lặp câu hỏi ID: " + item.getQuestionId());
            }
            if (item.getScoreWeight() != null && item.getScoreWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw new OperationNotPermittedException("Điểm số câu hỏi không được nhỏ hơn 0!");
            }
            Question question = questionRepository.findById(item.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + item.getQuestionId()));


            int order = item.getOrderNumber() != null ? item.getOrderNumber() : (i + 1);
            BigDecimal weight = item.getScoreWeight() != null ? item.getScoreWeight() : BigDecimal.ONE;

            AssignmentQuestionId id = AssignmentQuestionId.builder()
                    .assignmentId(assignmentId)
                    .questionId(question.getId())
                    .build();

            AssignmentQuestion aq = AssignmentQuestion.builder()
                    .id(id)
                    .assignment(assignment)
                    .question(question)
                    .orderNumber(order)
                    .scoreWeight(weight)
                    .build();

            toSave.add(aq);
        }

        List<AssignmentQuestion> savedList = assignmentQuestionRepository.saveAll(toSave);
        log.info("Đã cập nhật hàng loạt {} câu hỏi cho bài tập ID: {}", savedList.size(), assignmentId);

        return savedList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AssignmentQuestionDto mapToDto(AssignmentQuestion entity) {
        return AssignmentQuestionDto.builder()
                .assignmentId(entity.getAssignment() != null ? entity.getAssignment().getId() : null)
                .questionId(entity.getQuestion() != null ? entity.getQuestion().getId() : null)
                .orderNumber(entity.getOrderNumber())
                .scoreWeight(entity.getScoreWeight())
                .question(entity.getQuestion() != null ? mapToQuestionDto(entity.getQuestion()) : null)
                .build();
    }

    private QuestionDto mapToQuestionDto(Question entity) {
        String downloadMediaUrl = null;
        if (entity.getMediaUrl() != null && !entity.getMediaUrl().trim().isEmpty()) {
            downloadMediaUrl = minioStorageService.getFileUrl(entity.getMediaUrl());
        }

        List<QuestionOptionDto> optionDtos = null;
        if (entity.getId() != null) {
            optionDtos = questionOptionRepository.findByQuestionIdOrderByIdAsc(entity.getId())
                    .stream()
                    .map(opt -> QuestionOptionDto.builder()
                            .id(opt.getId())
                            .questionId(entity.getId())
                            .optionContent(opt.getOptionContent())
                            .isCorrect(opt.getIsCorrect())
                            .build())
                    .collect(Collectors.toList());
        }

        return QuestionDto.builder()
                .id(entity.getId())
                .questionType(entity.getQuestionType())
                .content(entity.getContent())
                .mediaUrl(entity.getMediaUrl())
                .downloadMediaUrl(downloadMediaUrl)
                .readingPassage(entity.getReadingPassage())
                .createdAt(entity.getCreatedAt())
                .options(optionDtos)
                .build();
    }
}
