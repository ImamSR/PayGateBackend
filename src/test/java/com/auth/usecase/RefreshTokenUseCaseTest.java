package com.auth.usecase;

import com.auth.model.AuthSession;
import com.auth.model.UserRole;
import com.auth.service.RefreshTokenService;
import com.auth.security.JwtTokenProvider;
import com.auth.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private LoadUserPrincipalUseCase loadUserPrincipalUseCase;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private RefreshTokenUseCase refreshTokenUseCase;

    @Test
    void shouldCreateNewTokenPairFromValidRefreshToken() {
        UserPrincipal userPrincipal = new UserPrincipal(
                7L,
                "tester",
                "tester@example.com",
                "hashed",
                UserRole.USER,
                true,
                true,
                true,
                true
        );

        when(jwtTokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("refresh-token")).thenReturn(7L);
        when(refreshTokenService.isValid(7L, "refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("refresh-token")).thenReturn("tester");
        when(loadUserPrincipalUseCase.execute("tester")).thenReturn(userPrincipal);
        when(jwtTokenProvider.generateToken(any())).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("new-refresh");

        AuthSession session = refreshTokenUseCase.execute("refresh-token");

        assertEquals("new-access", session.loginResponse().getToken());
        assertEquals("tester", session.loginResponse().getUser().getUsername());
        assertEquals("new-refresh", session.refreshToken());
        verify(refreshTokenService).revoke("refresh-token");
        verify(refreshTokenService).persist(7L, "new-refresh");
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        when(jwtTokenProvider.validateToken("bad-token")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenUseCase.execute("bad-token")
        );

        assertEquals("Refresh token is invalid", exception.getMessage());
    }
}
