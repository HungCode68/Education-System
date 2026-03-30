package com.lms.education.module.notification.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.notification.dto.NotificationDto;
import com.lms.education.module.notification.entity.Notification;
import com.lms.education.module.notification.repository.NotificationRepository;
import com.lms.education.module.notification.service.NotificationService;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.TeacherRepository; // Dùng để móc tên thật
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional
    public void sendNotification(String userId, String senderId, String title, String message,
                                 Notification.NotificationType type, String relatedType, String relatedId, String metadata) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người nhận thông báo!"));

        // Sender có thể null nếu là thông báo do Hệ thống tự phát ra
        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }

        Notification notification = Notification.builder()
                .user(user)
                .sender(sender)
                .title(title)
                .message(message)
                .type(type)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .metadata(metadata)
                // readAt mặc định là null (Chưa đọc)
                .build();

        notificationRepository.save(notification);
        log.info("Hệ thống đã bắn thông báo loại [{}] tới User [{}]", type, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo!"));

        // Chỉ chủ nhân mới được quyền đánh dấu đọc cái thông báo của họ
        if (!notification.getUser().getId().equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền thao tác trên thông báo này!");
        }

        // Chỉ update nếu thông báo này thực sự chưa được đọc
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("User {} đã đánh dấu ĐÃ ĐỌC cho toàn bộ thông báo", userId);
    }

    // HÀM HELPER: MAP ENTITY TO DTO

    private NotificationDto mapToDto(Notification entity) {
        NotificationDto dto = NotificationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .relatedType(entity.getRelatedType())
                .relatedId(entity.getRelatedId())
                .metadata(entity.getMetadata())

                // Frontend chỉ cần true/false để biết vẽ dấu chấm đỏ hay không
                .isRead(entity.getReadAt() != null)

                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();

        // Tự động móc tên thật của người gửi
        if (entity.getSender() != null) {
            String senderUserId = entity.getSender().getId();
            dto.setSenderId(senderUserId);

            // Dùng TeacherRepository tìm xem tài khoản này có phải là Giáo viên không
            teacherRepository.findByUserId(senderUserId).ifPresentOrElse(
                    teacher -> dto.setSenderName(teacher.getFullName()),
                    () -> dto.setSenderName("Hệ thống") // Nếu không tìm thấy hồ sơ GV, gán là Hệ thống
            );
        } else {
            dto.setSenderName("Hệ thống"); // Nếu Sender == null
        }

        return dto;
    }
}
