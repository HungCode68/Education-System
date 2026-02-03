package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.SubjectDto;
import com.lms.education.module.academic.entity.Subject;
import com.lms.education.module.academic.repository.SubjectRepository;
import com.lms.education.module.academic.service.SubjectService;
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
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public SubjectDto create(SubjectDto dto) {
        log.info("Tạo môn học mới: {}", dto.getName());

        // Validate: Trùng tên
        if (subjectRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Môn học với tên '" + dto.getName() + "' đã tồn tại!");
        }

        // Map DTO -> Entity
        Subject entity = Subject.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        // Lưu & Return
        return mapToDto(subjectRepository.save(entity));
    }

    @Override
    @Transactional
    public SubjectDto update(String id, SubjectDto dto) {
        log.info("Cập nhật môn học ID: {}", id);

        // Tìm bản ghi cũ
        Subject existingSubject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));

        // Validate: Trùng tên (Nếu tên thay đổi và tên mới đã tồn tại)
        if (!existingSubject.getName().equals(dto.getName()) && subjectRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Tên môn học '" + dto.getName() + "' đã được sử dụng!");
        }

        // Cập nhật dữ liệu
        existingSubject.setName(dto.getName());
        existingSubject.setDescription(dto.getDescription());

        // Chỉ cập nhật trạng thái nếu người dùng có gửi lên
        if (dto.getIsActive() != null) {
            existingSubject.setIsActive(dto.getIsActive());
        }

        return mapToDto(subjectRepository.save(existingSubject));
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Xóa môn học ID: {}", id);

        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id);
        }

        // TODO: Sau này khi có bảng 'Course' (Lớp học phần) hoặc 'Grade' (Điểm),
        // cần kiểm tra xem môn học này đã được sử dụng chưa.
        // Ví dụ: if (courseRepository.existsBySubjectId(id)) { throw ... }

        subjectRepository.deleteById(id);
    }

    @Override
    public SubjectDto getById(String id) {
        Subject entity = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public PageResponse<SubjectDto> search(String keyword, Boolean isActive, int page, int size) {
        log.info("Search Subject: keyword={}, isActive={}, page={}, size={}", keyword, isActive, page, size);

        // Sắp xếp theo tên A-Z để dễ nhìn
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("name").ascending());

        Page<Subject> pageResult = subjectRepository.search(keyword, isActive, pageable);

        List<SubjectDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<SubjectDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<SubjectDto> getAllActive() {
        // Lấy danh sách dùng cho Dropdown
        return subjectRepository.findByIsActive(true).stream()
                .map(this::mapToDto)
                .toList();
    }

    // --- Helper Mapper ---
    private SubjectDto mapToDto(Subject entity) {
        return SubjectDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}