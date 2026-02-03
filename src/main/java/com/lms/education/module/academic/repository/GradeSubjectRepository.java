package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.GradeSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeSubjectRepository extends JpaRepository<GradeSubject, String> {

    // Kiểm tra trùng lặp
    // Một môn học không thể được gán 2 lần vào cùng 1 khối
    boolean existsByGradeIdAndSubjectId(String gradeId, String subjectId);

    // Tìm bản ghi cụ thể theo cặp ID (Dùng khi update hoặc delete mềm)
    Optional<GradeSubject> findByGradeIdAndSubjectId(String gradeId, String subjectId);

    // Lấy chương trình học của một Khối
    // Tự động sắp xếp theo Display Order (Toán -> Văn -> Anh...)
    List<GradeSubject> findByGradeIdOrderByDisplayOrderAsc(String gradeId);

    // Lấy chương trình học cho phía (Học sinh/Giáo viên)
    // Chỉ lấy những môn được bật LMS (isLmsEnabled = true) và sắp xếp đúng thứ tự
    List<GradeSubject> findByGradeIdAndIsLmsEnabledOrderByDisplayOrderAsc(String gradeId, Boolean isLmsEnabled);

    @Query("SELECT gs FROM GradeSubject gs " +
            "JOIN gs.subject s " +
            "JOIN gs.grade g " +
            "WHERE g.id = :gradeId " +
            "AND s.isActive = true " + // Chỉ lấy môn đang hoạt động
            "AND (:isLmsEnabled IS NULL OR gs.isLmsEnabled = :isLmsEnabled) " +
            "ORDER BY gs.displayOrder ASC")
    List<GradeSubject> findByGradeIdAndStatus(String gradeId, Boolean isLmsEnabled);

    // Tìm kiếm & Phân trang nâng cao (Admin)
    @Query("SELECT gs FROM GradeSubject gs " +
            "JOIN gs.grade g " +
            "JOIN gs.subject s " +
            "WHERE (:gradeId IS NULL OR g.id = :gradeId) " +
            "AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            // "AND s.isActive = true " + // <--- Bỏ comment dòng này nếu muốn ẩn luôn khỏi trang Admin
            "ORDER BY g.level ASC, gs.displayOrder ASC")
    Page<GradeSubject> search(String gradeId, String keyword, Pageable pageable);
}
