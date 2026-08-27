package com.lms.education.module.enrollment.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.enrollment.dto.BulkEnrollmentDto;
import com.lms.education.module.enrollment.dto.EnrollmentDto;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Student mockStudent;
    private Classes mockClass;
    private Enrollment mockEnrollment;
    private EnrollmentDto mockDto;

    @BeforeEach
    void setUp() {
        mockStudent = new Student();
        mockStudent.setId(1L);
        mockStudent.setFullName("Test Student");

        mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setName("Test Class");
        mockClass.setCurrentStudents(10);
        mockClass.setMaxStudents(20);

        mockEnrollment = new Enrollment();
        mockEnrollment.setId(100L);
        mockEnrollment.setStudent(mockStudent);
        mockEnrollment.setClasses(mockClass);
        mockEnrollment.setStatus("ACTIVE");
        mockEnrollment.setEnrollmentDate(LocalDate.now());

        mockDto = EnrollmentDto.builder()
                .studentId(1L)
                .classId(10L)
                .status("ACTIVE")
                .enrollmentDate(LocalDate.now())
                .build();
    }

    @Test
    void create_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        EnrollmentDto result = enrollmentService.create(mockDto);

        assertNotNull(result);
        assertEquals("Test Student", result.getStudentName());
        assertEquals(11, mockClass.getCurrentStudents());
        verify(classesRepository).save(mockClass);
    }

    @Test
    void create_StudentNotFound_ThrowsException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.create(mockDto));
    }

    @Test
    void create_DuplicateEnrollment_ThrowsException() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> enrollmentService.create(mockDto));
    }

    @Test
    void create_MaxCapacityReached_ThrowsException() {
        mockClass.setCurrentStudents(20);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 10L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> enrollmentService.create(mockDto));
    }

    @Test
    void update_Success_StatusChange() {
        mockDto.setStatus("DROPPED");
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(mockEnrollment));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        EnrollmentDto result = enrollmentService.update(100L, mockDto);

        assertNotNull(result);
        assertEquals(9, mockClass.getCurrentStudents()); // Dropped -> decrement capacity
        verify(classesRepository).save(mockClass);
    }
    
    @Test
    void update_Success_ClassChange() {
        Classes newClass = new Classes();
        newClass.setId(20L);
        newClass.setCurrentStudents(5);
        newClass.setMaxStudents(20);
        
        mockDto.setClassId(20L);
        
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(mockEnrollment));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(newClass));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 20L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        EnrollmentDto result = enrollmentService.update(100L, mockDto);

        assertNotNull(result);
        assertEquals(9, mockClass.getCurrentStudents()); // old class decremented
        assertEquals(6, newClass.getCurrentStudents()); // new class incremented
        verify(classesRepository).save(mockClass);
        verify(classesRepository).save(newClass);
    }
    
    @Test
    void update_DuplicateEnrollment_ThrowsException() {
        mockDto.setClassId(20L);
        Classes newClass = new Classes();
        newClass.setId(20L);
        
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(mockEnrollment));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(classesRepository.findById(20L)).thenReturn(Optional.of(newClass));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 20L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> enrollmentService.update(100L, mockDto));
    }

    @Test
    void delete_Success() {
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(mockEnrollment));

        enrollmentService.delete(100L);

        verify(enrollmentRepository).delete(mockEnrollment);
        assertEquals(9, mockClass.getCurrentStudents()); // deleted ACTIVE -> decrement
        verify(classesRepository).save(mockClass);
    }

    @Test
    void getById_Success() {
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(mockEnrollment));
        EnrollmentDto result = enrollmentService.getById(100L);
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void getAll_Success() {
        Page<Enrollment> page = new PageImpl<>(List.of(mockEnrollment));
        Pageable pageable = PageRequest.of(0, 10);
        when(enrollmentRepository.findAll(pageable)).thenReturn(page);

        Page<EnrollmentDto> result = enrollmentService.getAll(null, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getByStudentId_Success() {
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(mockEnrollment));
        List<EnrollmentDto> result = enrollmentService.getByStudentId(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getByClassId_Success() {
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollment));
        List<EnrollmentDto> result = enrollmentService.getByClassId(10L);
        assertEquals(1, result.size());
    }

    @Test
    void enrollBulk_Success() {
        BulkEnrollmentDto bulkDto = new BulkEnrollmentDto();
        bulkDto.setClassId(10L);
        bulkDto.setStudentIds(List.of(1L, 2L));
        
        Student student2 = new Student();
        student2.setId(2L);
        
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 10L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndClassesId(2L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        Map<String, Object> result = enrollmentService.enrollBulk(bulkDto);

        assertEquals(2, result.get("successCount"));
        assertEquals(0, result.get("failureCount"));
        assertEquals(12, mockClass.getCurrentStudents());
    }

    @Test
    void enrollBulk_PartialFailure_ReturnsReport() {
        BulkEnrollmentDto bulkDto = new BulkEnrollmentDto();
        bulkDto.setClassId(10L);
        bulkDto.setStudentIds(List.of(1L, 2L));
        
        Student student2 = new Student();
        student2.setId(2L);
        
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(enrollmentRepository.existsByStudentIdAndClassesId(1L, 10L)).thenReturn(true); // Fail for 1L
        when(enrollmentRepository.existsByStudentIdAndClassesId(2L, 10L)).thenReturn(false); // Success for 2L
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        Map<String, Object> result = enrollmentService.enrollBulk(bulkDto);

        assertEquals(1, result.get("successCount"));
        assertEquals(1, result.get("failureCount"));
        assertEquals(11, mockClass.getCurrentStudents());
    }
}
