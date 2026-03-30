package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.AssignmentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, String> {

    // TÌM BÀI NỘP CỦA HỌC SINH
    // Dùng để kiểm tra xem học sinh đã có bản ghi nháp/nộp bài cho bài tập này chưa
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(String assignmentId, String studentId);

    boolean existsByAssignmentIdAndStudentId(String assignmentId, String studentId);

    // DÀNH CHO HỌC SINH: Xem toàn bộ lịch sử bài tập của mình
    Page<AssignmentSubmission> findByStudentIdOrderByUpdatedAtDesc(String studentId, Pageable pageable);

    // DÀNH CHO GIÁO VIÊN: Xem danh sách nộp bài của 1 bài tập (Có tìm kiếm theo tên/mã HS và lọc theo trạng thái)
    @Query("SELECT sub FROM AssignmentSubmission sub " +
            "WHERE sub.assignment.id = :assignmentId " +
            "AND (:keyword IS NULL OR " +
            "     LOWER(sub.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(sub.student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR sub.submissionStatus = :status)")
    Page<AssignmentSubmission> searchSubmissionsByAssignment(
            @Param("assignmentId") String assignmentId,
            @Param("keyword") String keyword,
            @Param("status") AssignmentSubmission.SubmissionStatus status,
            Pageable pageable
    );

    // THỐNG KÊ NHANH: Dành cho màn hình Dashboard của Giáo viên
    // (Ví dụ: Hiển thị "Đã chấm: 15/40", "Nộp muộn: 3")
    long countByAssignmentIdAndSubmissionStatus(String assignmentId, AssignmentSubmission.SubmissionStatus status);
}
