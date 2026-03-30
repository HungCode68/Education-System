package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, String> {

    //  Lấy toàn bộ đáp án của một câu hỏi cụ thể, sắp xếp theo thứ tự hiển thị (A, B, C, D...)
    List<QuestionOption> findByQuestionIdOrderByDisplayOrderAsc(String questionId);

    //  Kiểm tra xem câu hỏi này đã có đáp án đúng nào được đánh dấu chưa
    // (Rất hữu ích để validate: Nếu là câu hỏi Single Choice thì chỉ được phép có 1 đáp án đúng)
    boolean existsByQuestionIdAndIsCorrectTrue(String questionId);

    // Đếm số lượng đáp án hiện tại của một câu hỏi
    int countByQuestionId(String questionId);

    // Xóa toàn bộ đáp án của một câu hỏi
    @Modifying
    @Query("DELETE FROM QuestionOption qo WHERE qo.question.id = :questionId")
    void deleteByQuestionId(@Param("questionId") String questionId);
}
