package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.AssignmentQuestion;
import com.lms.education.module.lms.entity.AssignmentQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentQuestionRepository extends JpaRepository<AssignmentQuestion, AssignmentQuestionId> {

    List<AssignmentQuestion> findByAssignmentIdOrderByOrderNumberAsc(Long assignmentId);

    Optional<AssignmentQuestion> findByAssignmentIdAndQuestionId(Long assignmentId, Long questionId);

    void deleteByAssignmentId(Long assignmentId);

    void deleteByAssignmentIdAndQuestionId(Long assignmentId, Long questionId);

    boolean existsByAssignmentIdAndQuestionId(Long assignmentId, Long questionId);

    List<AssignmentQuestion> findByQuestionId(Long questionId);
}
