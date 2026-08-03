package com.lms.education.module.audit.repository;

import com.lms.education.module.audit.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("SELECT a FROM ActivityLog a WHERE " +
            "(:keyword IS NULL OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.details) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:module IS NULL OR a.module = :module) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<ActivityLog> searchAndFilterLogs(
            @Param("keyword") String keyword,
            @Param("module") String module,
            @Param("action") String action,
            @Param("status") ActivityLog.LogStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
