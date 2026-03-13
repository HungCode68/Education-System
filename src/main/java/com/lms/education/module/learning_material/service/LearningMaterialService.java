package com.lms.education.module.learning_material.service;

import com.lms.education.module.learning_material.dto.LearningMaterialDto;
import com.lms.education.module.learning_material.entity.LearningMaterial;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LearningMaterialService {

    // Upload file thực tế (Slide, Video, Document)
    LearningMaterialDto uploadMaterial(MultipartFile file, LearningMaterialDto dto, String uploaderId);

    // Thêm link ngoài (Youtube, Drive) - Không cần file
    LearningMaterialDto addLinkMaterial(LearningMaterialDto dto, String uploaderId);

    // Lấy danh sách cho Giáo viên (Thấy tất cả)
    List<LearningMaterialDto> getMaterialsForTeacher(String classId, String username);

    // Lấy danh sách cho Học sinh (Chỉ thấy Published)
    List<LearningMaterialDto> getMaterialsForStudent(String classId, String username);

    // Xóa tài liệu
    void deleteMaterial(String materialId, String username);

    // Đổi trạng thái (Publish / Unpublish)
    void changeStatus(String materialId, LearningMaterial.MaterialStatus status, String username);

    // Hàm lấy Presigned URL khi người dùng thực sự muốn xem/tải
    String getMaterialDownloadUrl(String materialId, String username);
}
