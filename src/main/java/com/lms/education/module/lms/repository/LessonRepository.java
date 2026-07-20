package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByClassesIdOrderByOrderNumberAsc(Long classId);

    boolean existsByClassesIdAndOrderNumber(Long classId, Integer orderNumber);

    @Query("SELECT l FROM Lesson l WHERE " +
           "LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.classes.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Lesson> searchLessons(@Param("keyword") String keyword, Pageable pageable);
}
