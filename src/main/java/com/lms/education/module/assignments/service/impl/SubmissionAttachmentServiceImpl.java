package com.lms.education.module.assignments.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.assignments.dto.SubmissionAttachmentDto;
import com.lms.education.module.assignments.entity.AssignmentSubmission;
import com.lms.education.module.assignments.entity.SubmissionAttachment;
import com.lms.education.module.assignments.repository.AssignmentSubmissionRepository;
import com.lms.education.module.assignments.repository.SubmissionAttachmentRepository;
import com.lms.education.module.assignments.service.SubmissionAttachmentService;
import com.lms.education.module.learning_material.service.MinioStorageService; // Import Service MinIO bạn đã viết
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionAttachmentServiceImpl implements SubmissionAttachmentService {

    private final SubmissionAttachmentRepository attachmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional
    public SubmissionAttachmentDto uploadAttachment(String submissionId, MultipartFile file, String userId) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi nộp bài!"));

        // Kiểm tra quyền: Chỉ chủ nhân của bài nộp mới được upload file
        validateStudentOwnership(submission, userId);

        // Logic chặn upload nếu bài đã nộp chính thức
        if (submission.getSubmissionStatus() != AssignmentSubmission.SubmissionStatus.draft &&
                submission.getSubmissionStatus() != AssignmentSubmission.SubmissionStatus.not_submitted) {
            throw new OperationNotPermittedException("Bài tập này đã được nộp chính thức. Bạn không thể tải thêm file đính kèm!");
        }

        // Gọi MinioStorageService để lưu file vật lý lên server
        String objectName = minioStorageService.uploadFile(file);

        // Phân loại định dạng file (Dựa vào content-type hoặc đuôi file)
        SubmissionAttachment.FileType fileType = determineFileType(file.getContentType());

        // Lưu thông tin vào Database
        SubmissionAttachment attachment = SubmissionAttachment.builder()
                .submission(submission)
                .fileName(file.getOriginalFilename())
                .filePath(objectName) // Lưu tên object trả về từ MinIO
                .fileType(fileType)
                .fileSize(file.getSize())
                .build();

        SubmissionAttachment savedAttachment = attachmentRepository.save(attachment);
        log.info("Học sinh đã upload file {} cho bài nộp {}", savedAttachment.getFileName(), submissionId);

        return mapToDto(savedAttachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionAttachmentDto> getAttachmentsBySubmissionId(String submissionId) {
        List<SubmissionAttachment> attachments = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId);

        return attachments.stream().map(attachment -> {
            SubmissionAttachmentDto dto = mapToDto(attachment);

            //  Tự động sinh ra Presigned URL từ MinIO có thời hạn 2 tiếng để user xem/tải file
            String downloadUrl = minioStorageService.getFileUrl(attachment.getFilePath());
            dto.setFileUrl(downloadUrl);

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAttachment(String attachmentId, String userId) {
        SubmissionAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file đính kèm!"));

        AssignmentSubmission submission = attachment.getSubmission();

        // Kiểm tra quyền sở hữu
        validateStudentOwnership(submission, userId);

        // Chặn xóa nếu bài đã nộp
        if (submission.getSubmissionStatus() != AssignmentSubmission.SubmissionStatus.draft &&
                submission.getSubmissionStatus() != AssignmentSubmission.SubmissionStatus.not_submitted) {
            throw new OperationNotPermittedException("Bài tập này đã được nộp chính thức. Bạn không thể xóa file đính kèm!");
        }

        // Phải xóa file vật lý trên MinIO TRƯỚC!
        minioStorageService.deleteFile(attachment.getFilePath());

        //  Sau đó mới xóa dòng dữ liệu trong Database
        attachmentRepository.delete(attachment);
        log.info("Đã xóa hoàn toàn file đính kèm ID: {} khỏi Database và MinIO", attachmentId);
    }

    // HÀM HELPER

    private void validateStudentOwnership(AssignmentSubmission submission, String userId) {
        String studentUserId = submission.getStudent().getUser().getId();
        if (!studentUserId.equals(userId)) {
            throw new OperationNotPermittedException("Bạn không có quyền can thiệp vào bài nộp của học sinh khác!");
        }
    }

    private SubmissionAttachment.FileType determineFileType(String contentType) {
        if (contentType == null) return SubmissionAttachment.FileType.other;

        if (contentType.startsWith("image/")) return SubmissionAttachment.FileType.image;
        if (contentType.startsWith("video/")) return SubmissionAttachment.FileType.video;
        if (contentType.startsWith("audio/")) return SubmissionAttachment.FileType.audio;

        if (contentType.contains("pdf") || contentType.contains("msword") ||
                contentType.contains("officedocument.wordprocessingml")) {
            return SubmissionAttachment.FileType.document;
        }

        if (contentType.contains("zip") || contentType.contains("rar") || contentType.contains("tar")) {
            return SubmissionAttachment.FileType.compressed;
        }

        return SubmissionAttachment.FileType.other;
    }

    private SubmissionAttachmentDto mapToDto(SubmissionAttachment entity) {
        return SubmissionAttachmentDto.builder()
                .id(entity.getId())
                .submissionId(entity.getSubmission().getId())
                .fileName(entity.getFileName())
                .filePath(entity.getFilePath())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .createdAt(entity.getCreatedAt())
                // Lưu ý: fileUrl sẽ được set riêng trong hàm getAttachmentsBySubmissionId
                .build();
    }
}