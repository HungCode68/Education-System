package com.lms.education.module.lms.repository;

import com.lms.education.module.lms.entity.LearningMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {

    List<LearningMaterial> findByLessonIdOrderByDisplayOrderAsc(Long lessonId);

    List<LearningMaterial> findByLessonClassesIdOrderByDisplayOrderAsc(Long classId);

    @Query("SELECT lm FROM LearningMaterial lm WHERE " +
           "LOWER(lm.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(lm.lesson.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(lm.lesson.classes.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(lm.lesson.classes.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LearningMaterial> searchMaterials(@Param("keyword") String keyword, Pageable pageable);
}
