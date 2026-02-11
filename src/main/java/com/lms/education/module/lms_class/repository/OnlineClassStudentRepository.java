package com.lms.education.module.lms_class.repository;

import com.lms.education.module.lms_class.entity.OnlineClassStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineClassStudentRepository extends JpaRepository<OnlineClassStudent, String> {

    // =================================================================
    // NGHIỆP VỤ: DANH SÁCH THÀNH VIÊN TRONG LỚP
    // =================================================================

    // Lấy danh sách học sinh trong một lớp Online (Kèm thông tin chi tiết Student)
    // Dùng cho màn hình "Danh sách lớp" của Giáo viên
    @Query("SELECT ocs FROM OnlineClassStudent ocs " +
            "JOIN FETCH ocs.student s " +
            "WHERE ocs.onlineClass.id = :onlineClassId " +
            "ORDER BY s.fullName ASC") // Sắp xếp theo tên
    List<OnlineClassStudent> findAllByOnlineClassId(@Param("onlineClassId") String onlineClassId);

    // Lọc theo trạng thái (VD: Chỉ lấy học sinh đang Active, bỏ qua Removed)
    @Query("SELECT ocs FROM OnlineClassStudent ocs " +
            "JOIN FETCH ocs.student s " +
            "WHERE ocs.onlineClass.id = :onlineClassId " +
            "AND ocs.status = :status " +
            "ORDER BY s.fullName ASC")
    List<OnlineClassStudent> findAllByOnlineClassIdAndStatus(
            @Param("onlineClassId") String onlineClassId,
            @Param("status") OnlineClassStudent.StudentStatus status
    );

    // =================================================================
    // NGHIỆP VỤ: GÓC NHÌN HỌC SINH (MY COURSES)
    // =================================================================

    // Lấy danh sách các lớp Online mà học sinh này đang tham gia
    // Dùng cho màn hình Dashboard của Học sinh
    @Query("SELECT ocs FROM OnlineClassStudent ocs " +
            "JOIN FETCH ocs.onlineClass oc " +
            "JOIN FETCH oc.teachingAssignment ta " + // Load luôn thông tin môn học/GV
            "JOIN FETCH ta.subject " +
            "JOIN FETCH ta.teacher " +
            "WHERE ocs.student.id = :studentId " +
            "AND ocs.status = 'active' " + // Chỉ lấy lớp đang học
            "ORDER BY oc.createdAt DESC")
    List<OnlineClassStudent> findAllByStudentId(@Param("studentId") String studentId);

    // =================================================================
    // NGHIỆP VỤ: KIỂM TRA & VALIDATION (CHO AUTO-SYNC)
    // =================================================================

    // Kiểm tra xem học sinh đã có trong lớp này chưa? (Tránh add trùng)
    boolean existsByOnlineClassIdAndStudentId(String onlineClassId, String studentId);

    // Tìm bản ghi cụ thể (để update trạng thái active/removed)
    Optional<OnlineClassStudent> findByOnlineClassIdAndStudentId(String onlineClassId, String studentId);

    // Đếm số lượng học sinh trong lớp
    long countByOnlineClassIdAndStatus(String onlineClassId, OnlineClassStudent.StudentStatus status);
}
