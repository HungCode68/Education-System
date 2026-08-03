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
import com.lms.education.module.reporting.service.ReportClassMetricsService;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportClassMetricsServiceImpl implements ReportClassMetricsService {

    private final ReportClassMetricsRepository metricsRepository;
    private final ClassesRepository classesRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;

    @Override
    @Transactional
    public ReportClassMetricsDto generateOrUpdateClassMetrics(Long classId) {
        Classes classes = classesRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học có id: " + classId));

        List<Enrollment> enrollments = enrollmentRepository.findByClassesId(classId);
        int totalStudents = (int) enrollments.stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .count();

        int droppedStudents = (int) enrollments.stream()
                .filter(e -> "DROPPED".equalsIgnoreCase(e.getStatus()))
                .count();

        long totalAttendance = attendanceRepository.countTotalAttendanceByClassId(classId);
        long presentAttendance = attendanceRepository.countPresentAttendanceByClassId(classId);
        BigDecimal averageAttendanceRate = totalAttendance == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf((presentAttendance * 100.0) / totalAttendance).setScale(2, RoundingMode.HALF_UP);

        Double avgScoreVal = submissionRepository.calculateAverageScoreByClassId(classId);
        BigDecimal averageAssignmentScore = (avgScoreVal == null) ? BigDecimal.ZERO :
                BigDecimal.valueOf(avgScoreVal).setScale(2, RoundingMode.HALF_UP);

        ReportClassMetrics entity = metricsRepository.findById(classId)
                .orElse(ReportClassMetrics.builder().classId(classId).build());

        entity.setTotalStudents(totalStudents);
        entity.setAverageAttendanceRate(averageAttendanceRate);
        entity.setAverageAssignmentScore(averageAssignmentScore);
        entity.setDroppedStudents(droppedStudents);
        entity.setLastCalculatedAt(LocalDateTime.now());

        ReportClassMetrics saved = metricsRepository.save(entity);
        return toDto(saved, classes, null, null);
    }

    @Override
    @Transactional
    public List<ReportClassMetricsDto> generateOrUpdateAllClassesMetrics() {
        log.info("Bắt đầu tính toán/đồng bộ toàn bộ chỉ số KPI lớp học trong trung tâm");
        List<Classes> allClasses = classesRepository.findAll();
        List<ReportClassMetricsDto> dtoList = allClasses.stream()
                .map(cls -> generateOrUpdateClassMetrics(cls.getId()))
                .collect(Collectors.toList());
        log.info("Đã cập nhật chỉ số KPI thành công cho {} lớp học", dtoList.size());
        return dtoList;
    }

    @Override
    @Transactional
    public ReportClassMetricsDto getClassMetrics(Long classId) {
        Classes classes = classesRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học có id: " + classId));

        ReportClassMetrics entity = metricsRepository.findById(classId)
                .orElseGet(() -> {
                    // Tự động tính toán nếu chưa có bản ghi nào
                    generateOrUpdateClassMetrics(classId);
                    return metricsRepository.findById(classId)
                            .orElse(ReportClassMetrics.builder().classId(classId).build());
                });

        return toDto(entity, classes, null, null);
    }

    @Override
    @Transactional
    public List<ReportClassMetricsDto> getAllClassesMetrics() {
        return classesRepository.findAll().stream()
                .map(cls -> getClassMetrics(cls.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportClassMetricsDto getClassMetricsInRange(Long classId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Khoảng thời gian từ ngày bắt đầu đến ngày kết thúc không hợp lệ!");
        }

        Classes classes = classesRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học có id: " + classId));

        List<Enrollment> enrollments = enrollmentRepository.findByClassesId(classId);
        int totalStudents = (int) enrollments.stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
                .count();

        int droppedStudents = (int) enrollments.stream()
                .filter(e -> "DROPPED".equalsIgnoreCase(e.getStatus()))
                .count();

        long totalAttendance = attendanceRepository.countTotalAttendanceByClassIdInRange(classId, startDate, endDate);
        long presentAttendance = attendanceRepository.countPresentAttendanceByClassIdInRange(classId, startDate, endDate);
        BigDecimal averageAttendanceRate = totalAttendance == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf((presentAttendance * 100.0) / totalAttendance).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.atTime(23, 59, 59);
        Double avgScoreVal = submissionRepository.calculateAverageScoreByClassIdInRange(classId, startDt, endDt);
        BigDecimal averageAssignmentScore = (avgScoreVal == null) ? BigDecimal.ZERO :
                BigDecimal.valueOf(avgScoreVal).setScale(2, RoundingMode.HALF_UP);

        return ReportClassMetricsDto.builder()
                .classId(classId)
                .classCode(classes.getCode())
                .className(classes.getName())
                .totalStudents(totalStudents)
                .averageAttendanceRate(averageAttendanceRate)
                .averageAssignmentScore(averageAssignmentScore)
                .droppedStudents(droppedStudents)
                .lastCalculatedAt(LocalDateTime.now())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportClassMetricsDto> getMyClassesMetrics() {
        User currentUser = resolveUser(null);
        if (currentUser == null) {
            throw new ResourceNotFoundException("Bạn cần đăng nhập để xem báo cáo lớp học được phân công!");
        }

        Long userId = currentUser.getId();
        Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

        Set<Long> myClassIds = new HashSet<>();
        myClassIds.addAll(scheduleAssignmentRepository.findClassIdsByTeacher(userId, staffId));
        myClassIds.addAll(teachingSubstitutionRepository.findClassIdsBySubstituteTeacher(userId, staffId));

        if (myClassIds.isEmpty()) {
            return Collections.emptyList();
        }

        return myClassIds.stream()
                .map(this::getClassMetrics)
                .collect(Collectors.toList());
    }

    private User resolveUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private ReportClassMetricsDto toDto(ReportClassMetrics entity, Classes classes, LocalDate startDate, LocalDate endDate) {
        return ReportClassMetricsDto.builder()
                .classId(entity.getClassId())
                .classCode(classes != null ? classes.getCode() : null)
                .className(classes != null ? classes.getName() : null)
                .totalStudents(entity.getTotalStudents())
                .averageAttendanceRate(entity.getAverageAttendanceRate())
                .averageAssignmentScore(entity.getAverageAssignmentScore())
                .droppedStudents(entity.getDroppedStudents())
                .lastCalculatedAt(entity.getLastCalculatedAt())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
