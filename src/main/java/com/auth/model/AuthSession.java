package com.auth.model;

import com.auth.dto.LoginResponse;

public record AuthSession(LoginResponse loginResponse, String refreshToken) {
}
