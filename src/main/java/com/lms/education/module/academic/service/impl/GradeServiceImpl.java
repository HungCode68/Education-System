package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.GradeDto;
import com.lms.education.module.academic.entity.Grade;
import com.lms.education.module.academic.repository.GradeRepository;
import com.lms.education.module.academic.service.GradeService;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;

    @Override
    @Transactional
    public GradeDto create(GradeDto dto) {
        log.info("Tạo khối mới: {} (Level {})", dto.getName(), dto.getLevel());

        // Validate trùng Tên
        if (gradeRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Tên khối '" + dto.getName() + "' đã tồn tại!");
        }

        // Validate trùng Level
        if (gradeRepository.existsByLevel(dto.getLevel())) {
            throw new DuplicateResourceException("Level " + dto.getLevel() + " đã được gán cho một khối khác!");
        }

        Grade entity = Grade.builder()
                .name(dto.getName())
                .level(dto.getLevel())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return mapToDto(gradeRepository.save(entity));
    }

    @Override
    @Transactional
    public GradeDto update(String id, GradeDto dto) {
        log.info("Cập nhật khối ID: {}", id);

        Grade existingGrade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối với ID: " + id));

        // CHECK TRÙNG TÊN (Logic bỏ qua chính mình)
        // Nếu người dùng đổi tên, kiểm tra xem tên mới có thuộc về ai khác không
        if (!existingGrade.getName().equals(dto.getName())) {
            Optional<Grade> duplicateName = gradeRepository.findByName(dto.getName());
            if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
                throw new DuplicateResourceException("Tên khối '" + dto.getName() + "' đã được sử dụng!");
            }
        }

        // CHECK TRÙNG LEVEL (Logic bỏ qua chính mình)
        // Nếu người dùng đổi Level, kiểm tra xem Level mới có thuộc về ai khác không
        if (!existingGrade.getLevel().equals(dto.getLevel())) {
            Optional<Grade> duplicateLevel = gradeRepository.findByLevel(dto.getLevel());
            if (duplicateLevel.isPresent() && !duplicateLevel.get().getId().equals(id)) {
                throw new DuplicateResourceException("Level " + dto.getLevel() + " đã được sử dụng!");
            }
        }

        // Cập nhật
        existingGrade.setName(dto.getName());
        existingGrade.setLevel(dto.getLevel());

        if (dto.getIsActive() != null) {
            existingGrade.setIsActive(dto.getIsActive());
        }

        return mapToDto(gradeRepository.save(existingGrade));
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Xóa khối ID: {}", id);
        if (!gradeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy khối với ID: " + id);
        }

        // TODO: Cần kiểm tra ràng buộc. Nếu khối này đã có Lớp học (PhysicalClass) thì không được xóa.
        // if (classRepository.existsByGradeId(id)) throw ...

        gradeRepository.deleteById(id);
    }

    @Override
    public GradeDto getById(String id) {
        Grade entity = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối với ID: " + id));
        return mapToDto(entity);
    }

    @Override
    public PageResponse<GradeDto> search(String keyword, Boolean isActive, int page, int size) {
        // Sắp xếp mặc định theo Level tăng dần
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("level").ascending());

        Page<Grade> pageResult = gradeRepository.search(keyword, isActive, pageable);

        List<GradeDto> dtos = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.<GradeDto>builder()
                .content(dtos)
                .pageNo(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<GradeDto> getAllActive() {
        // Lấy danh sách cho Dropdown, sắp xếp Level bé đến lớn
        return gradeRepository.findByIsActive(true, Sort.by("level").ascending())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private GradeDto mapToDto(Grade entity) {
        return GradeDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .level(entity.getLevel())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
