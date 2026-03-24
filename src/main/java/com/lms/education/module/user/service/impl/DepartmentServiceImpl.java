package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.DepartmentDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.module.user.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional
    public DepartmentDto create(DepartmentDto dto) {
        // Kiểm tra trùng tên phòng ban
        if (departmentRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new DuplicateResourceException("Tên phòng ban/tổ bộ môn '" + dto.getName() + "' đã tồn tại!");
        }

        Department department = Department.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .type(dto.getType() != null ? dto.getType() : Department.DepartmentType.academic)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Department savedDepartment = departmentRepository.save(department);
        log.info("Đã tạo mới Phòng ban/Tổ bộ môn: {}", savedDepartment.getName());

        return mapToDto(savedDepartment);
    }

    @Override
    @Transactional
    public DepartmentDto update(String id, DepartmentDto dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng ban với ID: " + id));

        String newName = dto.getName().trim();

        // Nếu đổi tên, phải kiểm tra xem tên mới có bị trùng với phòng ban khác không
        if (!department.getName().equalsIgnoreCase(newName) && departmentRepository.existsByNameIgnoreCase(newName)) {
            throw new DuplicateResourceException("Tên phòng ban/tổ bộ môn '" + newName + "' đã tồn tại!");
        }

        department.setName(newName);
        department.setDescription(dto.getDescription());

        if (dto.getType() != null) {
            department.setType(dto.getType());
        }
        if (dto.getIsActive() != null) {
            department.setIsActive(dto.getIsActive());
        }

        Department updatedDepartment = departmentRepository.save(department);
        log.info("Đã cập nhật Phòng ban ID: {}", id);

        return mapToDto(updatedDepartment);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng ban/Tổ bộ môn với ID: " + id));

        // Kiểm tra xem có giáo viên nào đang trực thuộc phòng ban này không
        boolean hasTeachers = teacherRepository.existsByDepartmentId(id);

        if (hasTeachers) {
            // NẾU CÓ GIÁO VIÊN -> XÓA MỀM (Chỉ ẩn đi)
            department.setIsActive(false);
            departmentRepository.save(department);
            log.info("Phòng ban ID: {} đang có giáo viên trực thuộc. Đã thực hiện XÓA MỀM (isActive = false)", id);
        } else {
            // NẾU KHÔNG CÓ GIÁO VIÊN -> XÓA CỨNG (Xóa sạch khỏi Database)
            departmentRepository.delete(department);
            log.info("Phòng ban ID: {} chưa có giáo viên nào. Đã thực hiện XÓA CỨNG thành công", id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getById(String id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng ban với ID: " + id));
        return mapToDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentDto> getAll(String keyword, Department.DepartmentType type, Boolean isActive, Pageable pageable) {
        return departmentRepository.searchAndFilter(keyword, type, isActive, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllActive() {
        return departmentRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getActiveByType(Department.DepartmentType type) {
        return departmentRepository.findByTypeAndIsActiveTrueOrderByNameAsc(type).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- Hàm Helper ---
    private DepartmentDto mapToDto(Department department) {
        return DepartmentDto.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .type(department.getType())
                .isActive(department.getIsActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
