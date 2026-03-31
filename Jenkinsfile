pipeline {
    agent any


    tools {
        maven 'Maven-3.9'
        jdk 'JDK-21'
    }

    // Biến môi trường
    environment {
        // Tên image docker bạn muốn đặt
        DOCKER_IMAGE = 'hungcode68/lms-backend'
        DOCKER_TAG = "${BUILD_NUMBER}" // Tag theo số lần build (v1, v2...)

        SECRET_DB_PASS = credentials('lms-db-password')
        SECRET_JWT_KEY = credentials('lms-jwt-secret')
        SECRET_MINIO_KEY = credentials('lms-minio-secret')
        SECRET_APP_PASS = credentials('lms-app-password')
    }

    stages {
        stage('1. Checkout Code') {
            steps {
                echo 'Đang lấy code từ GitHub...'
                checkout scm
            }
        }

        stage('2. Build Spring Boot') {
            steps {
                echo 'Đang build file JAR...'
                // Skip test để build cho nhanh (bỏ -DskipTests nếu muốn chạy test)
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('3. Build Docker Image') {
            steps {
                echo 'Đang đóng gói Docker...'
                // Lệnh build image từ Dockerfile
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
            }
        }

        stage('4. Deploy') {
            steps {
                script {
                echo 'Đang chạy Container...'
                // Xóa container cũ đi (nếu có) để chạy cái mới
                sh "docker stop lms-backend || true"
                sh "docker rm lms-backend || true"


                sh """
                    docker run -d \
                    --name lms-backend \
                    --network lms-network \
                    -p 8090:8081 \
                    -e DB_URL=jdbc:mysql://mysql-lms:3306/lms_db?createDatabaseIfNotExist=true \
                    -e DB_USERNAME='root' \
                    -e DB_PASSWORD='${SECRET_DB_PASS}' \
                    -e JWT_SECRET='${SECRET_JWT_KEY}' \
                    -e JWT_EXPIRATION=86400000 \
                    -e MINIO_ENDPOINT=http://103.57.220.127:9000 \
                    -e MINIO_ACCESS_KEY='admin' \
                    -e MINIO_SECRET_KEY='${SECRET_MINIO_KEY}' \
                    -e MINIO_BUCKET_NAME=lms-storage \
                    -e APP_SECURITY_USER='admin' \
                    -e APP_SECURITY_PASSWORD='${SECRET_APP_PASS}' \
                    -v /var/log/lms-backend:/logs \
                    ${DOCKER_IMAGE}:${DOCKER_TAG}
                """

                sh "docker logs -f lms-backend & sleep 15 ; kill \$!"

                sh "docker system prune -a -f"

                def targetContainers = ['lms-backend', 'mysql-lms', 'minio-server', 'frontend-app']
                def failedContainers = []

                targetContainers.each { containerName ->
                    // Kiểm tra trạng thái Running của từng container
                    // Lệnh "|| echo 'false'" để tránh lỗi pipeline nếu container không tồn tại
                    def isRunning = sh(script: "docker inspect -f '{{.State.Running}}' ${containerName} || echo 'false'", returnStdout: true).trim()

                    if (isRunning == 'true') {
                        echo "✅ [OK] Container '${containerName}' đang chạy ổn định."
                    } else {
                        echo "❌ [ERROR] Container '${containerName}' ĐÃ BỊ SẬP hoặc KHÔNG TỒN TẠI!"
                        failedContainers.add(containerName)

                        // In ngay 50 dòng log cuối của container bị lỗi để debug
                        echo "🔍 Log lỗi của ${containerName}:"
                        sh "docker logs --tail 50 ${containerName} || true"
                    }
                }

                if (!failedContainers.isEmpty()) {
                    error("🚨 Deployment Thất bại! Các container sau gặp sự cố: ${failedContainers}")
                } else {
                    echo "🎉 CHÚC MỪNG! Toàn bộ hệ thống (Backend, DB, MinIO, Frontend) đang hoạt động tốt!"
                }
              }
            }
        }
    }
}