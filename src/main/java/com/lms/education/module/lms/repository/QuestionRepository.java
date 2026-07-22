package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuestionType(String questionType);

    Page<Question> findByQuestionTypeIgnoreCase(String questionType, Pageable pageable);

    @Query(value = "SELECT q.* FROM questions q " +
                   "INNER JOIN assignment_questions aq ON q.id = aq.question_id " +
                   "WHERE aq.assignment_id = :assignmentId " +
                   "ORDER BY aq.order_number ASC", nativeQuery = true)
    List<Question> findByAssignmentId(@Param("assignmentId") Long assignmentId);

    @Query("SELECT q FROM Question q WHERE " +
           "LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(q.readingPassage IS NOT NULL AND LOWER(q.readingPassage) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
           "UPPER(q.questionType) = UPPER(:questionType) AND (" +
           "LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(q.readingPassage IS NOT NULL AND LOWER(q.readingPassage) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Question> findByKeywordAndType(@Param("keyword") String keyword,
                                       @Param("questionType") String questionType,
                                       Pageable pageable);
}
