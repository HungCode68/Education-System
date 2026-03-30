package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.SubmissionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionAnswerRepository extends JpaRepository<SubmissionAnswer, String> {

    // Lấy toàn bộ chi tiết câu trả lời của 1 bài nộp
    @Query("SELECT sa FROM SubmissionAnswer sa WHERE sa.submission.id = :submissionId ORDER BY sa.question.questionOrder ASC")
    List<SubmissionAnswer> findBySubmissionIdOrderByQuestionOrderAsc(@Param("submissionId") String submissionId);

    //  Tìm 1 câu trả lời cụ thể trong 1 bài nộp
    // (Dùng để Update/Lưu nháp từng câu riêng lẻ khi học sinh đang click chọn đáp án trong lúc thi)
    Optional<SubmissionAnswer> findBySubmissionIdAndQuestionId(String submissionId, String questionId);

    // Xóa sạch câu trả lời của 1 bài nộp
    @Modifying
    @Query("DELETE FROM SubmissionAnswer sa WHERE sa.submission.id = :submissionId")
    void deleteBySubmissionId(@Param("submissionId") String submissionId);
}