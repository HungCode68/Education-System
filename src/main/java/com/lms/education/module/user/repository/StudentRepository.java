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
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByStudentCode(String studentCode);
    boolean existsByStudentCode(String studentCode);
    Optional<Student> findByUser_Email(String email);
    Optional<Student> findByUserId(String userId);

    @Query("SELECT s FROM Student s WHERE " +
            "(:keyword IS NULL OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:admissionYear IS NULL OR s.admissionYear = :admissionYear)")
    Page<Student> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") Student.Status status,
            @Param("admissionYear") Integer admissionYear,
            Pageable pageable);

    // Tìm mã học sinh lớn nhất theo tiền tố (VD: tiền tố là "HS26")
    @Query("SELECT MAX(s.studentCode) FROM Student s WHERE s.studentCode LIKE CONCAT(:prefix, '%')")
    String findMaxStudentCodeByPrefix(@Param("prefix") String prefix);
}
