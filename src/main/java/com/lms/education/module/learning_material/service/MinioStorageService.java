package com.lms.education.module.learning_material.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:lms-local}")
    private String bucketName;

    // Upload file lên MinIO và trả về tên file đã lưu
    public String uploadFile(MultipartFile file) {
        try {
            // Tạo tên file độc nhất để tránh trùng lặp
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";
            String objectName = UUID.randomUUID().toString() + extension;

            InputStream inputStream = file.getInputStream();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Đã upload thành công file {} lên MinIO bucket {}", objectName, bucketName);
            return objectName;

        } catch (Exception e) {
            log.error("Lỗi khi upload file lên MinIO", e);
            throw new RuntimeException("Lỗi hệ thống khi lưu trữ file: " + e.getMessage());
        }
    }

    // Lấy URL để tải/xem file
    public String getFileUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(2, TimeUnit.HOURS) // URL chỉ có tác dụng trong 2 giờ (Bảo mật cho LMS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy URL file từ MinIO", e);
            return null;
        }
    }

    // Xóa file khỏi MinIO
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Đã xóa file {} khỏi MinIO", objectName);
        } catch (Exception e) {
            log.error("Lỗi khi xóa file trên MinIO", e);
        }
    }
}
