package com.auth.usecase;

import com.auth.dto.LoginRequest;
import com.auth.dto.LoginResponse;
import com.auth.model.AuthSession;
import com.auth.service.RefreshTokenService;
import com.auth.security.JwtTokenProvider;
import com.auth.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LoginUserUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public LoginUserUseCase(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthSession execute(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        refreshTokenService.persist(userPrincipal.getId(), refreshToken);

        return new AuthSession(
                new LoginResponse(
                        accessToken,
                        null,
                        3600L,
                        new LoginResponse.UserInfo(
                                userPrincipal.getId(),
                                userPrincipal.getUsername(),
                                userPrincipal.getEmail(),
                                userPrincipal.getRole().name()
                        )
                ),
                refreshToken
        );
    }
}
