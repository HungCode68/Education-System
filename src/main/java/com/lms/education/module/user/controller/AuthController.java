package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.*;
import com.lms.education.module.user.service.UserService;
import com.lms.education.security.UserPrincipal;
import com.lms.education.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @PostMapping("/login")
    @LogActivity(module = "AUTH", action = "LOGIN", targetType = "auth", description = "Đăng nhập và nhận Cookies")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        try {
            // Gọi Spring Security xác thực
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            // Xác thực thành công -> Lưu vào Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

            // Tạo cặp Token mới
            String newRefreshToken = UUID.randomUUID().toString();
            userService.updateRefreshToken(userDetails.getId(), newRefreshToken, Instant.now().plusSeconds(7L * 24 * 60 * 60));

            // Tạo Cookies
            var jwtCookie = jwtUtils.generateJwtCookie(userDetails);
            var jwtRefreshCookie = jwtUtils.generateRefreshJwtCookie(newRefreshToken);

            // Tạo Token thô để trả về cho SPA Frontend (Angular/React) lưu vào Memory/Storage
            String accessToken = jwtUtils.generateTokenFromEmail(userDetails.getEmail());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Đăng nhập thành công!");
            responseBody.put("accessToken", accessToken);
            responseBody.put("tokenType", "Bearer");
            responseBody.put("fullName", userDetails.getFullName());
            responseBody.put("roles", userDetails.getRoleDescriptions());
            responseBody.put("permissions", userDetails.getPermissionList());

            // Trả về kết quả (bao gồm cả Cookies lẫn Response Body cho Frontend)
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                    .body(responseBody);

        } catch (DisabledException e) {
            // Trạng thái != ACTIVE
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Tài khoản của bạn chưa được kích hoạt hoặc đã bị vô hiệu hóa."));

        } catch (LockedException e) {
            // Trạng thái == LOCKED
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên."));

        } catch (BadCredentialsException e) {
            // Sai email hoặc mật khẩu
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email hoặc mật khẩu không chính xác."));
        } catch (Exception e) {
            // Lỗi hệ thống khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đã xảy ra lỗi trong quá trình đăng nhập."));
        }
    }

    @PostMapping("/refresh-token")
    @LogActivity(module = "AUTH", action = "REFRESH", targetType = "token", description = "Xoay vòng Refresh Token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        // Lấy refresh token từ cookie
        String refreshToken = jwtUtils.getJwtRefreshFromCookies(request);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh Token không tìm thấy trong Cookie!"));
        }

        return userService.findByRefreshToken(refreshToken)
                .map(user -> {
                    // Kiểm tra hết hạn & Trạng thái tài khoản
                    if (user.getExpiryDate().isBefore(Instant.now())) {
                        userService.deleteRefreshToken(refreshToken);
                        throw new RuntimeException("Phiên làm việc hết hạn. Vùi lòng đăng nhập lại.");
                    }
                    if (!"ACTIVE".equals(user.getStatus())) {
                        throw new RuntimeException("Tài khoản đã bị khóa.");
                    }

                    // Tạo Refresh Token MỚI
                    String newRefreshToken = UUID.randomUUID().toString();
                    userService.updateRefreshToken(user.getId(), newRefreshToken, Instant.now().plusSeconds(7L * 24 * 60 * 60));

                    // Tạo Cookies MỚI
                    var userPrincipal = UserPrincipal.create(user);
                    var newJwtCookie = jwtUtils.generateJwtCookie(userPrincipal);
                    var newJwtRefreshCookie = jwtUtils.generateRefreshJwtCookie(newRefreshToken);

                    String newAccessToken = jwtUtils.generateTokenFromEmail(user.getEmail());

                    Map<String, Object> body = new HashMap<>();
                    body.put("message", "Làm mới phiên thành công!");
                    body.put("accessToken", newAccessToken);
                    body.put("tokenType", "Bearer");

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, newJwtCookie.toString())
                            .header(HttpHeaders.SET_COOKIE, newJwtRefreshCookie.toString())
                            .body(body);
                })
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ hoặc đã bị sử dụng!"));
    }

    @PostMapping("/logout")
    @LogActivity(module = "AUTH", action = "LOGOUT", targetType = "auth", description = "Đăng xuất và xóa Cookies")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String refreshToken = jwtUtils.getJwtRefreshFromCookies(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            userService.deleteRefreshToken(refreshToken);
        }

        // Xóa cookies ở trình duyệt
        var cleanJwtCookie = jwtUtils.getCleanJwtCookie();
        var cleanRefreshCookie = jwtUtils.getCleanJwtRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanJwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString())
                .body(Map.of("message", "Đăng xuất thành công và đã hủy phiên làm việc!"));
    }
}