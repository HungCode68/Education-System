package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.PhysicalClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhysicalClassRepository extends JpaRepository<PhysicalClass, String> {

    // Kiểm tra trùng tên lớp (Trong cùng 1 năm học)
    boolean existsByNameAndSchoolYearId(String name, String schoolYearId);

    // Tìm lớp theo tên và năm
    Optional<PhysicalClass> findByNameAndSchoolYearId(String name, String schoolYearId);

    // Kiểm tra trùng GVCN
    boolean existsByHomeroomTeacherIdAndSchoolYearId(String teacherId, String schoolYearId);

    // Lấy danh sách lớp để hiển thị Dropdown (Lọc theo Năm và Khối)
    List<PhysicalClass> findBySchoolYearIdAndGradeId(String schoolYearId, String gradeId);

    // Tìm lớp chủ nhiệm của một giáo viên trong năm cụ thể
    // Dùng cho màn hình "Lớp chủ nhiệm của tôi" phía Giáo viên
    Optional<PhysicalClass> findBySchoolYearIdAndHomeroomTeacherId(String schoolYearId, String teacherId);

    // Tìm kiếm & Phân trang nâng cao
    // Cho phép lọc theo: Năm học (bắt buộc hoặc tùy chọn), Khối, và tìm theo Tên lớp
    @Query("SELECT c FROM PhysicalClass c " +
            "WHERE (:schoolYearId IS NULL OR c.schoolYear.id = :schoolYearId) " +
            "AND (:gradeId IS NULL OR c.grade.id = :gradeId) " +
            "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY c.grade.level ASC, c.name ASC") // Sắp xếp theo Khối trước (10->11), rồi đến tên lớp (A->Z)
    Page<PhysicalClass> search(String schoolYearId, String gradeId, String keyword, Pageable pageable);

    // Hàm lấy toàn bộ lớp của một năm học cũ
    List<PhysicalClass> findBySchoolYearId(String schoolYearId);

    // 7. (Optional) Đếm số lượng học sinh hiện tại trong lớp (Nếu bạn chưa làm bảng class_students thì bỏ qua)
    @Query("SELECT COUNT(cs) FROM ClassStudent cs WHERE cs.physicalClass.id = :classId AND cs.status = 'studying'")
    long countStudentsByClassId(@Param("classId") String classId);
}