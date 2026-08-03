package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
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


    private SubmissionAnswer upsertAnswerInternal(Submission submission, SubmissionAnswerDto dto) {
        Question question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + dto.getQuestionId()));

        Long assignmentId = submission.getAssignment().getId();
        AssignmentQuestion aq = assignmentQuestionRepository.findByAssignmentIdAndQuestionId(assignmentId, question.getId())
                .orElseThrow(() -> new OperationNotPermittedException("Câu hỏi ID " + question.getId() + " không thuộc đề thi của bài nộp này!"));

        QuestionOption selectedOption = null;
        BigDecimal earnedScore = BigDecimal.ZERO;
        boolean isAutoGraded = false;

        if (dto.getSelectedOptionId() != null) {
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
        return SubmissionAnswerDto.builder()
                .id(entity.getId())
                .submissionId(entity.getSubmission() != null ? entity.getSubmission().getId() : null)
                .questionId(entity.getQuestion() != null ? entity.getQuestion().getId() : null)
                .selectedOptionId(entity.getSelectedOption() != null ? entity.getSelectedOption().getId() : null)
                .textAnswer(entity.getTextAnswer())
                .earnedScore(entity.getEarnedScore())
                .isAutoGraded(entity.getIsAutoGraded())
                .questionContent(entity.getQuestion() != null ? entity.getQuestion().getContent() : null)
                .questionType(entity.getQuestion() != null ? entity.getQuestion().getQuestionType() : null)
                .selectedOptionContent(entity.getSelectedOption() != null ? entity.getSelectedOption().getOptionContent() : null)
                .isSelectedOptionCorrect(entity.getSelectedOption() != null ? entity.getSelectedOption().getIsCorrect() : null)
                .build();
    }
}
