package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {

    // Lấy toàn bộ log của một User cụ thể (Ví dụ: Để xem một học sinh dạo này làm gì)
    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Tìm kiếm và Lọc Log tổng hợp (Dành cho màn hình Quản trị viên)
    @Query("SELECT a FROM ActivityLog a WHERE " +
            "(:keyword IS NULL OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:module IS NULL OR a.module = :module) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<ActivityLog> searchAndFilterLogs(
            @Param("keyword") String keyword,
            @Param("module") String module,
            @Param("action") String action,
            @Param("status") ActivityLog.LogStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
