package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, String> {

    // Kiểm tra tồn tại cơ bản (Trong cùng 1 lớp)
    boolean existsByPhysicalClassIdAndStudentId(String physicalClassId, String studentId);

    // Kiểm tra xem lớp có bất kỳ dữ liệu học sinh nào không?
    boolean existsByPhysicalClassId(String physicalClassId);

    // Kiểm tra nghiệp vụ: Học sinh này đã có lớp nào trong Năm học này chưa?
    // (Dùng để chặn khi xếp lớp tự động: Một HS không thể học 2 lớp cùng lúc)
    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END " +
            "FROM ClassStudent cs " +
            "JOIN cs.physicalClass pc " +
            "WHERE cs.student.id = :studentId " +
            "AND pc.schoolYear.id = :schoolYearId " +
            "AND cs.status = 'studying'")
    boolean isStudentEnrolledInYear(String studentId, String schoolYearId);

    // Lấy danh sách học sinh của một lớp (Sắp xếp theo STT hoặc Tên)
    // Dùng để hiển thị danh sách lớp
    @Query("SELECT cs FROM ClassStudent cs " +
            "JOIN FETCH cs.student s " +
            "WHERE cs.physicalClass.id = :classId " +
            "ORDER BY cs.studentNumber ASC, s.fullName ASC")
    List<ClassStudent> findByPhysicalClassId(String classId);

    // Đếm sĩ số hiện tại của một lớp
    // QUAN TRỌNG CHO TOOL PHÂN LỚP: Để thuật toán biết lớp này đầy chưa
    long countByPhysicalClassIdAndStatus(String classId, ClassStudent.StudentStatus status);

    // Tìm số thứ tự (STT) lớn nhất hiện có trong lớp
    // Dùng để auto-increment STT khi thêm học sinh mới vào
    @Query("SELECT MAX(cs.studentNumber) FROM ClassStudent cs WHERE cs.physicalClass.id = :classId")
    Integer findMaxStudentNumber(String classId);

    // Tìm lớp học hiện tại của học sinh trong một năm học cụ thể
    // Dùng để hiển thị profile học sinh hoặc logic chuyển lớp
    @Query("SELECT cs FROM ClassStudent cs " +
            "JOIN cs.physicalClass pc " +
            "WHERE cs.student.id = :studentId " +
            "AND pc.schoolYear.id = :schoolYearId " +
            "AND cs.status = 'studying'")
    Optional<ClassStudent> findCurrentClassOfStudent(String studentId, String schoolYearId);

    // Lấy danh sách học sinh ĐỦ ĐIỀU KIỆN lên lớp (Dùng cho Auto Promote)
    // Ví dụ: Lấy tất cả học sinh lớp 10A1 năm ngoái có trạng thái 'completed'
    List<ClassStudent> findByPhysicalClassIdAndStatus(String physicalClassId, ClassStudent.StudentStatus status);

    // Kiểm tra danh sách học sinh (Bulk Check)
    // Dùng khi xếp 1 lúc 50 học sinh, xem những em nào đã có lớp rồi để loại ra
    @Query("SELECT cs.student.id FROM ClassStudent cs " +
            "JOIN cs.physicalClass pc " +
            "WHERE pc.schoolYear.id = :schoolYearId " +
            "AND cs.student.id IN :studentIds " +
            "AND cs.status = 'studying'")
    List<String> findEnrolledStudentIdsInYear(String schoolYearId, List<String> studentIds);


    // Lấy danh sách học sinh có hỗ trợ lọc theo trạng thái
    @Query("SELECT cs FROM ClassStudent cs " +
            "JOIN FETCH cs.student s " +
            "WHERE cs.physicalClass.id = :classId " +
            "AND (:status IS NULL OR cs.status = :status) " +
            "ORDER BY cs.studentNumber ASC, s.fullName ASC") // Sắp xếp theo STT rồi đến Tên
    List<ClassStudent> findByPhysicalClassIdAndStatusOption(
            String classId,
            ClassStudent.StudentStatus status
    );
}
