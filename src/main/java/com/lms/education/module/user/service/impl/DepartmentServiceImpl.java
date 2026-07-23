package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.DepartmentDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.service.DepartmentService;
import com.lms.education.module.user.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public DepartmentDto create(DepartmentDto dto) {
        // Chuẩn hóa mã phòng ban: Cắt khoảng trắng và viết hoa toàn bộ
        String formattedCode = dto.getCode().trim().toUpperCase();

        if (departmentRepository.existsByCode(formattedCode)) {
            throw new DuplicateResourceException("Mã phòng ban/khoa '" + formattedCode + "' đã tồn tại!");
        }

        Department department = Department.builder()
                .code(formattedCode)
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .build();

        Department savedDepartment = departmentRepository.save(department);
        log.info("Đã tạo mới phòng ban/khoa: {} - {}", savedDepartment.getCode(), savedDepartment.getName());

        return mapToDto(savedDepartment);
    }

    @Override
    @Transactional
    public DepartmentDto update(Long id, DepartmentDto dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban/khoa với ID: " + id));

        String newFormattedCode = dto.getCode().trim().toUpperCase();

        // Kiểm tra xem người dùng có đổi mã code không, và mã mới có bị trùng với phòng ban khác không
        if (!department.getCode().equals(newFormattedCode) && departmentRepository.existsByCode(newFormattedCode)) {
            throw new DuplicateResourceException("Mã phòng ban/khoa '" + newFormattedCode + "' đã được sử dụng!");
        }

        department.setCode(newFormattedCode);
        department.setName(dto.getName().trim());
        department.setDescription(dto.getDescription());

        Department updatedDepartment = departmentRepository.save(department);
        log.info("Đã cập nhật phòng ban/khoa ID: {}", id);

        return mapToDto(updatedDepartment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban/khoa với ID: " + id));

        // Kiểm tra xem phòng ban này có đang chứa nhân sự nào không
        if (staffRepository.existsByDepartmentId(id)) {
            log.warn("Cố gắng xóa phòng ban ID: {} nhưng thất bại do vẫn còn nhân sự trực thuộc", id);
            // Ném ra Exception để GlobalExceptionHandler bắt và trả về mã lỗi
            throw new IllegalStateException("Không thể xóa phòng ban này vì vẫn đang có nhân sự trực thuộc. Vui lòng chuyển đổi công tác cho nhân sự trước!");
        }

        departmentRepository.delete(department);
        log.info("Đã xóa hoàn toàn phòng ban/khoa ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban/khoa với ID: " + id));
        return mapToDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getByCode(String code) {
        String formattedCode = code.trim().toUpperCase();
        Department department = departmentRepository.findByCode(formattedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban/khoa với mã: " + formattedCode));
        return mapToDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentDto> getAllDepartments(String keyword, Pageable pageable) {
        Page<Department> departments;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Tìm kiếm theo cả Tên và Mã code
            departments = departmentRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable);
        } else {
            departments = departmentRepository.findAll(pageable);
        }

        return departments.map(this::mapToDto);
    }

    // --- Hàm Helper chuyển đổi từ Entity sang DTO ---
    private DepartmentDto mapToDto(Department department) {
        return DepartmentDto.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .description(department.getDescription())
                .createdAt(department.getCreatedAt())
                .build();
    }
}