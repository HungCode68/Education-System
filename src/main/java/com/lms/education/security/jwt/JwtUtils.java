package com.lms.education.security.jwt;

import com.lms.education.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs}")
    private int jwtExpirationMs;

    @Value("${app.jwt.cookieName}")
    private String jwtCookie;

    @Value("${app.jwt.refreshCookieName}")
    private String jwtRefreshCookie;

    @Value("${app.jwt.cookieSecure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookieSameSite:Lax}")
    private String cookieSameSite;

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Tạo Cookie chứa Access Token
    public ResponseCookie generateJwtCookie(UserPrincipal userPrincipal) {
        String jwt = generateTokenFromEmail(userPrincipal.getEmail());
        return generateCookie(jwtCookie, jwt, "/api");
    }

    // Tạo Cookie chứa Refresh Token ---
    public ResponseCookie generateRefreshJwtCookie(String refreshToken) {
        // Bọc Refresh Token (UUID) vào JWT
        String wrappedToken = Jwts.builder()
                .subject(refreshToken)
                .issuedAt(new Date())
                // Thời hạn 7 ngày, khớp với thời hạn trong Database
                .expiration(new Date((new Date()).getTime() + 7L * 24 * 60 * 60 * 1000))
                .signWith(key())
                .compact();
        return generateCookie(jwtRefreshCookie, wrappedToken, "/api/v1/auth");
    }

    // Lấy Token từ Cookies ---
    public String getJwtFromCookies(HttpServletRequest request) {
        return getCookieValueByName(request, jwtCookie);
    }

    public String getJwtRefreshFromCookies(HttpServletRequest request) {
        String value = getCookieValueByName(request, jwtRefreshCookie);
        if (value == null) return null;

        // Tương thích ngược: Nếu Cookie chứa UUID cũ (Không có dấu chấm của JWT)
        if (!value.contains(".")) {
            return value;
        }

        // Nếu là Token mới đã được bọc JWT, xác thực chữ ký và mở lấy UUID
        if (validateJwtToken(value)) {
            return getSubjectFromJwtToken(value); // Subject chính là UUID
        }

        return null;
    }

    // Xóa Cookies (khi Logout)
    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookie, null).path("/api").build();
    }

    public ResponseCookie getCleanJwtRefreshCookie() {
        return ResponseCookie.from(jwtRefreshCookie, null).path("/api/v1/auth").build();
    }

    // Logic tạo Token thô
    public String generateTokenFromEmail(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    // --- Private Helpers ---
    private ResponseCookie generateCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .path(path)
                .maxAge(24 * 60 * 60) // 24h
                .httpOnly(true)       // Quan trọng: JavaScript không thể đọc
                .secure(cookieSecure) // Dynamic: false trên Localhost, true trên HTTPS Production
                .sameSite(cookieSameSite) // Dynamic: Lax trên Localhost, None trên HTTPS Production
                .build();
    }

    private String getCookieValueByName(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return (cookie != null) ? cookie.getValue() : null;
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (Exception e) {
            logger.error("Lỗi xác thực JWT: {}", e.getMessage());
        }
        return false;
    }

    public String getSubjectFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}