package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.AuthSession;
import com.example.auth.model.User;
import com.example.auth.service.AuthCookieService;
import com.example.auth.usecase.GetCurrentUserUseCase;
import com.example.auth.usecase.LoginUserUseCase;
import com.example.auth.usecase.LogoutUserUseCase;
import com.example.auth.usecase.RefreshTokenUseCase;
import com.example.auth.usecase.RegisterUserUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final AuthCookieService authCookieService;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            AuthCookieService authCookieService,
            GetCurrentUserUseCase getCurrentUserUseCase
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.authCookieService = authCookieService;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthSession authSession = loginUserUseCase.execute(loginRequest);
            return ResponseEntity.ok()
                    .header("Set-Cookie", authCookieService.createAccessTokenCookie(authSession.loginResponse().getToken()).toString())
                    .header("Set-Cookie", authCookieService.createRefreshTokenCookie(authSession.refreshToken()).toString())
                    .body(buildFullResponse(authSession));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username/email or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User registeredUser = registerUserUseCase.execute(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );
            return ResponseEntity.ok("Basic user registered successfully: " + registeredUser.getUsername());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body("Error: " + exception.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth controller is working!");
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse.UserInfo> currentUser(Authentication authentication) {
        try {
            User currentUser = getCurrentUserUseCase.execute(authentication);
            return ResponseEntity.ok(new LoginResponse.UserInfo(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getEmail(),
                    currentUser.getRole().name()
            ));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        logoutUserUseCase.execute(readRefreshTokenCookie(request));
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok()
                .header("Set-Cookie", authCookieService.clearAccessTokenCookie().toString())
                .header("Set-Cookie", authCookieService.clearRefreshTokenCookie().toString())
                .body("User logged out successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(HttpServletRequest request) {
        try {
            AuthSession authSession = refreshTokenUseCase.execute(readRefreshTokenCookie(request));
            return ResponseEntity.ok()
                    .header("Set-Cookie", authCookieService.createAccessTokenCookie(authSession.loginResponse().getToken()).toString())
                    .header("Set-Cookie", authCookieService.createRefreshTokenCookie(authSession.refreshToken()).toString())
                    .body(buildFullResponse(authSession));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private String readRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (authCookieService.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private LoginResponse buildFullResponse(AuthSession authSession) {
        return new LoginResponse(
                authSession.loginResponse().getToken(),
                authSession.refreshToken(),
                authSession.loginResponse().getExpiresIn(),
                authSession.loginResponse().getUser()
        );
    }
}
