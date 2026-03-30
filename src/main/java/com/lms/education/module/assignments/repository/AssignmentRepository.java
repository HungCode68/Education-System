package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {

    // Dành cho Học sinh/Giáo viên: Lấy toàn bộ bài tập của một lớp cụ thể
    Page<Assignment> findByOnlineClassId(String onlineClassId, Pageable pageable);

    // Dành cho Giáo viên: Lấy toàn bộ bài tập do chính giáo viên đó tạo
    Page<Assignment> findByCreatedById(String createdById, Pageable pageable);

    // Tìm các bài tập của lớp đang ở trạng thái PUBLISHED (Đã giao)
    Page<Assignment> findByOnlineClassIdAndStatus(String onlineClassId, Assignment.AssignmentStatus status, Pageable pageable);

    // Lấy các bài tập vừa hết hạn trong vòng 1 phút qua
    @Query("SELECT a FROM Assignment a WHERE a.dueTime >= :startTime AND a.dueTime <= :endTime")
    List<Assignment> findAssignmentsDueInTimeframe(
            @Param("startTime") java.time.LocalDateTime startTime,
            @Param("endTime") java.time.LocalDateTime endTime
    );
}
