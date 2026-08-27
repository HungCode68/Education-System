package com.lms.education.module.attendance.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.attendance.dto.AttendanceDto;
import com.lms.education.module.attendance.entity.Attendance;
import com.lms.education.module.attendance.repository.AttendanceRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private ClassSchedule mockSchedule;
    private Classes mockClass;
    private Student mockStudent;
    private Enrollment mockEnrollment;
    private Attendance mockAttendance;
    private LocalDate mockDate;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setName("Test Class");

        mockSchedule = new ClassSchedule();
        mockSchedule.setId(100L);
        mockSchedule.setClasses(mockClass);
        // Monday is dayOfWeek = 2
        mockSchedule.setDayOfWeek(2);

        mockStudent = new Student();
        mockStudent.setId(1L);
        mockStudent.setFullName("Test Student");

        mockEnrollment = new Enrollment();
        mockEnrollment.setId(1000L);
        mockEnrollment.setStudent(mockStudent);
        mockEnrollment.setClasses(mockClass);
        mockEnrollment.setStatus("ACTIVE");
        
        // Let's pick a Monday date
        // 2023-10-09 is Monday
        mockDate = LocalDate.of(2023, 10, 9);

        mockAttendance = new Attendance();
        mockAttendance.setId(5L);
        mockAttendance.setSchedule(mockSchedule);
        mockAttendance.setStudent(mockStudent);
        mockAttendance.setAttendanceDate(mockDate);
        mockAttendance.setStatus("PRESENT");
    }

    @Test
    void getAttendanceSheetByScheduleAndDate_Success() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollment));
        when(attendanceRepository.findByScheduleIdAndAttendanceDate(100L, mockDate)).thenReturn(List.of(mockAttendance));

        List<AttendanceDto> result = attendanceService.getAttendanceSheetByScheduleAndDate(100L, mockDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PRESENT", result.get(0).getStatus());
    }
    
    @Test
    void getAttendanceSheetByScheduleAndDate_Unmarked_Success() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollment));
        when(attendanceRepository.findByScheduleIdAndAttendanceDate(100L, mockDate)).thenReturn(List.of());

        List<AttendanceDto> result = attendanceService.getAttendanceSheetByScheduleAndDate(100L, mockDate);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getStatus());
    }

    @Test
    void getAttendanceSheetByScheduleAndDate_ScheduleNotFound_ThrowsException() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceSheetByScheduleAndDate(100L, mockDate));
    }

    @Test
    void getAttendanceSheetByScheduleAndDate_ClassNotLinked_ThrowsException() {
        mockSchedule.setClasses(null);
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceSheetByScheduleAndDate(100L, mockDate));
    }
    
    @Test
    void getAttendanceSheetByScheduleAndDate_InvalidDateDow_ThrowsException() {
        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        // 2023-10-10 is Tuesday, but schedule is Monday (2)
        LocalDate invalidDate = LocalDate.of(2023, 10, 10);

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceSheetByScheduleAndDate(100L, invalidDate));
    }

    @Test
    void markAttendance_Success() {
        AttendanceDto dto = new AttendanceDto();
        dto.setScheduleId(100L);
        dto.setStudentId(1L);
        dto.setAttendanceDate(mockDate);
        dto.setStatus("ABSENT");

        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(attendanceRepository.findByScheduleIdAndStudentIdAndAttendanceDate(100L, 1L, mockDate)).thenReturn(Optional.of(mockAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(mockAttendance);

        AttendanceDto result = attendanceService.markAttendance(dto);

        assertNotNull(result);
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void markAttendance_StudentNotFound_ThrowsException() {
        AttendanceDto dto = new AttendanceDto();
        dto.setScheduleId(100L);
        dto.setStudentId(1L);
        dto.setAttendanceDate(mockDate);

        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.markAttendance(dto));
    }

    @Test
    void batchMarkAttendance_Success() {
        AttendanceDto dto = new AttendanceDto();
        dto.setStudentId(1L);
        dto.setStatus("ABSENT");

        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(attendanceRepository.findByScheduleIdAndStudentIdAndAttendanceDate(100L, 1L, mockDate)).thenReturn(Optional.of(mockAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(mockAttendance);
        
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollment));
        when(attendanceRepository.findByScheduleIdAndAttendanceDate(100L, mockDate)).thenReturn(List.of(mockAttendance));

        List<AttendanceDto> result = attendanceService.batchMarkAttendance(100L, mockDate, List.of(dto));

        assertEquals(1, result.size());
        verify(attendanceRepository).save(any(Attendance.class));
    }
    
    @Test
    void batchMarkAttendance_NullStatus_DeletesExisting() {
        AttendanceDto dto = new AttendanceDto();
        dto.setStudentId(1L);
        dto.setStatus(null);

        when(classScheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(attendanceRepository.findByScheduleIdAndStudentIdAndAttendanceDate(100L, 1L, mockDate)).thenReturn(Optional.of(mockAttendance));
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollment));
        when(attendanceRepository.findByScheduleIdAndAttendanceDate(100L, mockDate)).thenReturn(List.of());

        List<AttendanceDto> result = attendanceService.batchMarkAttendance(100L, mockDate, List.of(dto));

        assertNotNull(result);
        verify(attendanceRepository).delete(mockAttendance);
    }

    @Test
    void getAttendanceByStudent_Success() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(attendanceRepository.findByStudentId(1L)).thenReturn(List.of(mockAttendance));

        List<AttendanceDto> result = attendanceService.getAttendanceByStudent(1L);
        assertEquals(1, result.size());
    }
    
    @Test
    void getAttendanceByStudent_NotFound_ThrowsException() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.getAttendanceByStudent(1L));
    }

    @Test
    void deleteAttendance_Success() {
        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(mockAttendance));
        
        attendanceService.deleteAttendance(5L);
        
        verify(attendanceRepository).delete(mockAttendance);
    }
    
    @Test
    void deleteAttendance_NotFound_ThrowsException() {
        when(attendanceRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attendanceService.deleteAttendance(5L));
    }
}
