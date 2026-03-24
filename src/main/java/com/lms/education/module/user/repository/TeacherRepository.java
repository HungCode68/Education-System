package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, String> {
    Optional<Teacher> findByTeacherCode(String teacherCode);
    boolean existsByTeacherCode(String teacherCode);
    Optional<Teacher> findByUser_Email(String email);
    boolean existsByDepartmentId(String departmentId);

    // TÌM MÃ GIÁO VIÊN LỚN NHẤT THEO TIỀN TỐ (VD: GV26)
    @Query("SELECT MAX(t.teacherCode) FROM Teacher t WHERE t.teacherCode LIKE CONCAT(:prefix, '%')")
    String findMaxTeacherCodeByPrefix(@Param("prefix") String prefix);

    //  TÌM KIẾM VÀ LỌC GIÁO VIÊN (Theo Keyword, Status, và DepartmentId)
    @Query("SELECT t FROM Teacher t WHERE " +
            "(:keyword IS NULL OR LOWER(t.teacherCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:departmentId IS NULL OR t.department.id = :departmentId)")
    Page<Teacher> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") Teacher.Status status,
            @Param("departmentId") String departmentId,
            Pageable pageable);

    // hàm này để hỗ trợ tìm Giáo viên theo ID tài khoản
    Optional<Teacher> findByUserId(String userId);
}
