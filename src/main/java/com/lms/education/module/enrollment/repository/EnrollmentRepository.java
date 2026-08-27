package com.lms.education.module.enrollment.repository;

import com.lms.education.module.enrollment.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndClassesId(Long studentId, Long classId);

    boolean existsByStudentIdAndClassesCourseIdAndStatus(Long studentId, Long courseId, String status);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByClassesId(Long classId);

    @Query("SELECT e FROM Enrollment e WHERE " +
           "LOWER(e.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.classes.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Enrollment> searchEnrollments(@Param("keyword") String keyword, Pageable pageable);
}
