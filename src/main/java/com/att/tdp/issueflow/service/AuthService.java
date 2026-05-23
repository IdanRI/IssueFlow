package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.config.JwtTokenProvider;
import com.att.tdp.issueflow.dto.request.LoginRequest;
import com.att.tdp.issueflow.dto.response.LoginResponse;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.entity.TokenBlacklist;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.mapper.UserMapper;
import com.att.tdp.issueflow.repository.TokenBlacklistRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expirationMs / 1000)
                .build();
    }

    @Transactional
    public void logout(String token) {
        String jti = jwtTokenProvider.getJtiFromToken(token);
        java.util.Date expiration = jwtTokenProvider.getExpirationFromToken(token);

        TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                .tokenJti(jti)
                .expiry(expiration.toInstant())
                .build();

        tokenBlacklistRepository.save(blacklistEntry);
    }

    public UserResponse getCurrentUser(User user) {
        return UserMapper.toResponse(user);
    }
}
