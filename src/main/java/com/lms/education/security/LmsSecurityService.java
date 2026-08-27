package com.lms.education.security;

import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service("lmsSecurity")
@RequiredArgsConstructor
public class LmsSecurityService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final org.springframework.context.ApplicationContext applicationContext;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean canAccessClass(Long classId) {
        if (classId == null) return false;
        User user = getCurrentUser();
        if (user == null) return false;

        Long userId = user.getId();
        Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);
        Long studentId = studentRepository.findByUserId(userId).map(Student::getId).orElse(null);

        // Check Teacher/Staff
        if (staffId != null || userId != null) {
            List<Long> scheduleClassIds = scheduleAssignmentRepository.findClassIdsByTeacher(userId, staffId);
            if (scheduleClassIds.contains(classId)) {
                return true;
            }

            if (staffId != null) {
                boolean isTeaching = teachingAssignmentRepository.findByTeacherId(staffId).stream()
                        .anyMatch(ta -> ta.getClasses() != null && ta.getClasses().getId().equals(classId));
                if (isTeaching) return true;
            }
        }

        // Check Student
        if (studentId != null) {
            boolean isEnrolled = enrollmentRepository.findByStudentId(studentId).stream()
                    .anyMatch(enrollment -> enrollment.getClasses() != null 
                            && enrollment.getClasses().getId().equals(classId)
                            && !"DROPPED".equalsIgnoreCase(enrollment.getStatus()));
            if (isEnrolled) return true;
        }

        return false;
    }

    @Transactional(readOnly = true)
    public boolean canManageClass(Long classId) {
        if (classId == null) return false;
        User user = getCurrentUser();
        if (user == null) return false;

        Long userId = user.getId();
        Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);

        // Chỉ giảng viên được phân công mới có quyền MANAGE
        if (staffId != null) {
            List<Long> scheduleClassIds = scheduleAssignmentRepository.findClassIdsByTeacher(userId, staffId);
            if (scheduleClassIds.contains(classId)) {
                return true;
            }
            boolean isTeaching = teachingAssignmentRepository.findByTeacherId(staffId).stream()
                    .anyMatch(ta -> ta.getClasses() != null && ta.getClasses().getId().equals(classId));
            if (isTeaching) return true;
        }

        return false;
    }
}
