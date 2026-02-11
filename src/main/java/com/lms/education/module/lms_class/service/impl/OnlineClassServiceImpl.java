package com.lms.education.module.lms_class.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms_class.dto.OnlineClassDto;
import com.lms.education.module.lms_class.entity.OnlineClass;
import com.lms.education.module.lms_class.repository.OnlineClassRepository;
import com.lms.education.module.lms_class.service.OnlineClassService;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import com.lms.education.module.teaching_assignment.repository.TeachingSubstitutionRepository;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineClassServiceImpl implements OnlineClassService {

    private final OnlineClassRepository onlineClassRepository;
    private final TeachingSubstitutionRepository substitutionRepository;

    @Override
    public OnlineClassDto getById(String id) {
        OnlineClass entity = onlineClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học trực tuyến không tồn tại"));
        return mapToDto(entity);
    }

    @Override
    public List<OnlineClassDto> getMyClasses(String teacherId) {
        LocalDate today = LocalDate.now();
        List<OnlineClass> entities = onlineClassRepository.findAllClassesForTeacher(teacherId, today);

        return entities.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public PageResponse<OnlineClassDto> search(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<OnlineClass> pageResult = onlineClassRepository.search(keyword, status, pageable);

        List<OnlineClassDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<OnlineClassDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional
    public OnlineClassDto update(String id, OnlineClassDto dto) {
        OnlineClass existing = onlineClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học trực tuyến không tồn tại"));

        // Chỉ cho phép sửa Tên hiển thị và Trạng thái
        if (dto.getName() != null && !dto.getName().isBlank()) {
            existing.setName(dto.getName());
        }

        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        // Không cho sửa TeachingAssignment (Giáo viên/Môn) ở đây
        // Muốn sửa GV thì phải quay lại module TeachingAssignment để assign lại

        return mapToDto(onlineClassRepository.save(existing));
    }

    // --- MAPPER ---
    private OnlineClassDto mapToDto(OnlineClass entity) {
        TeachingAssignment assignment = entity.getTeachingAssignment();
        OnlineClassDto dto = OnlineClassDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())

                // Flatten data từ Assignment
                .teachingAssignmentId(assignment.getId())

                .subjectId(assignment.getSubject().getId())
                .subjectName(assignment.getSubject().getName())

                .physicalClassId(assignment.getPhysicalClass().getId())
                .physicalClassName(assignment.getPhysicalClass().getName())

                .teacherId(assignment.getTeacher().getId())
                .teacherName(assignment.getTeacher().getFullName())
                .teacherCode(assignment.getTeacher().getTeacherCode())

                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // [LOGIC ENRICH] Kiểm tra xem HÔM NAY có ai dạy thay không?
        Optional<TeachingSubstitution> activeSub = substitutionRepository.findActiveSubstitution(
                assignment.getId(),
                LocalDate.now()
        );

        if (activeSub.isPresent()) {
            TeachingSubstitution sub = activeSub.get();
            dto.setSubstituted(true);

            // Sub info
            dto.setSubTeacherId(sub.getSubTeacher().getId());
            dto.setSubTeacherName(sub.getSubTeacher().getFullName());
            dto.setSubEndDate(sub.getEndDate());
        }

        return dto;
    }
}
