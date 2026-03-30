package com.lms.education.module.teaching_assignment.service.impl;

import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentHistoryDto;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignmentHistory;
import com.lms.education.module.teaching_assignment.repository.TeachingAssignmentHistoryRepository;
import com.lms.education.module.teaching_assignment.service.TeachingAssignmentHistoryService;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeachingAssignmentHistoryServiceImpl implements TeachingAssignmentHistoryService {

    private final TeachingAssignmentHistoryRepository historyRepository;

    @Override
    @Transactional
    public void log(TeachingAssignment assignment, Teacher oldTeacher, Teacher newTeacher,
                    TeachingAssignmentHistory.ActionType actionType, String reason, String changedBy) {

        log.info("Ghi log phân công: {} - {} -> {}", actionType,
                (oldTeacher != null ? oldTeacher.getFullName() : "null"),
                (newTeacher != null ? newTeacher.getFullName() : "null"));

        TeachingAssignmentHistory history = TeachingAssignmentHistory.builder()
                .assignment(assignment)
                .oldTeacher(oldTeacher)
                .newTeacher(newTeacher)
                .actionType(actionType)
                .reason(reason)
                .changedBy(changedBy != null ? changedBy : "SYSTEM") // Mặc định SYSTEM nếu không truyền user
                .build();

        historyRepository.save(history);
    }

    @Override
    public List<TeachingAssignmentHistoryDto> getByAssignment(String assignmentId) {
        return historyRepository.findAllByAssignmentIdOrderByChangedAtDesc(assignmentId)
                .stream().map(this::mapToDto).toList();
    }

    @Override
    public List<TeachingAssignmentHistoryDto> getByClass(String classId) {
        return historyRepository.findAllByClassId(classId)
                .stream().map(this::mapToDto).toList();
    }

    @Override
    public List<TeachingAssignmentHistoryDto> getByTeacher(String teacherId) {
        return historyRepository.findAllByTeacherId(teacherId)
                .stream().map(this::mapToDto).toList();
    }

    @Override
    public PageResponse<TeachingAssignmentHistoryDto> search(String keyword, String actionTypeStr, int page, int size) {
        // Nên sắp xếp log mới nhất lên đầu
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("changedAt").descending());

        TeachingAssignmentHistory.ActionType actionType = null;
        if (actionTypeStr != null && !actionTypeStr.trim().isEmpty()) {
            try {
                actionType = TeachingAssignmentHistory.ActionType.valueOf(actionTypeStr);
            } catch (IllegalArgumentException e) {
                log.warn("Action Type không hợp lệ: {}", actionTypeStr);
            }
        }

        Page<TeachingAssignmentHistory> pageResult = historyRepository.searchWithFilter(keyword, actionType, pageable);

        List<TeachingAssignmentHistoryDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<TeachingAssignmentHistoryDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    // --- MAPPER HELPER ---
    // Chuyển Entity sang DTO và xử lý null cho Teacher
    private TeachingAssignmentHistoryDto mapToDto(TeachingAssignmentHistory entity) {
        TeachingAssignmentHistoryDto.TeachingAssignmentHistoryDtoBuilder builder = TeachingAssignmentHistoryDto.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment().getId())
                .physicalClassName(entity.getAssignment().getPhysicalClass().getName())
                .subjectName(entity.getAssignment().getSubject().getName())
                .actionType(entity.getActionType())
                .reason(entity.getReason())
                .changedBy(entity.getChangedBy())
                .changedAt(entity.getChangedAt());

        // Map Old Teacher (Nếu có)
        if (entity.getOldTeacher() != null) {
            builder.oldTeacherId(entity.getOldTeacher().getId())
                    .oldTeacherName(entity.getOldTeacher().getFullName())
                    .oldTeacherCode(entity.getOldTeacher().getTeacherCode());
        }

        // Map New Teacher (Nếu có)
        if (entity.getNewTeacher() != null) {
            builder.newTeacherId(entity.getNewTeacher().getId())
                    .newTeacherName(entity.getNewTeacher().getFullName())
                    .newTeacherCode(entity.getNewTeacher().getTeacherCode());
        }

        return builder.build();
    }
}
