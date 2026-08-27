package com.lms.education.module.reporting.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.attendance.repository.AttendanceRepository;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.module.reporting.dto.ReportClassMetricsDto;
import com.lms.education.module.reporting.entity.ReportClassMetrics;
import com.lms.education.module.reporting.repository.ReportClassMetricsRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportClassMetricsServiceImplTest {

    @Mock
    private ReportClassMetricsRepository metricsRepository;
    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private ScheduleAssignmentRepository scheduleAssignmentRepository;
    @Mock
    private TeachingSubstitutionRepository teachingSubstitutionRepository;

    @InjectMocks
    private ReportClassMetricsServiceImpl reportClassMetricsService;

    private Classes mockClass;
    private Enrollment mockEnrollmentActive;
    private Enrollment mockEnrollmentDropped;

    @BeforeEach
    void setUp() {
        mockClass = new Classes();
        mockClass.setId(10L);
        mockClass.setName("Test Class");

        mockEnrollmentActive = new Enrollment();
        mockEnrollmentActive.setId(1L);
        mockEnrollmentActive.setStatus("ACTIVE");
        
        mockEnrollmentDropped = new Enrollment();
        mockEnrollmentDropped.setId(2L);
        mockEnrollmentDropped.setStatus("DROPPED");
    }

    @Test
    void generateOrUpdateClassMetrics_Success() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollmentActive, mockEnrollmentDropped));
        when(attendanceRepository.countTotalAttendanceByClassId(10L)).thenReturn(10L);
        when(attendanceRepository.countPresentAttendanceByClassId(10L)).thenReturn(8L);
        when(submissionRepository.calculateAverageScoreByClassId(10L)).thenReturn(8.5);
        when(metricsRepository.findById(10L)).thenReturn(Optional.empty());
        when(metricsRepository.save(any(ReportClassMetrics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportClassMetricsDto result = reportClassMetricsService.generateOrUpdateClassMetrics(10L);

        assertNotNull(result);
        assertEquals(1, result.getTotalStudents());
        assertEquals(1, result.getDroppedStudents());
        assertEquals(0, result.getAverageAttendanceRate().compareTo(new BigDecimal("80.00")));
        assertEquals(0, result.getAverageAssignmentScore().compareTo(new BigDecimal("8.50")));
    }
    
    @Test
    void generateOrUpdateClassMetrics_ClassNotFound_ThrowsException() {
        when(classesRepository.findById(10L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> reportClassMetricsService.generateOrUpdateClassMetrics(10L));
    }

    @Test
    void getClassMetrics_Success() {
        ReportClassMetrics mockMetrics = new ReportClassMetrics();
        mockMetrics.setClassId(10L);
        mockMetrics.setTotalStudents(5);

        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(metricsRepository.findById(10L)).thenReturn(Optional.of(mockMetrics));

        ReportClassMetricsDto result = reportClassMetricsService.getClassMetrics(10L);

        assertNotNull(result);
        assertEquals(5, result.getTotalStudents());
    }

    @Test
    void getClassMetricsInRange_Success() {
        when(classesRepository.findById(10L)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.findByClassesId(10L)).thenReturn(List.of(mockEnrollmentActive));
        when(attendanceRepository.countTotalAttendanceByClassIdInRange(eq(10L), any(), any())).thenReturn(5L);
        when(attendanceRepository.countPresentAttendanceByClassIdInRange(eq(10L), any(), any())).thenReturn(5L);
        when(submissionRepository.calculateAverageScoreByClassIdInRange(eq(10L), any(), any())).thenReturn(9.0);

        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        ReportClassMetricsDto result = reportClassMetricsService.getClassMetricsInRange(10L, startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.getTotalStudents());
        assertEquals(0, result.getAverageAttendanceRate().compareTo(new BigDecimal("100.00")));
        assertEquals(0, result.getAverageAssignmentScore().compareTo(new BigDecimal("9.00")));
    }
}
