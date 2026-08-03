package com.lms.education.module.reporting.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.CourseRepository;
import com.lms.education.module.reporting.dto.ReportCenterStatisticsDto;
import com.lms.education.module.reporting.dto.ReportSummaryDto;
import com.lms.education.module.reporting.entity.ReportCenterStatistics;
import com.lms.education.module.reporting.repository.ReportCenterStatisticsRepository;
import com.lms.education.module.reporting.service.ReportCenterStatisticsService;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.reporting.dto.ReportClassMetricsDto;
import com.lms.education.module.reporting.dto.TrainingDashboardDto;
import com.lms.education.module.reporting.dto.TrainingOverviewDto;
import com.lms.education.module.reporting.service.ReportClassMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCenterStatisticsServiceImpl implements ReportCenterStatisticsService {

    private final ReportCenterStatisticsRepository reportRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final CourseRepository courseRepository;
    private final ClassesRepository classesRepository;
    private final ReportClassMetricsService classMetricsService;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public ReportCenterStatisticsDto generateOrUpdateDailyReport(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        log.info("Bắt đầu tính toán/đồng bộ số liệu thống kê trung tâm cho ngày: {}", targetDate);

        List<Student> allStudents = studentRepository.findAll();
        int totalActiveStudents = (int) allStudents.stream()
                .filter(s -> "STUDYING".equalsIgnoreCase(s.getStatus()))
                .count();

        int newStudentsToday = (int) allStudents.stream()
                .filter(s -> s.getCreatedAt() != null && targetDate.equals(s.getCreatedAt().toLocalDate()))
                .count();

        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        int droppedStudentsToday = (int) allEnrollments.stream()
                .filter(e -> "DROPPED".equalsIgnoreCase(e.getStatus()) &&
                        ((e.getUpdatedAt() != null && targetDate.equals(e.getUpdatedAt().toLocalDate())) ||
                         (e.getUpdatedAt() == null && e.getEnrollmentDate() != null && targetDate.equals(e.getEnrollmentDate()))))
                .count();

        List<Staff> allStaff = staffRepository.findAll();
        int totalTeachers = (int) allStaff.stream()
                .filter(s -> "TEACHER".equalsIgnoreCase(s.getStaffType()))
                .count();

        int newTeachersToday = (int) allStaff.stream()
                .filter(s -> "TEACHER".equalsIgnoreCase(s.getStaffType()) && targetDate.equals(s.getHireDate()))
                .count();

        int totalOtherStaffs = (int) allStaff.stream()
                .filter(s -> s.getStaffType() == null || !"TEACHER".equalsIgnoreCase(s.getStaffType()))
                .count();

        int newStaffsToday = (int) allStaff.stream()
                .filter(s -> (s.getStaffType() == null || !"TEACHER".equalsIgnoreCase(s.getStaffType())) && targetDate.equals(s.getHireDate()))
                .count();

        int totalCourses = (int) courseRepository.count();

        List<Classes> allClasses = classesRepository.findAll();
        int totalActiveClasses = (int) allClasses.stream()
                .filter(c -> "ONGOING".equalsIgnoreCase(c.getStatus()) || "OPENING".equalsIgnoreCase(c.getStatus()))
                .count();

        int newClassesOpened = (int) allClasses.stream()
                .filter(c -> targetDate.equals(c.getStartDate()) ||
                        (c.getStartDate() == null && c.getCreatedAt() != null && targetDate.equals(c.getCreatedAt().toLocalDate())))
                .count();

        int classesClosedToday = (int) allClasses.stream()
                .filter(c -> targetDate.equals(c.getEndDate()) ||
                        (c.getEndDate() == null && "CLOSED".equalsIgnoreCase(c.getStatus()) && c.getUpdatedAt() != null && targetDate.equals(c.getUpdatedAt().toLocalDate())))
                .count();

        ReportCenterStatistics entity = reportRepository.findById(targetDate)
                .orElse(ReportCenterStatistics.builder().reportDate(targetDate).build());

        entity.setTotalActiveStudents(totalActiveStudents);
        entity.setNewStudentsToday(newStudentsToday);
        entity.setDroppedStudentsToday(droppedStudentsToday);
        entity.setTotalTeachers(totalTeachers);
        entity.setNewTeachersToday(newTeachersToday);
        entity.setResignedTeachersToday(0);
        entity.setTotalOtherStaffs(totalOtherStaffs);
        entity.setNewStaffsToday(newStaffsToday);
        entity.setResignedStaffsToday(0);
        entity.setTotalCourses(totalCourses);
        entity.setTotalActiveClasses(totalActiveClasses);
        entity.setNewClassesOpened(newClassesOpened);
        entity.setClassesClosedToday(classesClosedToday);

        ReportCenterStatistics saved = reportRepository.save(entity);
        log.info("Đã đồng bộ/tái chốt báo cáo thành công cho ngày: {}", targetDate);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ReportCenterStatisticsDto saveCustomReportSnapshot(ReportCenterStatisticsDto dto) {
        if (dto.getReportDate() == null) {
            throw new IllegalArgumentException("Ngày thống kê không được để trống");
        }

        ReportCenterStatistics entity = reportRepository.findById(dto.getReportDate())
                .orElse(ReportCenterStatistics.builder().reportDate(dto.getReportDate()).build());

        if (dto.getTotalActiveStudents() != null) entity.setTotalActiveStudents(dto.getTotalActiveStudents());
        if (dto.getNewStudentsToday() != null) entity.setNewStudentsToday(dto.getNewStudentsToday());
        if (dto.getDroppedStudentsToday() != null) entity.setDroppedStudentsToday(dto.getDroppedStudentsToday());
        if (dto.getTotalTeachers() != null) entity.setTotalTeachers(dto.getTotalTeachers());
        if (dto.getNewTeachersToday() != null) entity.setNewTeachersToday(dto.getNewTeachersToday());
        if (dto.getResignedTeachersToday() != null) entity.setResignedTeachersToday(dto.getResignedTeachersToday());
        if (dto.getTotalOtherStaffs() != null) entity.setTotalOtherStaffs(dto.getTotalOtherStaffs());
        if (dto.getNewStaffsToday() != null) entity.setNewStaffsToday(dto.getNewStaffsToday());
        if (dto.getResignedStaffsToday() != null) entity.setResignedStaffsToday(dto.getResignedStaffsToday());
        if (dto.getTotalCourses() != null) entity.setTotalCourses(dto.getTotalCourses());
        if (dto.getTotalActiveClasses() != null) entity.setTotalActiveClasses(dto.getTotalActiveClasses());
        if (dto.getNewClassesOpened() != null) entity.setNewClassesOpened(dto.getNewClassesOpened());
        if (dto.getClassesClosedToday() != null) entity.setClassesClosedToday(dto.getClassesClosedToday());

        ReportCenterStatistics saved = reportRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ReportCenterStatisticsDto getReportByDate(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        if (LocalDate.now().equals(targetDate)) {
            return generateOrUpdateDailyReport(targetDate);
        }
        ReportCenterStatistics entity = reportRepository.findById(targetDate)
                .orElse(ReportCenterStatistics.builder().reportDate(targetDate).build());
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCenterStatisticsDto> getReportsBetween(LocalDate startDate, LocalDate endDate) {
        return reportRepository.findByReportDateBetweenOrderByReportDateAsc(startDate, endDate)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportSummaryDto getSummaryReportBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Khoảng thời gian từ ngày bắt đầu đến ngày kết thúc không hợp lệ!");
        }

        List<Student> allStudents = studentRepository.findAll();
        int totalActiveStudents = (int) allStudents.stream()
                .filter(s -> "STUDYING".equalsIgnoreCase(s.getStatus()))
                .count();

        int totalNewStudentsInRange = (int) allStudents.stream()
                .filter(s -> s.getCreatedAt() != null &&
                        !s.getCreatedAt().toLocalDate().isBefore(startDate) &&
                        !s.getCreatedAt().toLocalDate().isAfter(endDate))
                .count();

        List<Enrollment> allEnrollments = enrollmentRepository.findAll();
        int totalDroppedStudentsInRange = (int) allEnrollments.stream()
                .filter(e -> "DROPPED".equalsIgnoreCase(e.getStatus()) &&
                        ((e.getUpdatedAt() != null && !e.getUpdatedAt().toLocalDate().isBefore(startDate) && !e.getUpdatedAt().toLocalDate().isAfter(endDate)) ||
                         (e.getUpdatedAt() == null && e.getEnrollmentDate() != null && !e.getEnrollmentDate().isBefore(startDate) && !e.getEnrollmentDate().isAfter(endDate))))
                .count();

        List<Staff> allStaff = staffRepository.findAll();
        int totalTeachers = (int) allStaff.stream()
                .filter(s -> "TEACHER".equalsIgnoreCase(s.getStaffType()))
                .count();

        int totalNewTeachersInRange = (int) allStaff.stream()
                .filter(s -> "TEACHER".equalsIgnoreCase(s.getStaffType()) && s.getHireDate() != null &&
                        !s.getHireDate().isBefore(startDate) &&
                        !s.getHireDate().isAfter(endDate))
                .count();

        int totalOtherStaffs = (int) allStaff.stream()
                .filter(s -> s.getStaffType() == null || !"TEACHER".equalsIgnoreCase(s.getStaffType()))
                .count();

        int totalNewStaffsInRange = (int) allStaff.stream()
                .filter(s -> (s.getStaffType() == null || !"TEACHER".equalsIgnoreCase(s.getStaffType())) && s.getHireDate() != null &&
                        !s.getHireDate().isBefore(startDate) &&
                        !s.getHireDate().isAfter(endDate))
                .count();

        int totalCourses = (int) courseRepository.count();

        List<Classes> allClasses = classesRepository.findAll();
        int totalActiveClasses = (int) allClasses.stream()
                .filter(c -> "ONGOING".equalsIgnoreCase(c.getStatus()) || "OPENING".equalsIgnoreCase(c.getStatus()))
                .count();

        int totalNewClassesOpenedInRange = (int) allClasses.stream()
                .filter(c -> {
                    LocalDate dateToCheck = c.getStartDate() != null ? c.getStartDate() : (c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : null);
                    return dateToCheck != null && !dateToCheck.isBefore(startDate) && !dateToCheck.isAfter(endDate);
                })
                .count();

        int totalClassesClosedInRange = (int) allClasses.stream()
                .filter(c -> {
                    LocalDate dateToCheck = c.getEndDate() != null ? c.getEndDate() : (c.getUpdatedAt() != null ? c.getUpdatedAt().toLocalDate() : null);
                    return dateToCheck != null && !dateToCheck.isBefore(startDate) && !dateToCheck.isAfter(endDate) && "CLOSED".equalsIgnoreCase(c.getStatus());
                })
                .count();

        int totalDaysReported = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

        return ReportSummaryDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalActiveStudents(totalActiveStudents)
                .totalTeachers(totalTeachers)
                .totalOtherStaffs(totalOtherStaffs)
                .totalCourses(totalCourses)
                .totalActiveClasses(totalActiveClasses)
                .totalNewStudentsInRange(totalNewStudentsInRange)
                .totalDroppedStudentsInRange(totalDroppedStudentsInRange)
                .totalNewTeachersInRange(totalNewTeachersInRange)
                .totalResignedTeachersInRange(0)
                .totalNewStaffsInRange(totalNewStaffsInRange)
                .totalResignedStaffsInRange(0)
                .totalNewClassesOpenedInRange(totalNewClassesOpenedInRange)
                .totalClassesClosedInRange(totalClassesClosedInRange)
                .totalDaysReported(totalDaysReported)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCenterStatisticsDto getLatestReport() {
        ReportCenterStatistics entity = reportRepository.findTopByOrderByReportDateDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có số liệu thống kê báo cáo nào trong hệ thống!"));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingDashboardDto getTrainingDashboard(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        ReportCenterStatisticsDto centerReport = getReportByDate(targetDate);

        TrainingOverviewDto overviewDto = TrainingOverviewDto.builder()
                .reportDate(centerReport.getReportDate())
                .totalActiveStudents(centerReport.getTotalActiveStudents())
                .newStudentsToday(centerReport.getNewStudentsToday())
                .droppedStudentsToday(centerReport.getDroppedStudentsToday())
                .totalTeachers(centerReport.getTotalTeachers())
                .newTeachersToday(centerReport.getNewTeachersToday())
                .resignedTeachersToday(centerReport.getResignedTeachersToday())
                .totalCourses(centerReport.getTotalCourses())
                .totalActiveClasses(centerReport.getTotalActiveClasses())
                .newClassesOpened(centerReport.getNewClassesOpened())
                .classesClosedToday(centerReport.getClassesClosedToday())
                .build();

        List<ReportClassMetricsDto> classMetricsList = classMetricsService.getAllClassesMetrics();

        return TrainingDashboardDto.builder()
                .centerOverview(overviewDto)
                .classMetricsList(classMetricsList)
                .build();
    }

    private ReportCenterStatisticsDto toDto(ReportCenterStatistics entity) {
        return ReportCenterStatisticsDto.builder()
                .reportDate(entity.getReportDate())
                .totalActiveStudents(entity.getTotalActiveStudents())
                .newStudentsToday(entity.getNewStudentsToday())
                .droppedStudentsToday(entity.getDroppedStudentsToday())
                .totalTeachers(entity.getTotalTeachers())
                .newTeachersToday(entity.getNewTeachersToday())
                .resignedTeachersToday(entity.getResignedTeachersToday())
                .totalOtherStaffs(entity.getTotalOtherStaffs())
                .newStaffsToday(entity.getNewStaffsToday())
                .resignedStaffsToday(entity.getResignedStaffsToday())
                .totalCourses(entity.getTotalCourses())
                .totalActiveClasses(entity.getTotalActiveClasses())
                .newClassesOpened(entity.getNewClassesOpened())
                .classesClosedToday(entity.getClassesClosedToday())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
