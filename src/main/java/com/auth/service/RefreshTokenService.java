package com.auth.service;

import com.auth.repository.RefreshTokenRepository;
import com.auth.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void persist(Long userId, String refreshToken) {
        refreshTokenRepository.save(
                userId,
                hash(refreshToken),
                jwtTokenProvider.getExpirationDateFromToken(refreshToken).toInstant()
        );
    }

    public boolean isValid(Long userId, String refreshToken) {
        return refreshTokenRepository.findActiveByUserIdAndTokenHash(
                userId,
                hash(refreshToken),
                Instant.now()
        ).isPresent();
    }

    public void revoke(String refreshToken) {
        refreshTokenRepository.revokeByTokenHash(hash(refreshToken));
    }

    private String hash(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
