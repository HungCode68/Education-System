package com.lms.education.module.teaching_assignment.repository;

import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, String> {

    // =================================================================
    // CÁC HÀM KIỂM TRA RÀNG BUỘC (QUAN TRỌNG NHẤT)
    // =================================================================

    // Kiểm tra xem Môn này ở Lớp này trong Học kỳ này đã có ai dạy chưa?
    // Dùng để chặn khi Admin phân công trùng (Logic 1 Lớp - 1 Môn - 1 GV)
    boolean existsByPhysicalClassIdAndSubjectIdAndSemesterIdAndStatus(
            String physicalClassId,
            String subjectId,
            String semesterId,
            TeachingAssignment.AssignmentStatus status
    );

    // Tìm bản ghi phân công hiện tại (để Update hoặc Xóa)
    Optional<TeachingAssignment> findByPhysicalClassIdAndSubjectIdAndSemesterIdAndStatus(
            String physicalClassId,
            String subjectId,
            String semesterId,
            TeachingAssignment.AssignmentStatus status
    );

    // =================================================================
    // CÁC HÀM TRA CỨU & HIỂN THỊ (CHO UI/TOOL)
    // =================================================================

    // Xem phân công của MỘT LỚP
    // Dùng cho màn hình "Chi tiết lớp học" -> Tab "Giáo viên bộ môn"
    @Query("SELECT ta FROM TeachingAssignment ta " +
            "JOIN FETCH ta.subject s " +
            "JOIN FETCH ta.teacher t " +
            "WHERE ta.physicalClass.id = :classId " +
            "AND ta.semester.id = :semesterId " +
            "AND ta.status = 'active' " +
            "ORDER BY s.name ASC") // Sắp xếp theo tên môn
    List<TeachingAssignment> findAllByClassAndSemester(
            @Param("classId") String classId,
            @Param("semesterId") String semesterId
    );

    // Xem phân công của MỘT GIÁO VIÊN (Thầy A đang dạy những lớp nào?)
    // Dùng để Admin kiểm tra "bằng mắt" xem thầy có bị quá tải không trước khi gán thêm
    @Query("SELECT ta FROM TeachingAssignment ta " +
            "JOIN FETCH ta.physicalClass pc " +
            "JOIN FETCH ta.subject s " +
            "WHERE ta.teacher.id = :teacherId " +
            "AND ta.semester.id = :semesterId " +
            "AND ta.status = 'active' " +
            "ORDER BY pc.grade.level ASC, pc.name ASC") // Sắp xếp theo Khối -> Tên lớp
    List<TeachingAssignment> findAllByTeacherAndSemester(
            @Param("teacherId") String teacherId,
            @Param("semesterId") String semesterId
    );

    // Tìm kiếm nâng cao (Cho màn hình "Quản lý phân công" tổng quát)
    // Admin lọc theo: Năm học, Học kỳ, Khối, Tên GV, Tên Lớp
    @Query("SELECT ta FROM TeachingAssignment ta " +
            "JOIN FETCH ta.physicalClass pc " +
            "JOIN FETCH ta.subject s " +
            "JOIN FETCH ta.teacher t " +
            "WHERE (:schoolYearId IS NULL OR ta.schoolYear.id = :schoolYearId) " +
            "AND (:semesterId IS NULL OR ta.semester.id = :semesterId) " +
            "AND (:gradeId IS NULL OR pc.grade.id = :gradeId) " +
            "AND (:classId IS NULL OR pc.id = :classId) " +
            "AND (:teacherId IS NULL OR t.id = :teacherId) " +
            "AND ta.status = 'active'")
    Page<TeachingAssignment> search(
            String schoolYearId,
            String semesterId,
            String gradeId,
            String classId,
            String teacherId,
            Pageable pageable
    );

    // =================================================================
    // CÁC HÀM HỖ TRỢ LOGIC "TOOL"
    // =================================================================

    // Đếm số lớp mà giáo viên này đang dạy trong học kỳ
    // Giúp hiển thị con số đơn giản: "Thầy A đang dạy 5 lớp"
    long countByTeacherIdAndSemesterIdAndStatus(
            String teacherId,
            String semesterId,
            TeachingAssignment.AssignmentStatus status
    );

    // Kiểm tra xem học kỳ này đã có phân công nào chưa?
    boolean existsBySemesterId(String semesterId);

    // Lấy danh sách ID các môn đã được phân công trong lớp (Để tìm ra môn còn thiếu)
    @Query("SELECT ta.subject.id FROM TeachingAssignment ta " +
            "WHERE ta.physicalClass.id = :classId " +
            "AND ta.semester.id = :semesterId " +
            "AND ta.status = 'active'")
    List<String> findAssignedSubjectIds(
            @Param("classId") String classId,
            @Param("semesterId") String semesterId
    );
}
