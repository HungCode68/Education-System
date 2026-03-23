package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.AssignmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentQuestionRepository extends JpaRepository<AssignmentQuestion, String> {

    // Lấy toàn bộ câu hỏi của 1 bài tập, sắp xếp theo thứ tự (question_order) tăng dần
    List<AssignmentQuestion> findByAssignmentIdOrderByQuestionOrderAsc(String assignmentId);

    // Đếm số lượng câu hỏi hiện tại của 1 bài tập (Dùng để tự động tính question_order cho câu tiếp theo)
    long countByAssignmentId(String assignmentId);

    // Xóa toàn bộ câu hỏi của 1 bài tập (Rất hữu ích khi giáo viên upload file Excel mới và muốn ghi đè toàn bộ đề cũ)
    @Modifying
    @Query("DELETE FROM AssignmentQuestion aq WHERE aq.assignment.id = :assignmentId")
    void deleteByAssignmentId(@Param("assignmentId") String assignmentId);
}
