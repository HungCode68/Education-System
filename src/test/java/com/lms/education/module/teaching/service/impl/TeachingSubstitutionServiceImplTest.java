package com.lms.education.module.teaching.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.teaching.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching.entity.TeachingSubstitution;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeachingSubstitutionServiceImplTest {

    @Mock
    private TeachingSubstitutionRepository teachingSubstitutionRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private TeachingSubstitutionServiceImpl teachingSubstitutionService;

    private TeachingSubstitution mockSubstitution;
    private TeachingSubstitutionDto mockDto;
    private Staff absentStaff;
    private Staff substituteStaff;
    private ClassSchedule mockSchedule;
    private Classes mockClass;

    @BeforeEach
    void setUp() {
        absentStaff = new Staff();
        absentStaff.setId(10L);
        absentStaff.setStaffType("TEACHER");

        substituteStaff = new Staff();
        substituteStaff.setId(20L);
        substituteStaff.setStaffType("TEACHER");

        mockClass = new Classes();
        mockClass.setId(100L);

        mockSchedule = new ClassSchedule();
        mockSchedule.setId(30L);
        mockSchedule.setClasses(mockClass);
        mockSchedule.setDayOfWeek(2);
        mockSchedule.setStartTime(LocalTime.of(8, 0));
        mockSchedule.setEndTime(LocalTime.of(10, 0));

        mockSubstitution = new TeachingSubstitution();
        mockSubstitution.setId(1L);
        mockSubstitution.setSchedule(mockSchedule);
        mockSubstitution.setAbsentStaff(absentStaff);
        mockSubstitution.setSubstituteStaff(substituteStaff);
        mockSubstitution.setReason("Sick");
        mockSubstitution.setStatus("APPROVED");

        mockDto = TeachingSubstitutionDto.builder()
                .scheduleId(30L)
                .absentStaffId(10L)
                .substituteStaffId(20L)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 2))
                .reason("Sick")
                .status("APPROVED")
                .build();
    }

    @Test
    void create_Success() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(substituteStaff));
        when(scheduleAssignmentRepository.existsTeacherConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(teachingSubstitutionRepository.existsSubstituteConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(teachingSubstitutionRepository.save(any(TeachingSubstitution.class))).thenReturn(mockSubstitution);

        TeachingSubstitutionDto result = teachingSubstitutionService.create(mockDto);

        assertNotNull(result);
        assertEquals("Sick", result.getReason());
    }

    @Test
    void create_ScheduleNotFound_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_AbsentStaffNotFound_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_AbsentStaffNotTeacher_ThrowsException() {
        absentStaff.setStaffType("ADMIN");
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        assertThrows(OperationNotPermittedException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_SubstituteStaffNotFound_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_SubstituteStaffNotTeacher_ThrowsException() {
        substituteStaff.setStaffType("ADMIN");
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(substituteStaff));
        assertThrows(OperationNotPermittedException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_SameTeacher_ThrowsException() {
        mockDto.setSubstituteStaffId(10L);
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        assertThrows(OperationNotPermittedException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_InvalidDates_ThrowsException() {
        mockDto.setStartDate(LocalDate.of(2024, 2, 1));
        mockDto.setEndDate(LocalDate.of(2024, 1, 1));
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(substituteStaff));
        assertThrows(OperationNotPermittedException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_RegularConflict_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(substituteStaff));
        when(scheduleAssignmentRepository.existsTeacherConflict(any(), any(), any(), any(), any())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void create_SubstituteConflict_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(substituteStaff));
        when(scheduleAssignmentRepository.existsTeacherConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(teachingSubstitutionRepository.existsSubstituteConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> teachingSubstitutionService.create(mockDto));
    }

    @Test
    void update_Success() {
        when(teachingSubstitutionRepository.findById(1L)).thenReturn(Optional.of(mockSubstitution));
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(absentStaff));
        when(staffRepository.findById(20L)).thenReturn(Optional.of(substituteStaff));
        when(scheduleAssignmentRepository.existsTeacherConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(teachingSubstitutionRepository.existsSubstituteConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(teachingSubstitutionRepository.save(any(TeachingSubstitution.class))).thenReturn(mockSubstitution);

        TeachingSubstitutionDto result = teachingSubstitutionService.update(1L, mockDto);

        assertNotNull(result);
        verify(teachingSubstitutionRepository).save(any(TeachingSubstitution.class));
    }

    @Test
    void update_NotFound_ThrowsException() {
        when(teachingSubstitutionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> teachingSubstitutionService.update(1L, mockDto));
    }

    @Test
    void delete_Success() {
        when(teachingSubstitutionRepository.findById(1L)).thenReturn(Optional.of(mockSubstitution));
        teachingSubstitutionService.delete(1L);
        verify(teachingSubstitutionRepository).delete(mockSubstitution);
    }

    @Test
    void getById_Success() {
        when(teachingSubstitutionRepository.findById(1L)).thenReturn(Optional.of(mockSubstitution));
        TeachingSubstitutionDto result = teachingSubstitutionService.getById(1L);
        assertEquals(10L, result.getAbsentStaffId());
    }

    @Test
    void getAll_Success() {
        Page<TeachingSubstitution> page = new PageImpl<>(List.of(mockSubstitution));
        Pageable pageable = PageRequest.of(0, 10);
        when(teachingSubstitutionRepository.searchSubstitutions("Sick", pageable)).thenReturn(page);
        Page<TeachingSubstitutionDto> result = teachingSubstitutionService.getAll("Sick", pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getSubstitutionsByClassId_Success() {
        when(teachingSubstitutionRepository.findByScheduleClassesId(100L)).thenReturn(List.of(mockSubstitution));
        List<TeachingSubstitutionDto> result = teachingSubstitutionService.getSubstitutionsByClassId(100L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getAvailableTeachers_Success() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));
        when(staffRepository.findByStaffTypeContainingIgnoreCase("TEACHER")).thenReturn(List.of(substituteStaff));
        when(scheduleAssignmentRepository.existsTeacherConflict(any(), any(), any(), any(), any())).thenReturn(false);
        when(teachingSubstitutionRepository.existsSubstituteConflict(any(), any(), any(), any(), any(), any(), any())).thenReturn(false);

        List<com.lms.education.module.user.dto.StaffDto> result = teachingSubstitutionService.getAvailableTeachers(
                30L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), null);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
    }
    
    @Test
    void getAvailableTeachers_InvalidDates_ThrowsException() {
        when(classScheduleRepository.findById(30L)).thenReturn(Optional.of(mockSchedule));

        assertThrows(OperationNotPermittedException.class, () -> teachingSubstitutionService.getAvailableTeachers(
                30L, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1), null));
    }
}
