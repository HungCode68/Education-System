package com.lms.education.module.learning_material.controller;

import com.lms.education.module.learning_material.dto.LearningMaterialDto;
import com.lms.education.module.learning_material.entity.LearningMaterial;
import com.lms.education.module.learning_material.service.LearningMaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/learning-materials")
@RequiredArgsConstructor
@Slf4j
public class LearningMaterialController {

    private final LearningMaterialService materialService;

    // UPLOAD VÀ THÊM TÀI LIỆU
    // Upload File (Slide, Video, Document)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MATERIAL_UPLOAD')")
    public ResponseEntity<LearningMaterialDto> uploadMaterial(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("data") LearningMaterialDto dto,
            Principal principal // Lấy user đang đăng nhập từ Spring Security
    ) {
        // Giả sử Principal.getName() trả về ID của User.
        // Nếu hệ thống của bạn trả về Username, bạn cần lấy ID từ DB hoặc CustomUserDetails nhé.
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String uploaderId = principal.getName();

        // IN LOG RA ĐỂ KIỂM TRA
        log.info("====> UPLOADER ID LẤY TỪ TOKEN: {}", uploaderId);

        log.info("Request upload file {} cho lớp {}", file.getOriginalFilename(), dto.getOnlineClassId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.uploadMaterial(file, dto, uploaderId));
    }

    // Thêm Link bên ngoài (Youtube, Google Drive)
    @PostMapping("/link")
    @PreAuthorize("hasAuthority('MATERIAL_UPLOAD')")
    public ResponseEntity<LearningMaterialDto> addLinkMaterial(
            @Valid @RequestBody LearningMaterialDto dto,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String uploaderId = principal.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.addLinkMaterial(dto, uploaderId));
    }

    // LẤY DANH SÁCH TÀI LIỆU (THEO QUYỀN)
    // Góc nhìn Giáo viên (Xem tất cả kể cả bản nháp)
    @GetMapping("/teacher/class/{classId}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<List<LearningMaterialDto>> getMaterialsForTeacher(@PathVariable String classId, Principal principal)
    {
        String username = principal.getName();
        return ResponseEntity.ok(materialService.getMaterialsForTeacher(classId, username));
    }

    // Góc nhìn Học sinh (Chỉ xem tài liệu đã Published)
    @GetMapping("/student/class/{classId}")
    @PreAuthorize("hasAuthority('MATERIAL_VIEW')")
    public ResponseEntity<List<LearningMaterialDto>> getMaterialsForStudent(@PathVariable String classId, Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(materialService.getMaterialsForStudent(classId, username));
    }

    // CẬP NHẬT & XÓA
    // Thay đổi trạng thái (Publish / Unpublish)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MATERIAL_PUBLISH')")
    public ResponseEntity<Map<String, String>> changeStatus(
            @PathVariable String id,
            @RequestParam String status,
            Principal principal
    ) {
        try {
            LearningMaterial.MaterialStatus statusEnum = LearningMaterial.MaterialStatus.valueOf(status);
            materialService.changeStatus(id, statusEnum, principal.getName()); // TRUYỀN USERNAME
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật trạng thái tài liệu thành: " + status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái không hợp lệ. Chỉ nhận 'published' hoặc 'unpublished'"));
        }
    }

    // Xóa tài liệu
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_DELETE')")
    public ResponseEntity<Map<String, String>> deleteMaterial(
            @PathVariable String id,
            Principal principal // BỔ SUNG
    ) {
        materialService.deleteMaterial(id, principal.getName()); // TRUYỀN USERNAME
        return ResponseEntity.ok(Map.of("message", "Đã xóa tài liệu thành công"));
    }

    // Khi người dùng ấn vào 1 file, Frontend gọi API này để lấy link tải thật
    @GetMapping("/{id}/download-url")
    @PreAuthorize("hasAuthority('MATERIAL_DOWLOAD')")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable String id, Principal principal) {
        String username = principal.getName();
        String url = materialService.getMaterialDownloadUrl(id, username);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
