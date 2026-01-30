package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.SchoolYear;
import com.lms.education.module.academic.entity.SchoolYear.SchoolYearStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolYearRepository extends JpaRepository<SchoolYear, String> {

    // Kiểm tra tên tồn tại (Dùng khi validate Create/Update để tránh trùng lặp)
    boolean existsByName(String name);

    // Tìm tất cả năm học theo trạng thái (VD: Lấy list các năm đang Active)
    List<SchoolYear> findByStatus(SchoolYearStatus status);

    // Tìm năm học chứa một ngày cụ thể (Rất quan trọng)
    // Nghiệp vụ: "Hôm nay là ngày 20/11/2025, cho tôi biết nó thuộc năm học nào?"
    @Query("SELECT s FROM SchoolYear s WHERE :date BETWEEN s.startDate AND s.endDate")
    Optional<SchoolYear> findCurrentSchoolYearByDate(LocalDate date);

    // Tìm năm học mới nhất (Sắp xếp theo ngày bắt đầu giảm dần)
    Optional<SchoolYear> findFirstByOrderByStartDateDesc();

    // Kiểm tra thời gian chồng chéo (Nâng cao)
    // Nghiệp vụ: Không cho phép tạo năm học mới mà thời gian lại dẫm đạp lên năm học cũ.
    @Query("SELECT COUNT(s) > 0 FROM SchoolYear s " +
            "WHERE (:startDate <= s.endDate AND :endDate >= s.startDate) " +
            "AND (:excludeId IS NULL OR s.id != :excludeId)")
    boolean existsOverlappingYear(LocalDate startDate, LocalDate endDate, String excludeId);

    // Chức năng tìm kiếm
    @Query("SELECT s FROM SchoolYear s WHERE " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR s.status = :status)")
    // Bỏ ORDER BY ở đây vì Pageable sẽ tự xử lý sort
    Page<SchoolYear> searchAndFilter(String keyword, SchoolYearStatus status, Pageable pageable);

    // ToDO: khi có bảng announcements và notification thì bổ sung chức năng thông báo việc năm học kết thúc
}