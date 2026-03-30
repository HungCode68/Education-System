package com.lms.education.module.assignments.repository;

import com.lms.education.module.assignments.entity.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, String> {

    // Lấy danh sách các file đính kèm của một bài nộp, sắp xếp theo thời gian upload
    List<SubmissionAttachment> findBySubmissionIdOrderByCreatedAtAsc(String submissionId);

    // Xóa toàn bộ file đính kèm của một bài nộp
    // (Dùng khi học sinh muốn xóa bản nháp và làm lại từ đầu)
    @Modifying
    @Query("DELETE FROM SubmissionAttachment sa WHERE sa.submission.id = :submissionId")
    void deleteBySubmissionId(@Param("submissionId") String submissionId);
}