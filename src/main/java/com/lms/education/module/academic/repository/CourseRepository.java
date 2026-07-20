package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Kiểm tra trùng lặp mã khóa học
    boolean existsByCode(String code);

    // Tìm kiếm chính xác theo mã khóa học
    Optional<Course> findByCode(String code);

    // Hỗ trợ tìm kiếm theo cả Tên và Mã khóa học
    @Query("SELECT c FROM Course c WHERE " +
            "LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Course> searchCourses(@Param("keyword") String keyword, Pageable pageable);
}