package com.lms.education.module.learning_material.repository;

import com.lms.education.module.learning_material.entity.LearningMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, String> {


    // GIÁO VIÊN / QUẢN TRỊ
    // Lấy tất cả tài liệu của một lớp (Bao gồm cả bản nháp - unpublished)
    @Query("SELECT lm FROM LearningMaterial lm " +
            "JOIN FETCH lm.uploadedBy u " + // Lấy luôn thông tin người tải lên
            "WHERE lm.onlineClass.id = :classId " +
            "ORDER BY lm.createdAt DESC")
    List<LearningMaterial> findAllByOnlineClassId(@Param("classId") String classId);


    // HỌC SINH
    // Chỉ lấy những tài liệu đã xuất bản (published)
    @Query("SELECT lm FROM LearningMaterial lm " +
            "JOIN FETCH lm.uploadedBy u " +
            "WHERE lm.onlineClass.id = :classId " +
            "AND lm.status = :status " +
            "ORDER BY lm.createdAt DESC")
    List<LearningMaterial> findAllByOnlineClassIdAndStatus(
            @Param("classId") String classId,
            @Param("status") LearningMaterial.MaterialStatus status
    );

    // CHI TIẾT & TÌM KIẾM
    // Lấy chi tiết một tài liệu
    @Query("SELECT lm FROM LearningMaterial lm " +
            "JOIN FETCH lm.uploadedBy u " +
            "JOIN FETCH lm.onlineClass oc " +
            "WHERE lm.id = :id")
    Optional<LearningMaterial> findByIdWithDetails(@Param("id") String id);

    // Tìm kiếm tài liệu trong lớp (Theo tiêu đề hoặc tên file) - Có phân trang
    @Query("SELECT lm FROM LearningMaterial lm " +
            "JOIN FETCH lm.uploadedBy u " +
            "WHERE lm.onlineClass.id = :classId " +
            "AND (:keyword IS NULL OR " +
            "   LOWER(lm.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(lm.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY lm.createdAt DESC")
    Page<LearningMaterial> searchMaterialsByClass(
            @Param("classId") String classId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // THỐNG KÊ (Dùng cho Dashboard)
    // Đếm tổng số tài liệu trong một lớp
    long countByOnlineClassId(String classId);
}
