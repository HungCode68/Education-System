package com.lms.education.module.assignments.service.scheduler;

import com.lms.education.module.assignments.entity.Assignment;
import com.lms.education.module.assignments.entity.AssignmentSubmission;
import com.lms.education.module.assignments.repository.AssignmentRepository;
import com.lms.education.module.assignments.repository.AssignmentSubmissionRepository;
import com.lms.education.module.lms_class.entity.OnlineClassStudent;
import com.lms.education.module.lms_class.repository.OnlineClassStudentRepository;
import com.lms.education.module.notification.entity.Notification;
import com.lms.education.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentDeadlineScheduler {

    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final OnlineClassStudentRepository onlineClassStudentRepository;
    private final AssignmentSubmissionRepository submissionRepository;


    // THÔNG BÁO KHI BÀI TẬP VỪA HẾT HẠN (CHẠY MỖI PHÚT)
    @Scheduled(cron = "0 * * * * *")
    public void checkAndNotifyDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);

        List<Assignment> dueAssignments = assignmentRepository.findAssignmentsDueInTimeframe(oneMinuteAgo, now);

        for (Assignment assignment : dueAssignments) {

            // Chỉ thông báo cho Học sinh trong lớp
            if (assignment.getOnlineClass() != null) {
                List<OnlineClassStudent> students = onlineClassStudentRepository.findAllByOnlineClassIdAndStatus(
                        assignment.getOnlineClass().getId(),
                        OnlineClassStudent.StudentStatus.active
                );

                for (OnlineClassStudent ocs : students) {
                    String studentUserId = ocs.getStudent().getUser().getId();

                    // Gửi thông báo đến tài khoản của từng học sinh
                    notificationService.sendNotification(
                            studentUserId,
                            null, // Hệ thống tự gửi nên sender = null
                            "Bài tập đã đóng!",
                            "Thời gian nộp bài cho '" + assignment.getTitle() + "' đã kết thúc.",
                            Notification.NotificationType.system,
                            "assignments",
                            assignment.getId(),
                            null
                    );
                }
                // Cập nhật lại log: Chỉ báo đã gửi cho Học sinh
                log.info("Đã gửi thông báo ĐÓNG bài tập [{}] cho {} Học sinh", assignment.getId(), students.size());
            }
        }
    }


    // NHẮC NHỞ HỌC SINH CHƯA NỘP BÀI TRƯỚC 24 GIỜ
    @Scheduled(cron = "0 * * * * *")
    public void notifyUpcomingDeadlines24h() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.plusHours(24);
        LocalDateTime targetTimeMinusOneMinute = targetTime.minusMinutes(1);

        // Tìm các bài tập sẽ hết hạn trong đúng 24h tới
        List<Assignment> upcomingAssignments = assignmentRepository.findAssignmentsDueInTimeframe(targetTimeMinusOneMinute, targetTime);

        for (Assignment assignment : upcomingAssignments) {
            if (assignment.getOnlineClass() != null) {
                // Lấy toàn bộ học sinh trong lớp
                List<OnlineClassStudent> students = onlineClassStudentRepository.findAllByOnlineClassIdAndStatus(
                        assignment.getOnlineClass().getId(),
                        OnlineClassStudent.StudentStatus.active
                );

                int reminderCount = 0;

                for (OnlineClassStudent ocs : students) {
                    String studentId = ocs.getStudent().getId();
                    String studentUserId = ocs.getStudent().getUser().getId();

                    // Kiểm tra xem học sinh này ĐÃ NỘP BÀI chưa
                    Optional<AssignmentSubmission> submissionOpt = submissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), studentId);

                    boolean isSubmitted = submissionOpt.isPresent() &&
                            (submissionOpt.get().getSubmissionStatus() == AssignmentSubmission.SubmissionStatus.submitted ||
                                    submissionOpt.get().getSubmissionStatus() == AssignmentSubmission.SubmissionStatus.graded ||
                                    submissionOpt.get().getSubmissionStatus() == AssignmentSubmission.SubmissionStatus.late);

                    // CHỈ GỬI THÔNG BÁO NẾU CHƯA NỘP BÀI HOẶC MỚI CHỈ LƯU NHÁP
                    if (!isSubmitted) {
                        notificationService.sendNotification(
                                studentUserId,
                                null,
                                "Nhắc nhở: Sắp đến hạn nộp bài!",
                                "Bài tập '" + assignment.getTitle() + "' sẽ hết hạn sau 24 giờ nữa. Đừng quên hoàn thành và nộp bài nhé!",
                                Notification.NotificationType.system,
                                "assignments",
                                assignment.getId(),
                                null
                        );
                        reminderCount++;
                    }
                }
                log.info("Đã gửi nhắc nhở 24H cho {} học sinh chưa nộp bài của bài tập [{}]", reminderCount, assignment.getId());
            }
        }
    }
}