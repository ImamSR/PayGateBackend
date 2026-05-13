package com.auth.usecase;

import com.auth.dto.LoginRequest;
import com.auth.model.AuthSession;
import com.auth.model.UserRole;
import com.auth.security.JwtTokenProvider;
import com.auth.security.UserPrincipal;
import com.auth.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private LoginUserUseCase loginUserUseCase;

    @Test
    void shouldReturnAccessTokenAndPersistRefreshToken() {
        UserPrincipal userPrincipal = new UserPrincipal(
                3L,
                "basicuser",
                "basic@example.com",
                "hashed",
                UserRole.USER,
                true,
                true,
                true,
                true
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(authentication)).thenReturn("refresh-token");

        AuthSession session = loginUserUseCase.execute(new LoginRequest("basicuser", "secret123"));

        assertEquals("access-token", session.loginResponse().getToken());
        assertEquals("basicuser", session.loginResponse().getUser().getUsername());
        assertEquals("refresh-token", session.refreshToken());
        assertEquals(null, session.loginResponse().getRefreshToken());
        verify(refreshTokenService).persist(3L, "refresh-token");
    }
}
