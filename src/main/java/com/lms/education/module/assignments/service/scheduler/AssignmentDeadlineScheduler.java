package com.lms.education.module.assignments.service.scheduler;

import com.lms.education.module.assignments.entity.Assignment;
import com.lms.education.module.assignments.repository.AssignmentRepository;
import com.lms.education.module.notification.entity.Notification;
import com.lms.education.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentDeadlineScheduler {

    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    // Cấu hình Cron: "0 * * * * *" nghĩa là cứ đúng giây thứ 0 của mỗi phút sẽ chạy hàm này 1 lần
    @Scheduled(cron = "0 * * * * *")
    public void checkAndNotifyDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        // Lùi lại 1 phút trước để quét
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);

        // Tìm tất cả các bài tập có hạn nộp rớt vào đúng 1 phút vừa rồi
        List<Assignment> dueAssignments = assignmentRepository.findAssignmentsDueInTimeframe(oneMinuteAgo, now);

        for (Assignment assignment : dueAssignments) {
            // Kiểm tra xem bài tập có ghi nhận người tạo (Giáo viên) không
            if (assignment.getCreatedBy() != null) {
                String teacherUserId = assignment.getCreatedBy().getId();

                notificationService.sendNotification(
                        teacherUserId,                  // Người nhận là Giáo viên tạo bài
                        null,                           // Người gửi: null (Hệ thống tự gửi)
                        "Bài tập đã đến hạn nộp!",
                        "Bài tập '" + assignment.getTitle() + "' vừa kết thúc thời gian nộp bài. Bạn có thể bắt đầu chấm điểm.",
                        Notification.NotificationType.system, // Loại thông báo hệ thống
                        "assignments",                  // Trỏ về trang chi tiết bài tập đó
                        assignment.getId(),
                        null
                );
                log.info("Đã gửi thông báo hết hạn bài tập [{}] cho Giáo viên [{}]", assignment.getId(), teacherUserId);
            }
        }
    }
}
