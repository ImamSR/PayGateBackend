package com.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    private final String accessCookieName;
    private final String refreshCookieName;
    private final String accessCookiePath;
    private final boolean secureCookie;
    private final String sameSite;
    private final String refreshCookiePath;
    private final Duration accessMaxAge;
    private final Duration refreshMaxAge;

    public AuthCookieService(
            @Value("${app.auth.access-cookie.name:access_token}") String accessCookieName,
            @Value("${app.auth.refresh-cookie.name:refresh_token}") String refreshCookieName,
            @Value("${app.auth.refresh-cookie.secure:false}") boolean secureCookie,
            @Value("${app.auth.refresh-cookie.same-site:Lax}") String sameSite,
            @Value("${app.auth.access-cookie.path:/}") String accessCookiePath,
            @Value("${app.auth.refresh-cookie.path:/api/auth}") String refreshCookiePath,
            @Value("${app.auth.access-cookie.max-age-seconds:900}") long accessMaxAgeSeconds,
            @Value("${app.auth.refresh-cookie.max-age-seconds:86400}") long refreshMaxAgeSeconds
    ) {
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.accessCookiePath = accessCookiePath;
        this.refreshCookiePath = refreshCookiePath;
        this.accessMaxAge = Duration.ofSeconds(accessMaxAgeSeconds);
        this.refreshMaxAge = Duration.ofSeconds(refreshMaxAgeSeconds);
    }

    public String getAccessCookieName() {
        return accessCookieName;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from(accessCookieName, accessToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(accessCookiePath)
                .maxAge(accessMaxAge)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(refreshCookiePath)
                .maxAge(refreshMaxAge)
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(accessCookiePath)
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(refreshCookiePath)
                .maxAge(Duration.ZERO)
                .build();
    }
}
