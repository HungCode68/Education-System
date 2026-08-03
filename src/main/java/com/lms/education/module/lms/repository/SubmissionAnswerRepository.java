package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, Long> {

    List<SubmissionAnswer> findBySubmissionId(Long submissionId);

    Optional<SubmissionAnswer> findBySubmissionIdAndQuestionId(Long submissionId, Long questionId);

    void deleteBySubmissionId(Long submissionId);

    boolean existsBySubmissionIdAndQuestionId(Long submissionId, Long questionId);
}
