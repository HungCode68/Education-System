package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.dto.SubmissionAnswerDto;
import com.lms.education.module.lms.entity.*;
import com.lms.education.module.lms.repository.*;
import com.lms.education.module.lms.service.SubmissionAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionAnswerServiceImpl implements SubmissionAnswerService {

    private final SubmissionAnswerRepository submissionAnswerRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final AssignmentQuestionRepository assignmentQuestionRepository;
    private final com.lms.education.service.MinioStorageService minioStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionAnswerDto> getAnswersBySubmissionId(Long submissionId) {
        if (!submissionRepository.existsById(submissionId)) {
            throw new ResourceNotFoundException("Không tìm thấy bài nộp với ID: " + submissionId);
        }
        return submissionAnswerRepository.findBySubmissionId(submissionId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubmissionAnswerDto saveOrUpdateAnswer(Long submissionId, SubmissionAnswerDto dto) {
        Submission submission = checkSubmissionSafeguard(submissionId);
        SubmissionAnswer answer = upsertAnswerInternal(submission, dto);
        updateSubmissionTotalScore(submission);
        return toDto(answer);
    }

    @Override
    @Transactional
    public List<SubmissionAnswerDto> batchSaveAnswers(Long submissionId, List<SubmissionAnswerDto> dtos) {
        Submission submission = checkSubmissionSafeguard(submissionId);
        List<SubmissionAnswer> savedAnswers = new ArrayList<>();
        if (dtos != null) {
            for (SubmissionAnswerDto dto : dtos) {
                if (dto.getQuestionId() == null) {
                    continue;
                }
                savedAnswers.add(upsertAnswerInternal(submission, dto));
            }
        }
        updateSubmissionTotalScore(submission);
        return savedAnswers.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeAnswer(Long submissionId, Long questionId) {
        checkSubmissionSafeguard(submissionId);
        SubmissionAnswer answer = submissionAnswerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời cho câu hỏi ID: " + questionId + " trong bài nộp này"));
        submissionAnswerRepository.delete(answer);
    }

    @Override
    @Transactional
    public SubmissionAnswerDto gradeAnswer(Long answerId, BigDecimal score) {
        SubmissionAnswer answer = submissionAnswerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời với ID: " + answerId));

        if (score != null && score.compareTo(BigDecimal.ZERO) < 0) {
            throw new OperationNotPermittedException("Điểm số không được nhỏ hơn 0");
        }

        answer.setEarnedScore(score != null ? score : BigDecimal.ZERO);
        SubmissionAnswer saved = submissionAnswerRepository.save(answer);

        Submission submission = answer.getSubmission();
        if (submission != null) {
            updateSubmissionTotalScore(submission);
            if ("SUBMITTED".equalsIgnoreCase(submission.getStatus()) || "LATE".equalsIgnoreCase(submission.getStatus())) {
                submission.setStatus("GRADED");
                submissionRepository.save(submission);
            }
        }

        return toDto(saved);
    }

    @Override
    @Transactional
    public List<SubmissionAnswerDto> batchGradeAnswers(Long submissionId, List<com.lms.education.module.lms.dto.GradeAnswerDto> grades) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài với ID: " + submissionId));

        if (grades == null || grades.isEmpty()) {
            return new ArrayList<>();
        }

        List<SubmissionAnswer> updatedAnswers = new ArrayList<>();
        for (com.lms.education.module.lms.dto.GradeAnswerDto grade : grades) {
            if (grade.getScore() != null && grade.getScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new OperationNotPermittedException("Điểm số không được nhỏ hơn 0");
            }
            
            SubmissionAnswer answer = submissionAnswerRepository.findById(grade.getAnswerId())
                    .orElse(null);
                    
            if (answer != null && answer.getSubmission() != null && answer.getSubmission().getId().equals(submissionId)) {
                answer.setEarnedScore(grade.getScore() != null ? grade.getScore() : BigDecimal.ZERO);
                updatedAnswers.add(submissionAnswerRepository.save(answer));
            }
        }
        
        updateSubmissionTotalScore(submission);

        if ("SUBMITTED".equalsIgnoreCase(submission.getStatus()) || "LATE".equalsIgnoreCase(submission.getStatus())) {
            submission.setStatus("GRADED");
            submissionRepository.save(submission);
        }

        return updatedAnswers.stream().map(this::toDto).collect(Collectors.toList());
    }


    private SubmissionAnswer upsertAnswerInternal(Submission submission, SubmissionAnswerDto dto) {
        Question question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + dto.getQuestionId()));

        Long assignmentId = submission.getAssignment().getId();
        AssignmentQuestion aq = assignmentQuestionRepository.findByAssignmentIdAndQuestionId(assignmentId, question.getId())
                .orElseThrow(() -> new OperationNotPermittedException("Câu hỏi ID " + question.getId() + " không thuộc đề thi của bài nộp này!"));

        QuestionOption selectedOption = null;
        BigDecimal earnedScore = BigDecimal.ZERO;
        boolean isAutoGraded = false;

        
        if (dto.getSelectedOptionIds() != null && !dto.getSelectedOptionIds().trim().isEmpty()) {
            // Multiple Answers Logic
            isAutoGraded = true;
            try {
                // Parse the JSON array string like "[1, 2, 3]"
                String cleanStr = dto.getSelectedOptionIds().replaceAll("[^0-9,]", "");
                String[] idsStr = cleanStr.split(",");
                int correctCount = 0;
                int totalCorrectOptions = 0;
                int selectedCount = 0;

                // Count total correct options for this question
                for (QuestionOption opt : question.getOptions()) {
                    if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                        totalCorrectOptions++;
                    }
                }

                // Check student's selected options
                for (String idStr : idsStr) {
                    if (idStr.trim().isEmpty()) continue;
                    selectedCount++;
                    Long optId = Long.parseLong(idStr.trim());
                    for (QuestionOption opt : question.getOptions()) {
                        if (opt.getId().equals(optId) && Boolean.TRUE.equals(opt.getIsCorrect())) {
                            correctCount++;
                            break;
                        }
                    }
                }

                // Simple grading: partial score based on (correctCount / totalCorrectOptions)
                // Deduct for over-selecting? Let's just do: if selectedCount == totalCorrectOptions and correctCount == totalCorrectOptions, full score.
                // Otherwise, simple proportion. But if they select everything, they get penalty.
                if (totalCorrectOptions > 0) {
                    double penalty = Math.max(0, selectedCount - correctCount);
                    double scoreRatio = (double)(correctCount - penalty) / totalCorrectOptions;
                    if (scoreRatio < 0) scoreRatio = 0;
                    BigDecimal maxScore = aq.getScoreWeight() != null ? aq.getScoreWeight() : BigDecimal.ONE;
                    earnedScore = maxScore.multiply(BigDecimal.valueOf(scoreRatio));
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        } else if (dto.getSelectedOptionId() != null) {

            selectedOption = questionOptionRepository.findById(dto.getSelectedOptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lựa chọn đáp án với ID: " + dto.getSelectedOptionId()));

            if (!selectedOption.getQuestion().getId().equals(question.getId())) {
                throw new OperationNotPermittedException("Lựa chọn đáp án ID " + dto.getSelectedOptionId() + " không thuộc câu hỏi ID " + question.getId());
            }

            isAutoGraded = true;
            if (Boolean.TRUE.equals(selectedOption.getIsCorrect())) {
                earnedScore = aq.getScoreWeight() != null ? aq.getScoreWeight() : BigDecimal.ONE;
            } else {
                earnedScore = BigDecimal.ZERO;
            }
        } else if ("MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType()) || "TRUE_FALSE".equalsIgnoreCase(question.getQuestionType())) {
            // Unanswered multiple-choice / true-false
            isAutoGraded = true;
            earnedScore = BigDecimal.ZERO;
        } else {
            // Essay / Short Answer -> wait for teacher grading
            isAutoGraded = false;
            earnedScore = BigDecimal.ZERO;
        }

        SubmissionAnswer answer = submissionAnswerRepository.findBySubmissionIdAndQuestionId(submission.getId(), question.getId())
                .orElse(SubmissionAnswer.builder()
                        .submission(submission)
                        .question(question)
                        .build());

        
        answer.setSelectedOption(selectedOption);
        answer.setSelectedOptionIds(dto.getSelectedOptionIds());

        answer.setTextAnswer(dto.getTextAnswer());
        answer.setEarnedScore(earnedScore);
        answer.setIsAutoGraded(isAutoGraded);

        return submissionAnswerRepository.save(answer);
    }

    private Submission checkSubmissionSafeguard(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài nộp với ID: " + submissionId));

        if (!"IN_PROGRESS".equalsIgnoreCase(submission.getStatus())) {
            throw new OperationNotPermittedException("Không thể chỉnh sửa câu trả lời vì bài nộp đã ở trạng thái: " + submission.getStatus());
        }
        return submission;
    }

    private void updateSubmissionTotalScore(Submission submission) {
        List<SubmissionAnswer> allAnswers = submissionAnswerRepository.findBySubmissionId(submission.getId());
        BigDecimal totalScore = allAnswers.stream()
                .map(a -> a.getEarnedScore() != null ? a.getEarnedScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        submission.setScore(totalScore);
        submissionRepository.save(submission);
    }

    private SubmissionAnswerDto toDto(SubmissionAnswer entity) {
        List<Long> correctOptionIds = null;
        
        Question question = entity.getQuestion();
        Submission submission = entity.getSubmission();
        boolean isGraded = submission != null && "GRADED".equalsIgnoreCase(submission.getStatus());
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isTeacher = auth != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(role -> role.equals("ROLE_TEACHER") || role.equals("ROLE_SYSTEM_ADMIN") || role.equals("ROLE_ADMIN") || role.equals("ROLE_STAFF") || role.equals("LMS_ASSIGNMENT_MANAGE"));
        boolean canViewAnswers = isGraded || isTeacher;
        
        boolean showCorrectAnswers = submission != null && submission.getAssignment() != null && Boolean.TRUE.equals(submission.getAssignment().getShowCorrectAnswers());
        boolean canViewCorrectAnswers = isTeacher || (isGraded && showCorrectAnswers);
        
        if (question != null && canViewCorrectAnswers) {
            correctOptionIds = new ArrayList<>();
            for (QuestionOption opt : question.getOptions()) {
                if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                    correctOptionIds.add(opt.getId());
                }
            }
        }

        List<QuestionOptionDto> options = null;
        if (question != null && question.getOptions() != null) {
            options = question.getOptions().stream().map(o -> QuestionOptionDto.builder()
                    .id(o.getId())
                    .optionContent(o.getOptionContent())
                    .isCorrect(canViewCorrectAnswers ? o.getIsCorrect() : null)
                    .build()).collect(Collectors.toList());
        }

        String attachmentUrl = null;
        if (question != null && question.getMediaUrl() != null && !question.getMediaUrl().isEmpty()) {
            try {
                attachmentUrl = minioStorageService.getFileUrl(question.getMediaUrl());
            } catch (Exception e) {
                log.error("Error getting Minio URL for {}", question.getMediaUrl(), e);
            }
        }

        BigDecimal maxScore = BigDecimal.ONE;
        if (question != null && submission != null && submission.getAssignment() != null) {
            maxScore = assignmentQuestionRepository.findByAssignmentIdAndQuestionId(submission.getAssignment().getId(), question.getId())
                    .map(AssignmentQuestion::getScoreWeight)
                    .orElse(BigDecimal.ONE);
        }

        return SubmissionAnswerDto.builder()
                .id(entity.getId())
                .submissionId(submission != null ? submission.getId() : null)
                .questionId(question != null ? question.getId() : null)
                .selectedOptionId(entity.getSelectedOption() != null ? entity.getSelectedOption().getId() : null)
                .selectedOptionIds(entity.getSelectedOptionIds())
                .textAnswer(entity.getTextAnswer())
                .earnedScore(canViewAnswers ? entity.getEarnedScore() : null)
                .maxScore(maxScore)
                .isAutoGraded(canViewAnswers ? entity.getIsAutoGraded() : null)
                .correctOptionIds(correctOptionIds)
                .questionContent(question != null ? question.getContent() : null)
                .questionType(question != null ? question.getQuestionType() : null)
                .questionAttachmentUrl(attachmentUrl)
                .options(options)
                .selectedOptionContent(entity.getSelectedOption() != null ? entity.getSelectedOption().getOptionContent() : null)
                .isSelectedOptionCorrect(canViewCorrectAnswers ? (entity.getSelectedOption() != null ? entity.getSelectedOption().getIsCorrect() : null) : null)
                .build();
    }
}
