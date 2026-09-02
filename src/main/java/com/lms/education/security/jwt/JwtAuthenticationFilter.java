package com.lms.education.security.jwt;

import com.lms.education.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Trích xuất JWT từ Authorization Header (Ví dụ: Bearer <token>)
            String jwt = null;
            String headerAuth = request.getHeader("Authorization");
            if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
                jwt = headerAuth.substring(7);
            } else {
                // 2. Nếu Header không có -> Trích xuất từ Cookie
                jwt = jwtUtils.getJwtFromCookies(request);
            }

            // Kiểm tra tính hợp lệ của Token
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // Lấy Email từ Token (Email đóng vai trò là định danh chính)
                String email = jwtUtils.getSubjectFromJwtToken(jwt);

                // Load thông tin người dùng từ Database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Kiểm tra status thông qua các hàm isEnabled, isAccountNonLocked
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Thiết lập phiên làm việc vào Security Context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Không thể thiết lập xác thực người dùng: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}