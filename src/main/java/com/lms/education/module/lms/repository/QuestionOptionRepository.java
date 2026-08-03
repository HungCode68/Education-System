package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionIdOrderByIdAsc(Long questionId);

    void deleteByQuestionId(Long questionId);
}
