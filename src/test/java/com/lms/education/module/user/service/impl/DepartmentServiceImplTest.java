package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.DepartmentDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department mockDepartment;
    private DepartmentDto mockDepartmentDto;

    @BeforeEach
    void setUp() {
        mockDepartment = new Department();
        mockDepartment.setId(1L);
        mockDepartment.setCode("IT");
        mockDepartment.setName("Information Technology");
        mockDepartment.setDescription("IT Dept");

        mockDepartmentDto = DepartmentDto.builder()
                .code("it ")
                .name("Information Technology")
                .description("IT Dept")
                .build();
    }

    @Test
    void create_Success_FormatsCode() {
        when(departmentRepository.existsByCode("IT")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(mockDepartment);

        DepartmentDto result = departmentService.create(mockDepartmentDto);

        assertNotNull(result);
        assertEquals("IT", result.getCode());
    }

    @Test
    void create_DuplicateCode_ThrowsException() {
        when(departmentRepository.existsByCode("IT")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> departmentService.create(mockDepartmentDto));
    }

    @Test
    void update_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));
        mockDepartmentDto.setCode("CS");
        when(departmentRepository.existsByCode("CS")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(mockDepartment);

        DepartmentDto result = departmentService.update(1L, mockDepartmentDto);

        assertNotNull(result);
        verify(departmentRepository).save(mockDepartment);
    }

    @Test
    void update_DuplicateCode_ThrowsException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));
        mockDepartmentDto.setCode("CS");
        when(departmentRepository.existsByCode("CS")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> departmentService.update(1L, mockDepartmentDto));
    }

    @Test
    void update_DepartmentNotFound_ThrowsException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.update(1L, mockDepartmentDto));
    }

    @Test
    void delete_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));
        when(staffRepository.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.delete(1L);

        verify(departmentRepository).delete(mockDepartment);
    }

    @Test
    void delete_HasStaffs_ThrowsException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));
        when(staffRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> departmentService.delete(1L));
    }

    @Test
    void delete_DepartmentNotFound_ThrowsException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.delete(1L));
    }

    @Test
    void getById_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));
        DepartmentDto result = departmentService.getById(1L);
        assertEquals("IT", result.getCode());
    }

    @Test
    void getById_DepartmentNotFound_ThrowsException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.getById(1L));
    }

    @Test
    void getByCode_Success() {
        when(departmentRepository.findByCode("IT")).thenReturn(Optional.of(mockDepartment));
        DepartmentDto result = departmentService.getByCode("it ");
        assertEquals(1L, result.getId());
    }

    @Test
    void getByCode_DepartmentNotFound_ThrowsException() {
        when(departmentRepository.findByCode("IT")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.getByCode("IT"));
    }

    @Test
    void getAllDepartments_WithKeyword() {
        Page<Department> page = new PageImpl<>(List.of(mockDepartment));
        Pageable pageable = PageRequest.of(0, 10);
        when(departmentRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase("IT", "IT", pageable))
                .thenReturn(page);

        Page<DepartmentDto> result = departmentService.getAllDepartments("IT", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllDepartments_WithoutKeyword() {
        Page<Department> page = new PageImpl<>(List.of(mockDepartment));
        Pageable pageable = PageRequest.of(0, 10);
        when(departmentRepository.findAll(pageable)).thenReturn(page);

        Page<DepartmentDto> result = departmentService.getAllDepartments(null, pageable);

        assertEquals(1, result.getTotalElements());
    }
}
