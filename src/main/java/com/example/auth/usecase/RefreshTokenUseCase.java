package com.example.auth.usecase;

import com.example.auth.dto.LoginResponse;
import com.example.auth.model.AuthSession;
import com.example.auth.service.RefreshTokenService;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoadUserPrincipalUseCase loadUserPrincipalUseCase;
    private final RefreshTokenService refreshTokenService;

    public RefreshTokenUseCase(
            JwtTokenProvider jwtTokenProvider,
            LoadUserPrincipalUseCase loadUserPrincipalUseCase,
            RefreshTokenService refreshTokenService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.loadUserPrincipalUseCase = loadUserPrincipalUseCase;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthSession execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is invalid");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (!refreshTokenService.isValid(userId, refreshToken)) {
            throw new IllegalArgumentException("Refresh token is revoked or expired");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UserPrincipal userPrincipal = loadUserPrincipalUseCase.execute(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        refreshTokenService.revoke(refreshToken);

        String nextRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        refreshTokenService.persist(userPrincipal.getId(), nextRefreshToken);

        return new AuthSession(
                new LoginResponse(
                        jwtTokenProvider.generateToken(authentication),
                        null,
                        3600L,
                        new LoginResponse.UserInfo(
                                userPrincipal.getId(),
                                userPrincipal.getUsername(),
                                userPrincipal.getEmail(),
                                userPrincipal.getRole().name()
                        )
                ),
                nextRefreshToken
        );
    }
}
