package com.lms.education.module.lms.job;

import com.lms.education.module.lms.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissingSubmissionJob {

    private final SubmissionRepository submissionRepository;

    @Scheduled(fixedRate = 600000) // Runs every 10 minutes (600,000 ms)
    @Transactional
    public void processMissingSubmissions() {
        log.info("Bắt đầu tiến trình quét và ghi nhận các học viên chưa nộp bài...");
        try {
            int insertedCount = submissionRepository.insertMissingSubmissions();
            if (insertedCount > 0) {
                log.info("Đã ghi nhận thành công {} bản ghi CHƯA NỘP cho các bài tập quá hạn.", insertedCount);
            } else {
                log.info("Không có học viên nào bị trễ hạn nộp bài trong lần quét này.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi chạy tiến trình quét học viên chưa nộp bài: {}", e.getMessage(), e);
        }
    }
}
