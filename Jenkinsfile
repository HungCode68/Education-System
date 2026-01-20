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
                    -e DB_PASSWORD=123456789 \
                    -v /var/log/lms-backend:/logs \
                    ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }
    }
}