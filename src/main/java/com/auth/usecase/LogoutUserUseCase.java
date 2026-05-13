package com.auth.usecase;

import com.auth.service.RefreshTokenService;
import org.springframework.stereotype.Service;

@Service
public class LogoutUserUseCase {

    private final RefreshTokenService refreshTokenService;

    public LogoutUserUseCase(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    public void execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenService.revoke(refreshToken);
    }
}
