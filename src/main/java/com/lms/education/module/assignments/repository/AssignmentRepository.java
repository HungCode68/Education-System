package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {

    // Dành cho Học sinh/Giáo viên: Lấy toàn bộ bài tập của một lớp cụ thể
    Page<Assignment> findByOnlineClassId(String onlineClassId, Pageable pageable);

    // Dành cho Giáo viên: Lấy toàn bộ bài tập do chính giáo viên đó tạo
    Page<Assignment> findByCreatedById(String createdById, Pageable pageable);

    // Tìm các bài tập của lớp đang ở trạng thái PUBLISHED (Đã giao)
    Page<Assignment> findByOnlineClassIdAndStatus(String onlineClassId, Assignment.AssignmentStatus status, Pageable pageable);
}
