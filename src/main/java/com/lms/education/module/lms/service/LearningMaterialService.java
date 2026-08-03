package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.LearningMaterialDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LearningMaterialService {

    LearningMaterialDto createWithFile(Long courseId, Long lessonId, String title, String materialType, Integer displayOrder, MultipartFile file);

    LearningMaterialDto createExternalLink(LearningMaterialDto dto);

    LearningMaterialDto update(Long id, LearningMaterialDto dto, MultipartFile file);

    void delete(Long id);

    LearningMaterialDto getById(Long id);

    List<LearningMaterialDto> getByLessonId(Long lessonId);

    List<LearningMaterialDto> getByCourseId(Long courseId);

    List<LearningMaterialDto> getByClassId(Long classId);

    Page<LearningMaterialDto> getAll(String keyword, Pageable pageable);
}
