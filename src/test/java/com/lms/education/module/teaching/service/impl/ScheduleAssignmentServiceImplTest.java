package com.lms.education.module.teaching.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.teaching.dto.ScheduleAssignmentDto;
import com.lms.education.module.teaching.entity.ScheduleAssignment;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScheduleAssignmentServiceImplTest {

    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;

    @Mock
    private TeachingAssignmentRepository teachingAssignmentRepository;

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private ScheduleAssignmentServiceImpl scheduleAssignmentService;

    private ScheduleAssignment mockAssignment;
    private ScheduleAssignmentDto mockDto;
    private Staff mockStaff;
    private ClassSchedule mockSchedule;
    private Classes mockClass;

    @BeforeEach
    void setUp() {
        mockStaff = new Staff();
        mockStaff.setId(10L);
        mockStaff.setStaffType("TEACHER");
        mockStaff.setFullName("Jane Doe");

        mockClass = new Classes();
        mockClass.setId(20L);
        mockClass.setCode("C01");

        mockSchedule = new ClassSchedule();
        mockSchedule.setId(30L);
        mockSchedule.setClasses(mockClass);
        mockSchedule.setDayOfWeek(2);
        mockSchedule.setStartTime(LocalTime.of(8, 0));
        mockSchedule.setEndTime(LocalTime.of(10, 0));

        mockAssignment = new ScheduleAssignment();
        mockAssignment.setId(1L);
        mockAssignment.setTeacher(mockStaff);
        mockAssignment.setSchedule(mockSchedule);
        mockAssignment.setRole("MAIN_TEACHER");

        mockDto = ScheduleAssignmentDto.builder()
                .staffId(10L)
                .scheduleId(30L)
                .role("MAIN_TEACHER")
                .build();
    }

    @Test
    void create_Success() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(true);
        when(scheduleAssignmentRepository.existsByScheduleIdAndTeacherId(30L, 10L)).thenReturn(false);
        when(scheduleAssignmentRepository.existsTeacherConflict(10L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0), null)).thenReturn(false);
        when(scheduleAssignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(mockAssignment);

        ScheduleAssignmentDto result = scheduleAssignmentService.create(mockDto);

        assertNotNull(result);
        assertEquals("MAIN_TEACHER", result.getRole());
        verify(scheduleAssignmentRepository).save(any(ScheduleAssignment.class));
    }

    @Test
    void create_ScheduleNotFound_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleAssignmentService.create(mockDto));
    }

    @Test
    void create_StaffNotFound_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleAssignmentService.create(mockDto));
    }

    @Test
    void create_StaffNotTeacher_ThrowsException() {
        mockStaff.setStaffType("ADMIN");
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));

        assertThrows(OperationNotPermittedException.class, () -> scheduleAssignmentService.create(mockDto));
    }

    @Test
    void create_NotAssignedToClass_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(false);

        assertThrows(OperationNotPermittedException.class, () -> scheduleAssignmentService.create(mockDto));
    }

    @Test
    void create_DuplicateAssignment_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(true);
        when(scheduleAssignmentRepository.existsByScheduleIdAndTeacherId(30L, 10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> scheduleAssignmentService.create(mockDto));
    }

    @Test
    void create_TeacherConflict_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(true);
        when(scheduleAssignmentRepository.existsByScheduleIdAndTeacherId(30L, 10L)).thenReturn(false);
        when(scheduleAssignmentRepository.existsTeacherConflict(10L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0), null)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> scheduleAssignmentService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(scheduleAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(teachingAssignmentRepository.existsByTeacherIdAndClassesId(10L, 20L)).thenReturn(true);
        when(scheduleAssignmentRepository.existsTeacherConflict(10L, 2, LocalTime.of(8, 0), LocalTime.of(10, 0), 1L)).thenReturn(false);
        when(scheduleAssignmentRepository.save(any(ScheduleAssignment.class))).thenReturn(mockAssignment);

        ScheduleAssignmentDto result = scheduleAssignmentService.update(1L, mockDto);

        assertNotNull(result);
        verify(scheduleAssignmentRepository).save(any(ScheduleAssignment.class));
    }

    @Test
    void update_AssignmentNotFound_ThrowsException() {
        when(scheduleAssignmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> scheduleAssignmentService.update(1L, mockDto));
    }

    @Test
    void delete_Success() {
        when(scheduleAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        
        scheduleAssignmentService.delete(1L);
        
        verify(scheduleAssignmentRepository).delete(mockAssignment);
    }

    @Test
    void getById_Success() {
        when(scheduleAssignmentRepository.findById(1L)).thenReturn(Optional.of(mockAssignment));
        
        ScheduleAssignmentDto result = scheduleAssignmentService.getById(1L);
        
        assertEquals(10L, result.getStaffId());
    }

    @Test
    void getAll_Success() {
        Page<ScheduleAssignment> page = new PageImpl<>(List.of(mockAssignment));
        Pageable pageable = PageRequest.of(0, 10);
        when(scheduleAssignmentRepository.searchAssignments("Jane", pageable)).thenReturn(page);

        Page<ScheduleAssignmentDto> result = scheduleAssignmentService.getAll("Jane", pageable);

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void getAssignmentsByClassId_Success() {
        when(scheduleAssignmentRepository.findByScheduleClassesId(20L)).thenReturn(List.of(mockAssignment));
        
        List<ScheduleAssignmentDto> result = scheduleAssignmentService.getAssignmentsByClassId(20L);
        
        assertEquals(1, result.size());
    }
    
    @Test
    void getAssignmentsByScheduleId_Success() {
        when(scheduleAssignmentRepository.findByScheduleId(30L)).thenReturn(List.of(mockAssignment));
        
        List<ScheduleAssignmentDto> result = scheduleAssignmentService.getAssignmentsByScheduleId(30L);
        
        assertEquals(1, result.size());
    }
}
