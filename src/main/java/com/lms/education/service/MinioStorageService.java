package com.lms.education.service;

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

    // Upload file lên MinIO và trả về tên object đã lưu
    public String uploadFile(MultipartFile file) {
        try {
            // Kiểm tra bucket tồn tại, nếu chưa có thì tự động tạo
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Đã tạo mới MinIO bucket: {}", bucketName);
            }

            // Tạo tên file độc nhất để tránh trùng lặp
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
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

    // Upload file giữ nguyên tên gốc (bọc trong thư mục UUID để tránh trùng lặp)
    public String uploadFileKeepName(MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
            }

            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                originalFileName = "unnamed_file";
            }
            
            // Tạo objectName dạng thư_mục_UUID/tên_file_gốc.ext
            // Khi tải về trình duyệt sẽ lấy tên_file_gốc.ext
            String objectName = UUID.randomUUID().toString() + "/" + originalFileName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Đã upload thành công file {} lên MinIO", objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Lỗi khi upload file lên MinIO", e);
            throw new RuntimeException("Lỗi hệ thống khi lưu trữ file: " + e.getMessage());
        }
    }

    // Lấy Presigned URL để tải/xem file
    public String getFileUrl(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(2, TimeUnit.HOURS) // URL có tác dụng trong 2 giờ
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy URL file từ MinIO", e);
            return null;
        }
    }

    // Xóa file khỏi MinIO
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return;
        }
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
