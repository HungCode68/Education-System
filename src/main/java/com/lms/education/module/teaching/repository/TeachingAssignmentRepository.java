package com.lms.education.module.teaching.repository;

import com.lms.education.module.teaching.entity.TeachingAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {

    boolean existsByTeacherIdAndClassesId(Long teacherId, Long classId);

    List<TeachingAssignment> findByClassesId(Long classId);

    @Query("SELECT ta FROM TeachingAssignment ta WHERE " +
           "LOWER(ta.teacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(ta.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<TeachingAssignment> searchAssignments(@Param("keyword") String keyword, Pageable pageable);
}
