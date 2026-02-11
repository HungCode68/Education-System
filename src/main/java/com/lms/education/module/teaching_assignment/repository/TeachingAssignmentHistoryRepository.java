package com.lms.education.module.teaching_assignment.repository;

import com.lms.education.module.teaching_assignment.entity.TeachingAssignmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingAssignmentHistoryRepository extends JpaRepository<TeachingAssignmentHistory, String> {


    // Xem lịch sử của một Phân công cụ thể (Assignment ID)
    // Dùng khi click vào nút "Lịch sử" trên dòng phân công môn Toán 10A1
    List<TeachingAssignmentHistory> findAllByAssignmentIdOrderByChangedAtDesc(String assignmentId);

    // Xem lịch sử biến động của một LỚP HỌC (Physical Class)
    // VD: Hiệu trưởng muốn xem "Lớp 10A1 học kỳ này có thay đổi giáo viên nhiều không?"
    @Query("SELECT h FROM TeachingAssignmentHistory h " +
            "JOIN FETCH h.assignment a " +
            "JOIN FETCH a.subject s " +
            "LEFT JOIN FETCH h.oldTeacher ot " +
            "LEFT JOIN FETCH h.newTeacher nt " +
            "WHERE a.physicalClass.id = :classId " +
            "ORDER BY h.changedAt DESC")
    List<TeachingAssignmentHistory> findAllByClassId(@Param("classId") String classId);

    // Xem lịch sử biến động của một GIÁO VIÊN (Teacher)
    // Tìm cả lúc họ là người cũ (bị thay) hoặc người mới (được gán)
    @Query("SELECT h FROM TeachingAssignmentHistory h " +
            "LEFT JOIN FETCH h.assignment a " +
            "LEFT JOIN FETCH a.physicalClass pc " +
            "WHERE h.oldTeacher.id = :teacherId " +
            "OR h.newTeacher.id = :teacherId " +
            "ORDER BY h.changedAt DESC")
    List<TeachingAssignmentHistory> findAllByTeacherId(@Param("teacherId") String teacherId);

    // Tìm kiếm trong log: Theo tên lớp, tên môn, tên giáo viên cũ/mới
    @Query("SELECT h FROM TeachingAssignmentHistory h " +
            "JOIN h.assignment a " +
            "JOIN a.physicalClass pc " +
            "JOIN a.subject s " +
            "LEFT JOIN h.oldTeacher ot " +
            "LEFT JOIN h.newTeacher nt " +
            "WHERE (:keyword IS NULL OR " +
            "   LOWER(pc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(ot.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(nt.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY h.changedAt DESC")
    Page<TeachingAssignmentHistory> search(@Param("keyword") String keyword, Pageable pageable);
}
