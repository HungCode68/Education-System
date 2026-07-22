package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByLessonIdOrderByDueDateAsc(Long lessonId);

    List<Assignment> findByLessonClassesIdOrderByDueDateAsc(Long classId);

    @Query("SELECT a FROM Assignment a WHERE " +
           "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.lesson.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.lesson.classes.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.lesson.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Assignment> searchAssignments(@Param("keyword") String keyword, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Assignment a SET a.status = 'CLOSED' WHERE a.dueDate IS NOT NULL AND a.dueDate < :now AND UPPER(a.status) = 'PUBLISHED'")
    int closeExpiredAssignments(@Param("now") java.time.LocalDateTime now);
}
