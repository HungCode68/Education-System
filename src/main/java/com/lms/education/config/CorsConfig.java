package com.lms.education.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Áp dụng CORS cho tất cả các endpoint bắt đầu bằng /api
                // Cấu hình các port Frontend được phép gọi đến (Ví dụ: React, Vue/Vite, Angular)
                .allowedOrigins(
                        "http://localhost:3000",  // Thường dùng cho React (CRA)
                        "http://localhost:5173",  // Thường dùng cho Vite (Vue/React)
                        "http://localhost:4200"   // Thường dùng cho Angular
                )
                // Nếu đang test nội bộ mà muốn lười (mở cho TẤT CẢ), bạn có thể dùng dòng dưới thay cho allowedOrigins
                // .allowedOriginPatterns("*")

                // Các HTTP Method được phép gọi
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

                // Cho phép tất cả các Headers
                .allowedHeaders("*")

                // Cho phép gửi Cookie, Token (Quan trọng khi làm chức năng Login)
                .allowCredentials(true)

                // Cache thông tin CORS preflight request trong 1 giờ để tăng tốc độ
                .maxAge(3600);
    }
}
