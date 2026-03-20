package com.lms.education.module.assignments.service;

import com.lms.education.module.assignments.dto.AssignmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AssignmentService {
    AssignmentDto create(AssignmentDto dto, MultipartFile file, String userId);

    AssignmentDto update(String id, AssignmentDto dto, MultipartFile file, String userId);

    void delete(String id, String userId);

    AssignmentDto getById(String id, String userId);

    // Dành cho học sinh/giáo viên xem bài tập trong 1 lớp
    Page<AssignmentDto> getAssignmentsByClass(String classId, Pageable pageable, String userId);

    // Dành cho giáo viên xem lại các bài tập mình đã tạo
    Page<AssignmentDto> getAssignmentsByCreator(String userId, Pageable pageable);
}
