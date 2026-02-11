package com.lms.education.module.lms_class.repository;

import com.lms.education.module.lms_class.entity.OnlineClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineClassRepository extends JpaRepository<OnlineClass, String> {
    // Tìm lớp online dựa trên ID phân công (Để check xem đã tạo chưa)
    Optional<OnlineClass> findByTeachingAssignmentId(String teachingAssignmentId);

    // Lấy danh sách lớp Online mà giáo viên này đang dạy
    // Query đi qua bảng TeachingAssignment để lấy Teacher ID
    @Query("SELECT oc FROM OnlineClass oc " +
            "WHERE oc.teachingAssignment.teacher.id = :teacherId " +
            "AND oc.status = 'active' " +
            "ORDER BY oc.createdAt DESC")
    List<OnlineClass> findAllByTeacherId(@Param("teacherId") String teacherId);

    // Lấy danh sách lớp Online của một lớp vật lý (VD: Xem tất cả lớp online của 10A1)
    @Query("SELECT oc FROM OnlineClass oc " +
            "WHERE oc.teachingAssignment.physicalClass.id = :classId " +
            "AND oc.status = 'active'")
    List<OnlineClass> findAllByPhysicalClassId(@Param("classId") String classId);

    // Tìm kiếm nâng cao cho Admin
    @Query("SELECT oc FROM OnlineClass oc " +
            "JOIN oc.teachingAssignment ta " +
            "JOIN ta.physicalClass pc " +
            "JOIN ta.subject s " +
            "JOIN ta.teacher t " +
            "WHERE (:keyword IS NULL OR " +
            "    LOWER(oc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(pc.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR oc.status = :status)")
    Page<OnlineClass> search(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    // Lấy danh sách lớp cho Giáo viên (bao gồm cả lớp chính và lớp dạy thay)
    @Query("SELECT oc FROM OnlineClass oc " +
            "JOIN oc.teachingAssignment ta " +
            "LEFT JOIN TeachingSubstitution ts ON ts.originalAssignment = ta " + // Join bảng dạy thay
            "WHERE oc.status = 'active' " +
            "AND (" +
            "   ta.teacher.id = :teacherId " + // Trường hợp 1: Là giáo viên chính
            "   OR " +
            "   (ts.subTeacher.id = :teacherId " + // Trường hợp 2: Là giáo viên dạy thay
            "    AND ts.status = 'approved' " +
            "    AND :today BETWEEN ts.startDate AND ts.endDate) " + // Và phải đang trong thời gian dạy
            ") " +
            "ORDER BY oc.createdAt DESC")
    List<OnlineClass> findAllClassesForTeacher(
            @Param("teacherId") String teacherId,
            @Param("today") LocalDate today
    );
}
