package com.lms.education.module.teaching.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.teaching.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching.entity.TeachingAssignment;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import com.lms.education.module.user.entity.Staff;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeachingAssignmentServiceImplTest {

    @Mock
    private TeachingAssignmentRepository teachingAssignmentRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private ClassesRepository classesRepository;

    @InjectMocks
    private TeachingAssignmentServiceImpl teachingAssignmentService;

    private TeachingAssignment mockAssignment;
    private TeachingAssignmentDto mockDto;
    private Staff mockStaff;
    private Classes mockClass;

    @BeforeEach
    void setUp() {
        mockStaff = new Staff();
        mockStaff.setId(10L);
        mockStaff.setStaffType("TEACHER");
        mockStaff.setFullName("John Doe");

        mockClass = new Classes();
        mockClass.setId(20L);
        mockClass.setCode("C01");

        mockAssignment = new TeachingAssignment();
        mockAssignment.setId(1L);
        mockAssignment.setTeacher(mockStaff);
        mockAssignment.setClasses(mockClass);
        mockAssignment.setRole("MAIN_TEACHER");
        mockAssignment.setAssignedDate(LocalDate.of(2024, 1, 1));
        mockAssignment.setStatus("ACTIVE");

        mockDto = TeachingAssignmentDto.builder()
                .staffId(10L)
                .classId(20L)
                .role("MAIN_TEACHER")
                .assignedDate(LocalDate.of(2024, 1, 1))
                .status("ACTIVE")
                .build();
    }

    @Test
    void create_Success() {
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(mockClass));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(false);
        when(teachingAssignmentRepository.save(any(TeachingAssignment.class))).thenReturn(mockAssignment);

        TeachingAssignmentDto result = teachingAssignmentService.create(mockDto);

        assertNotNull(result);
        assertEquals("MAIN_TEACHER", result.getRole());
        verify(teachingAssignmentRepository).save(any(TeachingAssignment.class));
    }

    @Test
    void create_StaffNotFound_ThrowsException() {
        when(staffRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teachingAssignmentService.create(mockDto));
    }

    @Test
    void create_StaffNotTeacher_ThrowsException() {
        mockStaff.setStaffType("ADMIN");
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));

        assertThrows(OperationNotPermittedException.class, () -> teachingAssignmentService.create(mockDto));
    }

    @Test
    void create_ClassNotFound_ThrowsException() {
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(classesRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teachingAssignmentService.create(mockDto));
    }

    @Test
    void create_DuplicateAssignment_ThrowsException() {
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(mockClass));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teachingAssignmentService.create(mockDto));
    }

    @Test
    void create_InvalidDates_ThrowsException() {
        mockDto.setAssignedDate(LocalDate.of(2024, 5, 1));
        mockDto.setEndDate(LocalDate.of(2024, 4, 1));

        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(mockClass));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> teachingAssignmentService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(teachingAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(mockClass));
        when(teachingAssignmentRepository.save(any(TeachingAssignment.class))).thenReturn(mockAssignment);

        TeachingAssignmentDto result = teachingAssignmentService.update(1L, mockDto);

        assertNotNull(result);
    }

    @Test
    void update_AssignmentNotFound_ThrowsException() {
        when(teachingAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teachingAssignmentService.update(1L, mockDto));
    }
    
    @Test
    void update_DuplicateAssignment_ThrowsException() {
        when(teachingAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(staffRepository.findById(11L)).thenReturn(Optional.of(mockStaff));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(mockClass));
        
        mockDto.setStaffId(11L);
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(11L, 20L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teachingAssignmentService.update(1L, mockDto));
    }

    @Test
    void delete_Success() {
        when(teachingAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        
        teachingAssignmentService.delete(1L);
        
        verify(teachingAssignmentRepository).delete(mockAssignment);
    }

    @Test
    void getById_Success() {
        when(teachingAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        
        TeachingAssignmentDto result = teachingAssignmentService.getById(1L);
        
        assertEquals(10L, result.getStaffId());
    }

    @Test
    void getAllAssignments_Success() {
        Page<TeachingAssignment> page = new PageImpl<>(List.of(mockAssignment));
        Pageable pageable = PageRequest.of(0, 10);
        when(teachingAssignmentRepository.searchAssignments("John", pageable)).thenReturn(page);

        Page<TeachingAssignmentDto> result = teachingAssignmentService.getAllAssignments("John", pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAssignmentsByClassId_Success() {
        when(classesRepository.existsById(20L)).thenReturn(true);
        when(teachingAssignmentRepository.findByClassesId(20L)).thenReturn(List.of(mockAssignment));
        
        List<TeachingAssignmentDto> result = teachingAssignmentService.getAssignmentsByClassId(20L);
        
        assertEquals(1, result.size());
    }
}
