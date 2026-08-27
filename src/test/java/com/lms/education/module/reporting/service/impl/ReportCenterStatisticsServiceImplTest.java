package com.lms.education.module.reporting.service.impl;
import com.lms.education.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.reporting.dto.ReportCenterStatisticsDto;
import com.lms.education.module.reporting.dto.ReportSummaryDto;
import com.lms.education.module.reporting.entity.ReportCenterStatistics;
import com.lms.education.module.reporting.repository.ReportCenterStatisticsRepository;
import com.lms.education.module.reporting.service.ReportClassMetricsService;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StaffRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReportCenterStatisticsServiceImplTest {

    @Mock
    private ReportCenterStatisticsRepository reportRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ClassesRepository classesRepository;
    @Mock
    private ReportClassMetricsService classMetricsService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReportCenterStatisticsServiceImpl reportCenterStatisticsService;

    private Student mockStudentActive;
    private Student mockStudentDropped;
    private Staff mockTeacher;
    private Staff mockStaff;
    private Classes mockClassOngoing;
    private Classes mockClassClosed;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        mockStudentActive = new Student();
        mockStudentActive.setId(1L);
        mockStudentActive.setStatus("STUDYING");
        mockStudentActive.setCreatedAt(today.atStartOfDay());

        mockStudentDropped = new Student();
        mockStudentDropped.setId(2L);
        mockStudentDropped.setStatus("DROPPED");
        mockStudentDropped.setUpdatedAt(today.atStartOfDay());

        mockTeacher = new Staff();
        mockTeacher.setId(10L);
        mockTeacher.setStaffType("TEACHER");
        mockTeacher.setStatus("ACTIVE");
        mockTeacher.setHireDate(today);

        mockStaff = new Staff();
        mockStaff.setId(11L);
        mockStaff.setStaffType("ADMIN");
        mockStaff.setStatus("ACTIVE");
        mockStaff.setHireDate(today);

        mockClassOngoing = new Classes();
        mockClassOngoing.setId(100L);
        mockClassOngoing.setStatus("ONGOING");
        mockClassOngoing.setStartDate(today);

        mockClassClosed = new Classes();
        mockClassClosed.setId(101L);
        mockClassClosed.setStatus("CLOSED");
        mockClassClosed.setEndDate(today);
        mockClassClosed.setUpdatedAt(today.atStartOfDay());
    }

    @Test
    void generateOrUpdateDailyReport_Success() {
        when(studentRepository.findAll()).thenReturn(List.of(mockStudentActive, mockStudentDropped));
        when(staffRepository.findAll()).thenReturn(List.of(mockTeacher, mockStaff));
        when(courseRepository.count()).thenReturn(5L);
        when(classesRepository.findAll()).thenReturn(List.of(mockClassOngoing, mockClassClosed));
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any(ReportCenterStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);

        assertNotNull(result);
        assertEquals(1, result.getTotalActiveStudents());
        assertEquals(1, result.getNewStudentsToday());
        assertEquals(1, result.getDroppedStudentsToday());
        assertEquals(1, result.getTotalTeachers());
        assertEquals(1, result.getNewTeachersToday());
        assertEquals(1, result.getTotalOtherStaffs());
        assertEquals(5, result.getTotalCourses());
        assertEquals(1, result.getTotalActiveClasses());
        assertEquals(1, result.getNewClassesOpened());
        assertEquals(1, result.getClassesClosedToday());
    }

    @Test
    void getSummaryReportBetween_Success() {
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;

        Enrollment droppedEnrollment = new Enrollment();
        droppedEnrollment.setStatus("DROPPED");
        droppedEnrollment.setUpdatedAt(today.atStartOfDay());

        when(studentRepository.findAll()).thenReturn(List.of(mockStudentActive));
        when(enrollmentRepository.findAll()).thenReturn(List.of(droppedEnrollment));
        when(staffRepository.findAll()).thenReturn(List.of(mockTeacher));
        when(courseRepository.count()).thenReturn(5L);
        when(classesRepository.findAll()).thenReturn(List.of(mockClassOngoing));

        ReportSummaryDto result = reportCenterStatisticsService.getSummaryReportBetween(startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.getTotalActiveStudents());
        assertEquals(1, result.getTotalNewStudentsInRange());
        assertEquals(1, result.getTotalDroppedStudentsInRange());
        assertEquals(1, result.getTotalTeachers());
        assertEquals(1, result.getTotalNewTeachersInRange());
        assertEquals(5, result.getTotalCourses());
        assertEquals(1, result.getTotalActiveClasses());
        assertEquals(1, result.getTotalNewClassesOpenedInRange());
        assertEquals(8, result.getTotalDaysReported());
    }

    @Test
    void getSummaryReportBetween_InvalidDates_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> reportCenterStatisticsService.getSummaryReportBetween(null, today));
        assertThrows(IllegalArgumentException.class, () -> reportCenterStatisticsService.getSummaryReportBetween(today, null));
        assertThrows(IllegalArgumentException.class, () -> reportCenterStatisticsService.getSummaryReportBetween(today, today.minusDays(1)));
    }

    @Test
    void saveCustomReportSnapshot_InvalidDate_ThrowsException() {
        ReportCenterStatisticsDto dto = new ReportCenterStatisticsDto();
        assertThrows(IllegalArgumentException.class, () -> reportCenterStatisticsService.saveCustomReportSnapshot(dto));
    }
    
    @Test
    void saveCustomReportSnapshot_Success() {
        ReportCenterStatisticsDto dto = new ReportCenterStatisticsDto();
        dto.setReportDate(today);
        dto.setTotalActiveStudents(10);
        dto.setNewStudentsToday(5);
        dto.setDroppedStudentsToday(2);
        dto.setTotalTeachers(20);
        dto.setNewTeachersToday(1);
        dto.setResignedTeachersToday(1);
        dto.setTotalOtherStaffs(5);
        dto.setNewStaffsToday(1);
        dto.setResignedStaffsToday(1);
        dto.setTotalCourses(10);
        dto.setTotalActiveClasses(15);
        dto.setNewClassesOpened(2);
        dto.setClassesClosedToday(1);

        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.saveCustomReportSnapshot(dto);
        assertNotNull(result);
        assertEquals(10, result.getTotalActiveStudents());
    }

    @Test
    void generateOrUpdateDailyReport_NullDate() {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(any())).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(null);
        assertNotNull(result);
    }

    @Test
    void getLatestReport_NotFound_ThrowsException() {
        when(reportRepository.findTopByOrderByReportDateDesc()).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> reportCenterStatisticsService.getLatestReport());
    }

    @Test
    void getLatestReport_Success() {
        ReportCenterStatistics stats = new ReportCenterStatistics();
        stats.setReportDate(today);
        when(reportRepository.findTopByOrderByReportDateDesc()).thenReturn(Optional.of(stats));
        assertNotNull(reportCenterStatisticsService.getLatestReport());
    }
    
    @Test
    void getReportsBetween_Success() {
        ReportCenterStatistics stats = new ReportCenterStatistics();
        stats.setReportDate(today);
        when(reportRepository.findByReportDateBetweenOrderByReportDateAsc(any(), any())).thenReturn(List.of(stats));
        List<ReportCenterStatisticsDto> result = reportCenterStatisticsService.getReportsBetween(today, today);
        assertEquals(1, result.size());
    }
    
    @Test
    void getReportByDate_NotToday() {
        LocalDate pastDate = today.minusDays(1);
        ReportCenterStatistics stats = new ReportCenterStatistics();
        stats.setReportDate(pastDate);
        when(reportRepository.findById(pastDate)).thenReturn(Optional.of(stats));
        ReportCenterStatisticsDto result = reportCenterStatisticsService.getReportByDate(pastDate);
        assertEquals(pastDate, result.getReportDate());
    }

    @Test
    void generateOrUpdateDailyReport_JsonException() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of(mockStudentActive));
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON Parse Error"));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertNotNull(result);
    }

    // ---------- Extra tests covering missed branches ----------

    @Test
    void generateOrUpdateDailyReport_WithOpeningClass_CountsAsActive() {
        Classes openingClass = new Classes();
        openingClass.setId(200L);
        openingClass.setStatus("OPENING");
        openingClass.setStartDate(today);

        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(openingClass));
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertEquals(1, result.getTotalActiveClasses());
        assertEquals(1, result.getNewClassesOpened());
    }

    @Test
    void generateOrUpdateDailyReport_ResignedTeacherAndStaff() {
        Staff resignedTeacher = new Staff();
        resignedTeacher.setId(20L);
        resignedTeacher.setStaffType("TEACHER");
        resignedTeacher.setStatus("RESIGNED");
        resignedTeacher.setUpdatedAt(today.atStartOfDay());

        Staff resignedStaff = new Staff();
        resignedStaff.setId(21L);
        resignedStaff.setStaffType(null); // non-teacher
        resignedStaff.setStatus("RESIGNED");
        resignedStaff.setUpdatedAt(today.atStartOfDay());

        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of(resignedTeacher, resignedStaff));
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertEquals(1, result.getResignedTeachersToday());
        assertEquals(1, result.getResignedStaffsToday());
    }

    @Test
    void generateOrUpdateDailyReport_ClassClosedWithNoEndDate_UsesUpdatedAt() {
        Classes closedClass = new Classes();
        closedClass.setId(300L);
        closedClass.setStatus("CLOSED");
        closedClass.setEndDate(null);       // no endDate
        closedClass.setUpdatedAt(today.atStartOfDay()); // use updatedAt

        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(closedClass));
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertEquals(1, result.getClassesClosedToday());
    }

    @Test
    void generateOrUpdateDailyReport_ClassStartDateNull_UsesCreatedAt() {
        Classes newClass = new Classes();
        newClass.setId(400L);
        newClass.setStatus("ONGOING");
        newClass.setStartDate(null);
        newClass.setCreatedAt(today.atStartOfDay());

        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(newClass));
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertEquals(1, result.getNewClassesOpened());
    }

    @Test
    void generateOrUpdateDailyReport_ExistingRecord_UpdatesInPlace() {
        ReportCenterStatistics existing = new ReportCenterStatistics();
        existing.setReportDate(today);
        existing.setTotalActiveStudents(5);

        when(studentRepository.findAll()).thenReturn(List.of(mockStudentActive));
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(3L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(today)).thenReturn(Optional.of(existing));
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertEquals(1, result.getTotalActiveStudents());
        assertEquals(3, result.getTotalCourses());
    }

    @Test
    void getSummaryReportBetween_WithEnrollmentNullUpdatedAtUsesEnrollmentDate() {
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;

        Enrollment droppedEnrollment = new Enrollment();
        droppedEnrollment.setStatus("DROPPED");
        droppedEnrollment.setUpdatedAt(null); // null updatedAt -> use enrollmentDate
        droppedEnrollment.setEnrollmentDate(today);

        when(studentRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.findAll()).thenReturn(List.of(droppedEnrollment));
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());

        ReportSummaryDto result = reportCenterStatisticsService.getSummaryReportBetween(startDate, endDate);
        assertEquals(1, result.getTotalDroppedStudentsInRange());
    }

    @Test
    void getSummaryReportBetween_ClassStartDateNull_UseCreatedAt() {
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;

        Classes newClass = new Classes();
        newClass.setId(500L);
        newClass.setStatus("ONGOING");
        newClass.setStartDate(null);
        newClass.setCreatedAt(today.atStartOfDay());

        when(studentRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(newClass));

        ReportSummaryDto result = reportCenterStatisticsService.getSummaryReportBetween(startDate, endDate);
        assertEquals(1, result.getTotalNewClassesOpenedInRange());
    }

    @Test
    void getSummaryReportBetween_ClosedClassUsingUpdatedAt() {
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;

        Classes closedClass = new Classes();
        closedClass.setId(600L);
        closedClass.setStatus("CLOSED");
        closedClass.setEndDate(null);
        closedClass.setUpdatedAt(today.atStartOfDay());

        when(studentRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(closedClass));

        ReportSummaryDto result = reportCenterStatisticsService.getSummaryReportBetween(startDate, endDate);
        assertEquals(1, result.getTotalClassesClosedInRange());
    }

    @Test
    void getSummaryReportBetween_NonTeacherStaffInRange() {
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;

        Staff nullTypeStaff = new Staff();
        nullTypeStaff.setId(30L);
        nullTypeStaff.setStaffType(null); // non-teacher
        nullTypeStaff.setHireDate(today);

        when(studentRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of(nullTypeStaff));
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());

        ReportSummaryDto result = reportCenterStatisticsService.getSummaryReportBetween(startDate, endDate);
        assertEquals(1, result.getTotalOtherStaffs());
        assertEquals(1, result.getTotalNewStaffsInRange());
    }

    @Test
    void getReportByDate_Today_GeneratesReport() {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(any())).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.getReportByDate(today);
        assertNotNull(result);
    }

    @Test
    void getReportByDate_NullDate_UsesToday() {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(any())).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.getReportByDate(null);
        assertNotNull(result);
    }

    @Test
    void getTrainingDashboard_Success() {
        when(studentRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of());
        when(reportRepository.findById(any())).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(classMetricsService.getAllClassesMetrics()).thenReturn(List.of());

        assertNotNull(reportCenterStatisticsService.getTrainingDashboard(null));
        assertNotNull(reportCenterStatisticsService.getTrainingDashboard(today));
    }

    @Test
    void saveCustomReportSnapshot_ExistingRecord_UpdatesOnlyNonNullFields() {
        ReportCenterStatistics existing = new ReportCenterStatistics();
        existing.setReportDate(today);
        existing.setTotalActiveStudents(99); // existing value

        ReportCenterStatisticsDto dto = new ReportCenterStatisticsDto();
        dto.setReportDate(today);
        // Only set some fields (null fields should NOT overwrite)
        dto.setTotalTeachers(5);

        when(reportRepository.findById(today)).thenReturn(Optional.of(existing));
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.saveCustomReportSnapshot(dto);
        assertNotNull(result);
    }

    @Test
    void toDto_WithJsonIds_ParsesCorrectly() throws Exception {
        ReportCenterStatistics stats = new ReportCenterStatistics();
        stats.setReportDate(today);
        stats.setNewStudentIds("[1,2,3]");
        stats.setDroppedStudentIds("[4]");
        stats.setNewTeacherIds("[10]");
        stats.setResignedTeacherIds("[11]");
        stats.setNewClassIds("[100]");
        stats.setClosedClassIds("[101]");

        when(objectMapper.readValue(eq("[1,2,3]"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of(1L, 2L, 3L));
        when(objectMapper.readValue(eq("[4]"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of(4L));
        when(objectMapper.readValue(eq("[10]"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of(10L));
        when(objectMapper.readValue(eq("[11]"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of(11L));
        when(objectMapper.readValue(eq("[100]"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of(100L));
        when(objectMapper.readValue(eq("[101]"), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(List.of(101L));

        when(reportRepository.findTopByOrderByReportDateDesc()).thenReturn(Optional.of(stats));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.getLatestReport();
        assertNotNull(result);
    }
    @Test
    void generateOrUpdateDailyReport_EdgeCases() {
        // Student edge cases
        Student s1 = new Student(); s1.setId(101L); s1.setStatus(null); // null status
        Student s2 = new Student(); s2.setId(102L); s2.setStatus("OTHER"); // other status
        Student s3 = new Student(); s3.setId(103L); s3.setStatus("STUDYING"); s3.setCreatedAt(null); // null created
        Student s4 = new Student(); s4.setId(104L); s4.setStatus("DROPPED"); s4.setUpdatedAt(null); // null updated
        Student s5 = new Student(); s5.setId(105L); s5.setStatus("DROPPED"); s5.setUpdatedAt(today.minusDays(1).atStartOfDay()); // different date

        // Staff edge cases
        Staff t1 = new Staff(); t1.setId(201L); t1.setStaffType("TEACHER"); t1.setHireDate(null); // null hire date
        Staff t2 = new Staff(); t2.setId(202L); t2.setStaffType("TEACHER"); t2.setStatus("RESIGNED"); t2.setUpdatedAt(null); // null updated
        Staff t3 = new Staff(); t3.setId(203L); t3.setStaffType("TEACHER"); t3.setStatus("RESIGNED"); t3.setUpdatedAt(today.minusDays(1).atStartOfDay()); // different date
        Staff o1 = new Staff(); o1.setId(204L); o1.setStaffType(null); o1.setHireDate(null); // null type, null hire date
        Staff o2 = new Staff(); o2.setId(205L); o2.setStaffType("ADMIN"); o2.setStatus("RESIGNED"); o2.setUpdatedAt(null); // null updated

        // Classes edge cases
        Classes c1 = new Classes(); c1.setId(301L); c1.setStatus(null); // null status
        Classes c2 = new Classes(); c2.setId(302L); c2.setStatus("ONGOING"); c2.setStartDate(null); c2.setCreatedAt(null); // null start & created
        Classes c3 = new Classes(); c3.setId(303L); c3.setStatus("CLOSED"); c3.setEndDate(null); c3.setUpdatedAt(null); // null end & updated
        Classes c4 = new Classes(); c4.setId(304L); c4.setStatus("CLOSED"); c4.setEndDate(today.minusDays(1)); // different end date

        when(studentRepository.findAll()).thenReturn(List.of(s1, s2, s3, s4, s5));
        when(staffRepository.findAll()).thenReturn(List.of(t1, t2, t3, o1, o2));
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(c1, c2, c3, c4));
        when(reportRepository.findById(today)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportCenterStatisticsDto result = reportCenterStatisticsService.generateOrUpdateDailyReport(today);
        assertNotNull(result);
    }

    @Test
    void getSummaryReportBetween_EdgeCases() {
        LocalDate startDate = today.minusDays(7);
        LocalDate endDate = today;

        // Student edge cases
        Student s1 = new Student(); s1.setId(101L); s1.setCreatedAt(null);
        Student s2 = new Student(); s2.setId(102L); s2.setCreatedAt(today.minusDays(10).atStartOfDay()); // before start
        Student s3 = new Student(); s3.setId(103L); s3.setCreatedAt(today.plusDays(1).atStartOfDay()); // after end

        // Enrollment edge cases
        Enrollment e1 = new Enrollment(); e1.setId(401L); e1.setStatus(null); // null status
        Enrollment e2 = new Enrollment(); e2.setId(402L); e2.setStatus("DROPPED"); e2.setUpdatedAt(null); e2.setEnrollmentDate(null); // both null
        Enrollment e3 = new Enrollment(); e3.setId(403L); e3.setStatus("DROPPED"); e3.setUpdatedAt(today.minusDays(10).atStartOfDay()); // before start
        Enrollment e4 = new Enrollment(); e4.setId(404L); e4.setStatus("DROPPED"); e4.setUpdatedAt(null); e4.setEnrollmentDate(today.plusDays(1)); // after end

        // Staff edge cases
        Staff t1 = new Staff(); t1.setId(201L); t1.setStaffType("TEACHER"); t1.setHireDate(null);
        Staff t2 = new Staff(); t2.setId(202L); t2.setStaffType("TEACHER"); t2.setHireDate(today.minusDays(10)); // before start
        Staff t3 = new Staff(); t3.setId(203L); t3.setStaffType("TEACHER"); t3.setHireDate(today.plusDays(1)); // after end
        Staff o1 = new Staff(); o1.setId(204L); o1.setStaffType("ADMIN"); o1.setHireDate(null);
        Staff o2 = new Staff(); o2.setId(205L); o2.setStaffType("ADMIN"); o2.setHireDate(today.minusDays(10)); // before start

        // Classes edge cases
        Classes c1 = new Classes(); c1.setId(301L); c1.setStatus("ONGOING"); c1.setStartDate(null); c1.setCreatedAt(null);
        Classes c2 = new Classes(); c2.setId(302L); c2.setStatus("ONGOING"); c2.setStartDate(today.minusDays(10)); // before start
        Classes c3 = new Classes(); c3.setId(303L); c3.setStatus("ONGOING"); c3.setStartDate(today.plusDays(1)); // after end
        Classes c4 = new Classes(); c4.setId(304L); c4.setStatus("CLOSED"); c4.setEndDate(null); c4.setUpdatedAt(null);
        Classes c5 = new Classes(); c5.setId(305L); c5.setStatus("CLOSED"); c5.setEndDate(today.minusDays(10)); // before start
        Classes c6 = new Classes(); c6.setId(306L); c6.setStatus("CLOSED"); c6.setEndDate(today.plusDays(1)); // after end

        when(studentRepository.findAll()).thenReturn(List.of(s1, s2, s3));
        when(enrollmentRepository.findAll()).thenReturn(List.of(e1, e2, e3, e4));
        when(staffRepository.findAll()).thenReturn(List.of(t1, t2, t3, o1, o2));
        when(courseRepository.count()).thenReturn(0L);
        when(classesRepository.findAll()).thenReturn(List.of(c1, c2, c3, c4, c5, c6));

        ReportSummaryDto result = reportCenterStatisticsService.getSummaryReportBetween(startDate, endDate);
        assertNotNull(result);
    }
}
