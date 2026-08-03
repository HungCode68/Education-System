package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.LearningMaterialDto;
import com.lms.education.module.lms.service.LearningMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning-materials")
@RequiredArgsConstructor
public class LearningMaterialController {

    private final LearningMaterialService learningMaterialService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MATERIAL_CREATE')")
    @LogActivity(module = "LMS", action = "UPLOAD_FILE", targetType = "learning_material", description = "Tải lên tệp tài liệu")
    public ResponseEntity<Map<String, Object>> uploadFileMaterial(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "lessonId", required = false) Long lessonId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "materialType", defaultValue = "DOCUMENT") String materialType,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder,
            @RequestPart("file") MultipartFile file) {

        LearningMaterialDto created = learningMaterialService.createWithFile(courseId, lessonId, title, materialType, displayOrder, file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload tệp tài liệu thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/link")
    @PreAuthorize("hasAuthority('MATERIAL_CREATE')")
    @LogActivity(module = "LMS", action = "ADD_LINK", targetType = "learning_material", description = "Thêm liên kết tài liệu từ bên ngoài")
    public ResponseEntity<Map<String, Object>> createExternalLinkMaterial(@Valid @RequestBody LearningMaterialDto dto) {
        LearningMaterialDto created = learningMaterialService.createExternalLink(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Thêm liên kết tài liệu thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MATERIAL_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "learning_material", description = "Cập nhật tài liệu học tập")
    public ResponseEntity<Map<String, Object>> updateMaterial(
            @PathVariable Long id,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "lessonId", required = false) Long lessonId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "materialType", required = false) String materialType,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "resourceUrl", required = false) String resourceUrl,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "isOfficial", required = false) Boolean isOfficial,
            @RequestParam(value = "isRagEnabled", required = false) Boolean isRagEnabled,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        LearningMaterialDto dto = LearningMaterialDto.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .title(title)
                .materialType(materialType)
                .sourceType(sourceType)
                .resourceUrl(resourceUrl)
                .displayOrder(displayOrder)
                .isOfficial(isOfficial)
                .isRagEnabled(isRagEnabled)
                .build();

        LearningMaterialDto updated = learningMaterialService.update(id, dto, file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật tài liệu học tập thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('MATERIAL_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "learning_material", description = "Cập nhật tài liệu học tập (JSON)")
    public ResponseEntity<Map<String, Object>> updateMaterialJson(
            @PathVariable Long id,
            @RequestBody LearningMaterialDto dto) {

        LearningMaterialDto updated = learningMaterialService.update(id, dto, null);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật tài liệu học tập thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_DELETE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "learning_material", description = "Xóa tài liệu học tập")
    public ResponseEntity<Map<String, String>> deleteMaterial(@PathVariable Long id) {
        learningMaterialService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa tài liệu học tập thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<LearningMaterialDto> getMaterialById(@PathVariable Long id) {
        return ResponseEntity.ok(learningMaterialService.getById(id));
    }

    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<List<LearningMaterialDto>> getMaterialsByLessonId(@PathVariable Long lessonId) {
        return ResponseEntity.ok(learningMaterialService.getByLessonId(lessonId));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<List<LearningMaterialDto>> getMaterialsByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(learningMaterialService.getByCourseId(courseId));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<List<LearningMaterialDto>> getMaterialsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(learningMaterialService.getByClassId(classId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<Page<LearningMaterialDto>> getAllMaterials(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(learningMaterialService.getAll(keyword, pageable));
    }
}
