package com.lms.education.module.notification.repository;

import com.lms.education.module.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    // Lấy danh sách thông báo của 1 User (Có phân trang, cái nào mới nhất thì lên đầu)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Đếm số lượng thông báo CHƯA ĐỌC của 1 User (Dùng để hiển thị số đỏ trên biểu tượng chuông)
    long countByUserIdAndReadAtIsNull(String userId);

    // Đánh dấu TẤT CẢ thông báo của 1 User là đã đọc (Khi người dùng bấm nút "Mark all as read")
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.readAt IS NULL")
    void markAllAsReadByUserId(@Param("userId") String userId);
}
