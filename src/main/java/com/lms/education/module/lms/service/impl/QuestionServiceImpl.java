package com.lms.education.module.lms.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.entity.Question;
import com.lms.education.module.lms.entity.QuestionOption;
import com.lms.education.module.lms.repository.QuestionOptionRepository;
import com.lms.education.module.lms.repository.QuestionRepository;
import com.lms.education.module.lms.service.QuestionService;
import com.lms.education.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

import com.lms.education.module.lms.repository.AssignmentQuestionRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final MinioStorageService minioStorageService;
    private final AssignmentQuestionRepository assignmentQuestionRepository;
    private final SubmissionRepository submissionRepository;


    private static final java.util.Set<String> VALID_QUESTION_TYPES = java.util.Set.of(
            "MULTIPLE_CHOICE", "ESSAY", "LISTENING", "READING", "FILL_BLANK", "TRUE_FALSE"
    );

    @Override
    @Transactional
    public QuestionDto create(QuestionDto dto, MultipartFile mediaFile) {
        String formattedType = dto.getQuestionType() != null ? dto.getQuestionType().trim().toUpperCase() : "";
        if (!VALID_QUESTION_TYPES.contains(formattedType)) {
            throw new OperationNotPermittedException("Loại câu hỏi '" + dto.getQuestionType() + "' không hợp lệ! Loại câu hỏi phải là một trong: " + VALID_QUESTION_TYPES);
        }

        // Kiểm tra ràng buộc theo loại câu hỏi
        if ("READING".equalsIgnoreCase(formattedType) && (dto.getReadingPassage() == null || dto.getReadingPassage().trim().isEmpty())) {
            throw new OperationNotPermittedException("Loại câu hỏi READING (Bài đọc) bắt buộc phải có nội dung đoạn văn (readingPassage)!");
        }

        if ("LISTENING".equalsIgnoreCase(formattedType)
                && (mediaFile == null || mediaFile.isEmpty())
                && (dto.getMediaUrl() == null || dto.getMediaUrl().trim().isEmpty())) {
            throw new OperationNotPermittedException("Loại câu hỏi LISTENING (Bài nghe) bắt buộc phải đính kèm tệp âm thanh/hình ảnh!");
        }

        String objectName = null;
        if (mediaFile != null && !mediaFile.isEmpty()) {
            objectName = minioStorageService.uploadFile(mediaFile);
        }

        Question question = Question.builder()
                .questionType(formattedType)
                .content(dto.getContent().trim())
                .mediaUrl(objectName != null ? objectName : (dto.getMediaUrl() != null ? dto.getMediaUrl().trim() : null))
                .readingPassage(dto.getReadingPassage() != null ? dto.getReadingPassage().trim() : null)
                .build();

        Question saved = questionRepository.save(question);
        log.info("Đã tạo mới câu hỏi ngân hàng ID: {} (Loại: {})", saved.getId(), saved.getQuestionType());

        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            QuestionOptionServiceImpl.validateOptionsForQuestionType(formattedType, dto.getOptions());
            for (QuestionOptionDto optDto : dto.getOptions()) {
                if (optDto.getOptionContent() == null || optDto.getOptionContent().trim().isEmpty()) {
                    continue;
                }
                QuestionOption option = QuestionOption.builder()
                        .question(saved)
                        .optionContent(optDto.getOptionContent().trim())
                        .isCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()))
                        .build();
                questionOptionRepository.save(option);
            }
        }

        return mapToDto(saved);
    }


    private void checkSubmissionSafeguard(Long questionId) {
        boolean hasSubmissions = assignmentQuestionRepository.findByQuestionId(questionId).stream()
                .anyMatch(aq -> submissionRepository.existsByAssignmentId(aq.getAssignment().getId()));
        if (hasSubmissions) {
            throw new OperationNotPermittedException("Câu hỏi này đang được sử dụng trong bài tập đã có học viên làm bài, không thể thay đổi nội dung hoặc xóa!");
        }
    }

    @Override
    @Transactional
    public QuestionDto update(Long id, QuestionDto dto, MultipartFile mediaFile) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + id));

        checkSubmissionSafeguard(id);

        String targetType = existing.getQuestionType();
        if (dto.getQuestionType() != null && !dto.getQuestionType().trim().isEmpty()) {
            String formattedType = dto.getQuestionType().trim().toUpperCase();
            if (!VALID_QUESTION_TYPES.contains(formattedType)) {
                throw new OperationNotPermittedException("Loại câu hỏi '" + dto.getQuestionType() + "' không hợp lệ! Loại câu hỏi phải là một trong: " + VALID_QUESTION_TYPES);
            }
            targetType = formattedType;
            existing.setQuestionType(formattedType);
        }

        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            existing.setContent(dto.getContent().trim());
        }

        if (dto.getReadingPassage() != null) {
            existing.setReadingPassage(dto.getReadingPassage().trim());
        }

        // Kiểm tra ràng buộc loại câu hỏi READING sau khi cập nhật
        if ("READING".equalsIgnoreCase(targetType) && (existing.getReadingPassage() == null || existing.getReadingPassage().trim().isEmpty())) {
            throw new OperationNotPermittedException("Loại câu hỏi READING (Bài đọc) bắt buộc phải có nội dung đoạn văn (readingPassage)!");
        }

        if (mediaFile != null && !mediaFile.isEmpty()) {
            if (existing.getMediaUrl() != null && !existing.getMediaUrl().isEmpty()) {
                minioStorageService.deleteFile(existing.getMediaUrl());
            }
            String newObjectName = minioStorageService.uploadFile(mediaFile);
            existing.setMediaUrl(newObjectName);
        } else if (dto.getMediaUrl() != null) {
            String trimmedMedia = dto.getMediaUrl().trim();
            if (trimmedMedia.isEmpty() || "null".equalsIgnoreCase(trimmedMedia) || "CLEAR".equalsIgnoreCase(trimmedMedia)) {
                // Nếu người dùng muốn gỡ bỏ file đính kèm cũ -> xóa file trên MinIO và set null
                if (existing.getMediaUrl() != null && !existing.getMediaUrl().isEmpty()) {
                    minioStorageService.deleteFile(existing.getMediaUrl());
                }
                existing.setMediaUrl(null);
            } else {
                existing.setMediaUrl(trimmedMedia);
            }
        }

        // Kiểm tra ràng buộc loại câu hỏi LISTENING sau khi cập nhật media
        if ("LISTENING".equalsIgnoreCase(targetType) && (existing.getMediaUrl() == null || existing.getMediaUrl().trim().isEmpty())) {
            throw new OperationNotPermittedException("Loại câu hỏi LISTENING (Bài nghe) bắt buộc phải có tệp âm thanh/hình ảnh đính kèm!");
        }

        Question updated = questionRepository.save(existing);
        log.info("Đã cập nhật câu hỏi ID: {}", id);

        if (dto.getOptions() != null) {
            questionOptionRepository.deleteByQuestionId(id);
            questionOptionRepository.flush();

            if (!dto.getOptions().isEmpty()) {
                QuestionOptionServiceImpl.validateOptionsForQuestionType(targetType, dto.getOptions());
                for (QuestionOptionDto optDto : dto.getOptions()) {
                    if (optDto.getOptionContent() == null || optDto.getOptionContent().trim().isEmpty()) {
                        continue;
                    }
                    QuestionOption option = QuestionOption.builder()
                            .question(updated)
                            .optionContent(optDto.getOptionContent().trim())
                            .isCorrect(Boolean.TRUE.equals(optDto.getIsCorrect()))
                            .build();
                    questionOptionRepository.save(option);
                }
            }
        }

        return mapToDto(updated);
    }


    @Override
    @Transactional
    public void delete(Long id) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + id));

        checkSubmissionSafeguard(id);

        if (existing.getMediaUrl() != null && !existing.getMediaUrl().isEmpty()) {
            minioStorageService.deleteFile(existing.getMediaUrl());
        }

        questionRepository.delete(existing);
        log.info("Đã xóa câu hỏi ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDto getById(Long id) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDto> getByAssignmentId(Long assignmentId) {
        return questionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionDto> getAll(String keyword, String questionType, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanType = (questionType != null && !questionType.trim().isEmpty()) ? questionType.trim() : null;

        Page<Question> page;
        if (cleanKeyword != null && cleanType != null) {
            page = questionRepository.findByKeywordAndType(cleanKeyword, cleanType, pageable);
        } else if (cleanKeyword != null) {
            page = questionRepository.findByKeyword(cleanKeyword, pageable);
        } else if (cleanType != null) {
            page = questionRepository.findByQuestionTypeIgnoreCase(cleanType, pageable);
        } else {
            page = questionRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    private QuestionDto mapToDto(Question entity) {
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

