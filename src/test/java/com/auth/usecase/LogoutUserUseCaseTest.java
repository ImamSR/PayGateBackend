package com.auth.usecase;

import com.auth.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutUserUseCaseTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private LogoutUserUseCase logoutUserUseCase;

    @Test
    void shouldRevokeRefreshTokenWhenPresent() {
        logoutUserUseCase.execute("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    @Test
    void shouldIgnoreMissingRefreshToken() {
        logoutUserUseCase.execute(null);
        logoutUserUseCase.execute("");

        verify(refreshTokenService, never()).revoke("refresh-token");
    }
}
