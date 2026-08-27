package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByAssignmentId(Long assignmentId);
    boolean existsByAssignmentIdAndStudentIdAndStatus(Long assignmentId, Long studentId, String status);

    Optional<Submission> findTopByAssignmentIdAndStudentIdOrderByStartTimeDesc(Long assignmentId, Long studentId);

    List<Submission> findByAssignmentIdAndStudentIdOrderByStartTimeDesc(Long assignmentId, Long studentId);

    long countByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    List<Submission> findTop5ByStudentIdOrderBySubmittedAtDesc(Long studentId);

    Page<Submission> findByAssignmentId(Long assignmentId, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.assignment.id = :assignmentId AND " +
           "(:status IS NULL OR :status = '' OR UPPER(s.status) = UPPER(:status)) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(s.student.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Submission> findByAssignmentIdWithFilters(@Param("assignmentId") Long assignmentId, 
                                                   @Param("status") String status, 
                                                   @Param("keyword") String keyword, 
                                                   Pageable pageable);

    @Modifying
    @Query(value = "INSERT INTO submissions (assignment_id, student_id, score, status, submitted_at, updated_at) " +
                   "SELECT a.id, cs.student_id, 0, 'MISSING', NOW(), NOW() " +
                   "FROM assignments a " +
                   "JOIN lessons l ON a.lesson_id = l.id " +
                   "JOIN enrollments cs ON l.class_id = cs.class_id AND cs.status = 'ACTIVE' " +
                   "WHERE a.due_date < NOW() " +
                   "AND a.status = 'PUBLISHED' " +
                   "AND NOT EXISTS ( " +
                   "    SELECT 1 FROM submissions s WHERE s.assignment_id = a.id AND s.student_id = cs.student_id " +
                   ")", nativeQuery = true)
    int insertMissingSubmissions();

    @Query("SELECT AVG(s.score) FROM Submission s WHERE s.assignment.lesson.classes.id = :classId AND s.score IS NOT NULL")
    Double calculateAverageScoreByClassId(@Param("classId") Long classId);

    @Query("SELECT AVG(s.score) FROM Submission s WHERE s.assignment.lesson.classes.id = :classId AND s.score IS NOT NULL AND s.submittedAt >= :startDateTime AND s.submittedAt <= :endDateTime")
    Double calculateAverageScoreByClassIdInRange(@Param("classId") Long classId, @Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);
}
