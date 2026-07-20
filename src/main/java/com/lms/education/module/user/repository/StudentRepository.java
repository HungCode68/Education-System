package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Kiểm tra xem User này đã gắn với học viên nào chưa
    boolean existsByUserId(Long userId);

    // Tìm kiếm chính xác theo mã học viên
    Optional<Student> findByStudentCode(String studentCode);

    // Tìm kiếm hồ sơ học viên dựa theo User ID
    Optional<Student> findByUserId(Long userId);

    // Đếm số lượng học viên theo năm để sinh mã tự động (Ví dụ: 'HV26%')
    @Query("SELECT COUNT(s) FROM Student s WHERE s.studentCode LIKE CONCAT(:prefix, :year, '%')")
    long countByStudentCodePattern(@Param("prefix") String prefix, @Param("year") String year);

    // Hỗ trợ ô Search: Tìm theo Tên, Mã học viên hoặc Số điện thoại phụ huynh
    @Query("SELECT s FROM Student s WHERE " +
            "LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "s.parentPhone LIKE CONCAT('%', :keyword, '%')")
    Page<Student> searchStudents(@Param("keyword") String keyword, Pageable pageable);
}