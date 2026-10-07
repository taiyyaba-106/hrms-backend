package com.hrms.service.impl;

import com.hrms.config.JwtProperties;
import com.hrms.dto.LoginRequest;
import com.hrms.dto.LoginResponse;
import com.hrms.entity.JwtToken;
import com.hrms.entity.User;
import com.hrms.entity.enums.TokenType;
import com.hrms.entity.enums.UserStatus;
import com.hrms.exception.ApiException;
import com.hrms.repository.JwtTokenRepository;
import com.hrms.repository.UserRepository;
import com.hrms.security.JwtUtils;
import com.hrms.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtTokenRepository jwtTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(UserRepository userRepository,
                           JwtTokenRepository jwtTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.jwtTokenRepository = jwtTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String email = loginRequest.getEmail().trim();
        String rawPassword = loginRequest.getPassword();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    logger.warn("Authentication failed: User with email {} not found", email);
                    return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            logger.warn("Authentication failed: User {} has status {}", email, user.getStatus());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            logger.warn("Authentication failed: Password mismatch for user {}", email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String accessToken = jwtUtils.generateTokenForUser(user);
        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProperties.getExpirationMs(), ChronoUnit.MILLIS);

        JwtToken jwtTokenEntity = JwtToken.builder()
                .token(accessToken)
                .tokenType(TokenType.BEARER)
                .user(user)
                .revoked(false)
                .expired(false)
                .expiresAt(expiresAt)
                .build();
        jwtTokenRepository.save(jwtTokenEntity);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("User {} logged in successfully.", email);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .build();
    }
}
