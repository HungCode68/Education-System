package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.JwtResponse;
import com.lms.education.module.user.dto.LoginRequest;
import com.lms.education.module.user.dto.TokenRefreshRequest;
import com.lms.education.module.user.dto.TokenRefreshResponse;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.security.UserPrincipal;
import com.lms.education.security.jwt.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Value("${jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.replace("ROLE_", ""))
                .collect(Collectors.toList());

        List<String> permissions = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toList());

        // Generate Refresh Token (JWT format)
        String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());
        
        // Save Refresh Token to DB
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userDetails.getId()));
        
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusNanos(refreshTokenDurationMs * 1000000)); // ms to nanos
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                refreshToken,
                userDetails.getId(),
                userDetails.getEmail(),
                roles,
                permissions));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Find user by refresh token (Needs a method in repo or just search all - suboptimal but works for now, or add method)
        // Better: Add findByRefreshToken to UserRepository
        // For now, let's assume we can find it. I'll update UserRepository first.
        return userRepository.findByRefreshToken(requestRefreshToken)
                .map(user -> {
                    if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
                        throw new RuntimeException("Refresh token was expired. Please make a new signin request");
                    }
                    
                    String token = jwtUtils.generateTokenFromUsername(user.getEmail());
                    // Rotate Refresh Token? (Optional, but good for security. Keeping same for now or generating new?)
                    // If we want to rotate, we should generate new one here too.
                    // For now, keep existing behavior (return new access token, keep old refresh token)
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
